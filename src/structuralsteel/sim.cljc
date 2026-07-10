(ns structuralsteel.sim
  "Demo driver -- `clojure -M:dev:run`. Walks a clean assembly through
  intake -> welding-procedure requirements verification -> NDE-
  inspection screening -> assembly-dispatch proposal (always
  escalates) -> human approval -> commit, then through fabrication-
  certificate proposal (always escalates) -> human approval -> commit,
  then shows five HARD holds (a jurisdiction with no spec-basis, an
  out-of-spec camber deviation, an unresolved NDE defect screened
  directly via `:nde-inspection/screen` [never via an actuation op
  against an unscreened assembly -- see this actor's own governor ns
  docstring / the lesson `parksafety`'s ADR-2607071922 Decision 5,
  `eldercare`'s, `museum`'s, `conservation`'s, `salon`'s,
  `entertainment`'s, `casework`'s, `hospital`'s, `facility`'s,
  `school`'s, `association`'s, `leasing`'s, `behavioral`'s,
  `secondary`'s, `card`'s, `water`'s, `telecom`'s, `turbine`'s,
  `steelworks`'s and `automotive`'s ADR-0001s already recorded], and a
  double assembly-dispatch/certificate-issuance of an already-
  processed assembly) that never reach a human at all, and prints the
  audit ledger + the draft assembly-dispatch and fabrication-
  certificate records."
  (:require [langgraph.graph :as g]
            [structuralsteel.export :as export]
            [structuralsteel.store :as store]
            [structuralsteel.operation :as op]))

(def operator {:actor-id "op-1" :actor-role :certified-welding-inspector :phase 3})

(defn- exec! [actor tid request context]
  (g/run* actor {:request request :context context} {:thread-id tid}))

(defn- approve! [actor tid]
  (g/run* actor {:approval {:status :approved :by "op-1"}} {:thread-id tid :resume? true}))

(defn -main [& _]
  (let [db (store/seed-db)
        actor (op/build db)]
    (println "== assembly/intake assembly-1 (JPN, clean; camber within spec, no NDE defect) ==")
    (println (exec! actor "t1" {:op :assembly/intake :subject "assembly-1"
                                :patch {:id "assembly-1" :assembly-name "Sakura Moment-Frame Column Assembly MC-04"}} operator))

    (println "== welding-procedure/verify assembly-1 (escalates -- human approves) ==")
    (println (exec! actor "t2" {:op :welding-procedure/verify :subject "assembly-1"} operator))
    (println (approve! actor "t2"))

    (println "== nde-inspection/screen assembly-1 (clean; escalates -- human approves) ==")
    (println (exec! actor "t3" {:op :nde-inspection/screen :subject "assembly-1"} operator))
    (println (approve! actor "t3"))

    (println "== actuation/dispatch-assembly assembly-1 (always escalates -- actuation/dispatch-assembly) ==")
    (let [r (exec! actor "t4" {:op :actuation/dispatch-assembly :subject "assembly-1"} operator)]
      (println r)
      (println "-- human certified welding inspector approves --")
      (println (approve! actor "t4")))

    (println "== actuation/issue-fabrication-certificate assembly-1 (always escalates -- actuation/issue-fabrication-certificate) ==")
    (let [r (exec! actor "t5" {:op :actuation/issue-fabrication-certificate :subject "assembly-1"} operator)]
      (println r)
      (println "-- human certified welding inspector approves --")
      (println (approve! actor "t5")))

    (println "== welding-procedure/verify assembly-2 (no spec-basis -> HARD hold) ==")
    (println (exec! actor "t6" {:op :welding-procedure/verify :subject "assembly-2" :no-spec? true} operator))

    (println "== welding-procedure/verify assembly-3 (escalates -- human approves; sets up the out-of-spec test) ==")
    (println (exec! actor "t7" {:op :welding-procedure/verify :subject "assembly-3"} operator))
    (println (approve! actor "t7"))

    (println "== actuation/dispatch-assembly assembly-3 (0.35 outside [-0.10,0.10] tolerance -> HARD hold) ==")
    (println (exec! actor "t8" {:op :actuation/dispatch-assembly :subject "assembly-3"} operator))

    (println "== nde-inspection/screen assembly-4 (unresolved -> HARD hold, never reaches a human) ==")
    (println (exec! actor "t9" {:op :nde-inspection/screen :subject "assembly-4"} operator))

    (println "== actuation/dispatch-assembly assembly-1 AGAIN (double-dispatch -> HARD hold) ==")
    (println (exec! actor "t10" {:op :actuation/dispatch-assembly :subject "assembly-1"} operator))

    (println "== actuation/issue-fabrication-certificate assembly-1 AGAIN (double-issuance -> HARD hold) ==")
    (println (exec! actor "t11" {:op :actuation/issue-fabrication-certificate :subject "assembly-1"} operator))

    (println "== audit ledger ==")
    (doseq [f (store/ledger db)] (println f))

    (println "== draft assembly-dispatch records ==")
    (doseq [r (store/dispatch-history db)] (println r))

    (println "== draft fabrication-certificate records ==")
    (doseq [r (store/evidence-history db)] (println r))

    (println "== social hand-off: audit package counts ==")
    (println (:counts (export/audit-package db)))
    (println "== social hand-off: CSV bundle keys ==")
    (println (keys (export/package->csv-bundle db)))))
