(ns structuralsteel.store-contract-test
  "The Store contract, run against BOTH backends. Proving MemStore and
  the Datomic-backed (langchain.db) store satisfy the same contract is
  what makes 'swap the SSoT for Datomic / kotoba-server' a configuration
  change, not a rewrite -- see `cloud-itonami-isic-6511`'s
  `underwriting.store-contract-test` for the same pattern on the sibling
  actor."
  (:require [clojure.test :refer [deftest is testing]]
            [structuralsteel.store :as store]))

(defn- backends []
  [["MemStore" (store/seed-db)] ["DatomicStore" (store/datomic-seed-db)]])

(deftest read-parity
  (doseq [[label s] (backends)]
    (testing label
      (is (= "Sakura Moment-Frame Column Assembly MC-04" (:assembly-name (store/assembly s "assembly-1"))))
      (is (= "JPN" (:jurisdiction (store/assembly s "assembly-1"))))
      (is (= 0.05 (:camber-deviation-actual (store/assembly s "assembly-1"))))
      (is (= -0.10 (:camber-deviation-min (store/assembly s "assembly-1"))))
      (is (= 0.10 (:camber-deviation-max (store/assembly s "assembly-1"))))
      (is (false? (:nde-defect-unresolved? (store/assembly s "assembly-1"))))
      (is (= 0.35 (:camber-deviation-actual (store/assembly s "assembly-3"))))
      (is (true? (:nde-defect-unresolved? (store/assembly s "assembly-4"))))
      (is (false? (:assembly-dispatched? (store/assembly s "assembly-1"))))
      (is (false? (:fabrication-certified? (store/assembly s "assembly-1"))))
      (is (= ["assembly-1" "assembly-2" "assembly-3" "assembly-4"]
             (mapv :id (store/all-assemblies s))))
      (is (nil? (store/nde-screen-of s "assembly-1")))
      (is (nil? (store/requirements-verification-of s "assembly-1")))
      (is (= [] (store/ledger s)))
      (is (= [] (store/dispatch-history s)))
      (is (= [] (store/evidence-history s)))
      (is (zero? (store/next-dispatch-sequence s "JPN")))
      (is (zero? (store/next-evidence-sequence s "JPN")))
      (is (false? (store/assembly-already-dispatched? s "assembly-1")))
      (is (false? (store/assembly-already-certified? s "assembly-1"))))))

(deftest write-and-ledger-parity
  (doseq [[label s] (backends)]
    (testing label
      (testing "partial upsert merges, preserving untouched fields"
        (store/commit-record! s {:effect :assembly/upsert
                                 :value {:id "assembly-1" :assembly-name "Sakura Moment-Frame Column Assembly MC-04"}})
        (is (= "Sakura Moment-Frame Column Assembly MC-04" (:assembly-name (store/assembly s "assembly-1"))))
        (is (= 0.05 (:camber-deviation-actual (store/assembly s "assembly-1"))) "unrelated field preserved"))
      (testing "verification / NDE-screen payloads commit and read back"
        (store/commit-record! s {:effect :verification/set :path ["assembly-1"]
                                 :payload {:jurisdiction "JPN" :checklist ["a" "b"]}})
        (is (= {:jurisdiction "JPN" :checklist ["a" "b"]} (store/requirements-verification-of s "assembly-1")))
        (store/commit-record! s {:effect :nde-screen/set :path ["assembly-1"]
                                 :payload {:assembly-id "assembly-1" :verdict :resolved}})
        (is (= {:assembly-id "assembly-1" :verdict :resolved} (store/nde-screen-of s "assembly-1"))))
      (testing "assembly dispatch drafts a record and advances the sequence"
        (store/commit-record! s {:effect :assembly/mark-dispatched :path ["assembly-1"]})
        (is (= "JPN-ASM-000000" (get (first (store/dispatch-history s)) "record_id")))
        (is (= "assembly-dispatch-draft" (get (first (store/dispatch-history s)) "kind")))
        (is (true? (:assembly-dispatched? (store/assembly s "assembly-1"))))
        (is (= 1 (count (store/dispatch-history s))))
        (is (= 1 (store/next-dispatch-sequence s "JPN")))
        (is (true? (store/assembly-already-dispatched? s "assembly-1")))
        (is (false? (store/assembly-already-dispatched? s "assembly-2"))))
      (testing "fabrication certificate drafts a record and advances the sequence"
        (store/commit-record! s {:effect :assembly/mark-certified :path ["assembly-1"]})
        (is (= "JPN-FAB-000000" (get (first (store/evidence-history s)) "record_id")))
        (is (= "fabrication-certificate-draft" (get (first (store/evidence-history s)) "kind")))
        (is (true? (:fabrication-certified? (store/assembly s "assembly-1"))))
        (is (= 1 (count (store/evidence-history s))))
        (is (= 1 (store/next-evidence-sequence s "JPN")))
        (is (true? (store/assembly-already-certified? s "assembly-1")))
        (is (false? (store/assembly-already-certified? s "assembly-2"))))
      (testing "ledger is append-only and order-preserving"
        (store/append-ledger! s {:op :a :disposition :commit})
        (store/append-ledger! s {:op :b :disposition :hold})
        (is (= [:commit :hold] (mapv :disposition (store/ledger s))))))))

(deftest datomic-empty-store-is-usable
  (let [s (store/datomic-store)]
    (is (nil? (store/assembly s "nope")))
    (is (= [] (store/all-assemblies s)))
    (is (= [] (store/ledger s)))
    (is (= [] (store/dispatch-history s)))
    (is (= [] (store/evidence-history s)))
    (is (zero? (store/next-dispatch-sequence s "JPN")))
    (is (zero? (store/next-evidence-sequence s "JPN")))
    (store/with-assemblies s {"x" {:id "x" :assembly-name "n" :camber-deviation-actual 0.05
                                   :camber-deviation-min -0.10 :camber-deviation-max 0.10
                                   :nde-defect-unresolved? false
                                   :assembly-dispatched? false :fabrication-certified? false
                                   :jurisdiction "JPN" :status :intake}})
    (is (= "n" (:assembly-name (store/assembly s "x"))))))
