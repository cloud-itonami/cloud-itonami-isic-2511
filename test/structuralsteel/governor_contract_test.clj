(ns structuralsteel.governor-contract-test
  "The governor contract as executable tests -- the structural-steel-
  fabricator analog of `cloud-itonami-isic-6512`'s `casualty.
  governor-contract-test`. The single invariant under test:

    Structural Fabrication Advisor never dispatches an assembly action
    or issues a fabrication certificate the Structural Fabrication
    Governor would reject, `:actuation/dispatch-assembly`/
    `:actuation/issue-fabrication-certificate` NEVER auto-commit at
    any phase, `:assembly/intake` (no direct capital risk) MAY
    auto-commit when clean, and every decision (commit OR hold) leaves
    exactly one ledger fact."
  (:require [clojure.test :refer [deftest is testing]]
            [langgraph.graph :as g]
            [structuralsteel.store :as store]
            [structuralsteel.operation :as op]))

(defn- fresh []
  (let [db (store/seed-db)]
    [db (op/build db)]))

(def operator {:actor-id "op-1" :actor-role :certified-welding-inspector :phase 3})

(defn- exec-op [actor tid request context]
  (g/run* actor {:request request :context context} {:thread-id tid}))

(defn- approve! [actor tid]
  (g/run* actor {:approval {:status :approved :by "op-1"}} {:thread-id tid :resume? true}))

(defn- verify!
  "Walks `subject` through verify -> approve, leaving a requirements
  verification on file. Uses distinct thread-ids per call site by
  suffixing `tid-prefix`."
  [actor tid-prefix subject]
  (exec-op actor (str tid-prefix "-verify") {:op :welding-procedure/verify :subject subject} operator)
  (approve! actor (str tid-prefix "-verify")))

(defn- screen!
  "Walks `subject` through NDE-defect screening -> approve, leaving a
  screening on file. Only safe to call for an assembly whose defect
  status has already resolved -- an unresolved defect HARD-holds the
  screen itself (see `nde-defect-is-held-and-unoverridable`)."
  [actor tid-prefix subject]
  (exec-op actor (str tid-prefix "-screen") {:op :nde-inspection/screen :subject subject} operator)
  (approve! actor (str tid-prefix "-screen")))

(deftest clean-intake-auto-commits
  (let [[db actor] (fresh)
        res (exec-op actor "t1"
                  {:op :assembly/intake :subject "assembly-1"
                   :patch {:id "assembly-1" :assembly-name "Sakura Moment-Frame Column Assembly MC-04"}} operator)]
    (is (= :commit (get-in res [:state :disposition])))
    (is (= "Sakura Moment-Frame Column Assembly MC-04" (:assembly-name (store/assembly db "assembly-1"))) "SSoT actually updated")
    (is (= 1 (count (store/ledger db))))))

(deftest requirements-verify-always-needs-approval
  (testing "verify is never in any phase's :auto set -- always human approval, even when clean"
    (let [[db actor] (fresh)
          res (exec-op actor "t2" {:op :welding-procedure/verify :subject "assembly-1"} operator)]
      (is (= :interrupted (:status res)))
      (let [r2 (approve! actor "t2")]
        (is (= :commit (get-in r2 [:state :disposition])))
        (is (some? (store/requirements-verification-of db "assembly-1")))))))

(deftest fabricated-jurisdiction-is-held
  (testing "a welding-procedure/verify proposal with no official spec-basis -> HOLD, never reaches a human"
    (let [[db actor] (fresh)
          res (exec-op actor "t3"
                    {:op :welding-procedure/verify :subject "assembly-1" :no-spec? true} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:no-spec-basis} (-> (store/ledger db) first :basis)))
      (is (nil? (store/requirements-verification-of db "assembly-1")) "no verification written"))))

(deftest dispatch-assembly-without-verification-is-held
  (testing "actuation/dispatch-assembly before any requirements verification -> HOLD (evidence incomplete)"
    (let [[db actor] (fresh)
          res (exec-op actor "t4" {:op :actuation/dispatch-assembly :subject "assembly-1"} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:evidence-incomplete} (-> (store/ledger db) first :basis))))))

(deftest assembly-camber-out-of-range-is-held
  (testing "an assembly whose own camber deviation falls outside its own spec bounds -> HOLD"
    (let [[db actor] (fresh)
          _ (verify! actor "t5pre" "assembly-3")
          res (exec-op actor "t5" {:op :actuation/dispatch-assembly :subject "assembly-3"} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:assembly-camber-out-of-range} (-> (store/ledger db) last :basis)))
      (is (empty? (store/dispatch-history db))))))

(deftest nde-defect-is-held-and-unoverridable
  (testing "an unresolved NDE defect on an assembly -> HOLD, and never reaches request-approval -- exercised via :nde-inspection/screen DIRECTLY, not via the actuation op against an unscreened assembly (see this actor's governor ns docstring / parksafety's ADR-2607071922 Decision 5 / eldercare's, museum's, conservation's, salon's, entertainment's, casework's, hospital's, facility's, school's, association's, leasing's, behavioral's, secondary's, card's, water's, telecom's, turbine's, steelworks's and automotive's ADR-0001s)"
    (let [[db actor] (fresh)
          res (exec-op actor "t6" {:op :nde-inspection/screen :subject "assembly-4"} operator)]
      (is (= :hold (get-in res [:state :disposition])) "settles immediately, no interrupt")
      (is (not= :interrupted (:status res)))
      (is (some #{:nde-defect-unresolved} (-> (store/ledger db) first :basis)))
      (is (nil? (store/nde-screen-of db "assembly-4")) "no clearance written"))))

(deftest dispatch-assembly-always-escalates-then-human-decides
  (testing "a clean, fully-verified, in-spec assembly still ALWAYS interrupts for human approval -- actuation/dispatch-assembly is never auto"
    (let [[db actor] (fresh)
          _ (verify! actor "t7pre" "assembly-1")
          r1 (exec-op actor "t7" {:op :actuation/dispatch-assembly :subject "assembly-1"} operator)]
      (is (= :interrupted (:status r1)) "pauses for human approval even when governor-clean")
      (testing "approve -> commit, dispatch record drafted"
        (let [r2 (approve! actor "t7")]
          (is (= :commit (get-in r2 [:state :disposition])))
          (is (true? (:assembly-dispatched? (store/assembly db "assembly-1"))))
          (is (= 1 (count (store/dispatch-history db))) "one draft dispatch record"))))))

(deftest issue-fabrication-certificate-always-escalates-then-human-decides
  (testing "a clean, fully-verified, resolved-defect assembly still ALWAYS interrupts for human approval -- actuation/issue-fabrication-certificate is never auto"
    (let [[db actor] (fresh)
          _ (verify! actor "t8pre" "assembly-1")
          _ (screen! actor "t8pre2" "assembly-1")
          r1 (exec-op actor "t8" {:op :actuation/issue-fabrication-certificate :subject "assembly-1"} operator)]
      (is (= :interrupted (:status r1)) "pauses for human approval even when governor-clean")
      (testing "approve -> commit, certificate record drafted"
        (let [r2 (approve! actor "t8")]
          (is (= :commit (get-in r2 [:state :disposition])))
          (is (true? (:fabrication-certified? (store/assembly db "assembly-1"))))
          (is (= 1 (count (store/evidence-history db))) "one draft certificate record"))))))

(deftest dispatch-assembly-double-dispatch-is-held
  (testing "dispatching the same assembly's action twice -> HOLD on the second attempt"
    (let [[db actor] (fresh)
          _ (verify! actor "t9pre" "assembly-1")
          _ (exec-op actor "t9a" {:op :actuation/dispatch-assembly :subject "assembly-1"} operator)
          _ (approve! actor "t9a")
          res (exec-op actor "t9" {:op :actuation/dispatch-assembly :subject "assembly-1"} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:already-dispatched} (-> (store/ledger db) last :basis)))
      (is (= 1 (count (store/dispatch-history db))) "still only the one earlier dispatch"))))

(deftest issue-fabrication-certificate-double-issuance-is-held
  (testing "issuing the same assembly's fabrication certificate twice -> HOLD on the second attempt"
    (let [[db actor] (fresh)
          _ (verify! actor "t10pre" "assembly-1")
          _ (screen! actor "t10pre2" "assembly-1")
          _ (exec-op actor "t10a" {:op :actuation/issue-fabrication-certificate :subject "assembly-1"} operator)
          _ (approve! actor "t10a")
          res (exec-op actor "t10" {:op :actuation/issue-fabrication-certificate :subject "assembly-1"} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:already-certified} (-> (store/ledger db) last :basis)))
      (is (= 1 (count (store/evidence-history db))) "still only the one earlier certificate issuance"))))

(deftest every-decision-leaves-one-ledger-fact
  (testing "write-only-through-ledger: N operations -> N ledger facts"
    (let [[db actor] (fresh)]
      (exec-op actor "a" {:op :assembly/intake :subject "assembly-1"
                          :patch {:id "assembly-1" :assembly-name "Sakura Moment-Frame Column Assembly MC-04"}} operator)
      (exec-op actor "b" {:op :welding-procedure/verify :subject "assembly-1" :no-spec? true} operator)
      (is (= 2 (count (store/ledger db)))
          "one commit + one hold, both recorded"))))
