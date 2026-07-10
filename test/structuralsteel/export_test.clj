(ns structuralsteel.export-test
  "Audit-package export contract -- social/regulatory hand-off shape."
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [langgraph.graph :as g]
            [structuralsteel.export :as export]
            [structuralsteel.operation :as op]
            [structuralsteel.store :as store]))

(def operator {:actor-id "op-1" :actor-role :certified-welding-inspector :phase 3})

(defn- exec! [actor tid request]
  (g/run* actor {:request request :context operator} {:thread-id tid}))

(defn- approve! [actor tid]
  (g/run* actor {:approval {:status :approved :by "op-1"}}
          {:thread-id tid :resume? true}))

(defn- seed-with-one-dispatch []
  (let [db (store/seed-db)
        actor (op/build db)]
    (exec! actor "v" {:op :welding-procedure/verify :subject "assembly-1"})
    (approve! actor "v")
    (exec! actor "d" {:op :actuation/dispatch-assembly :subject "assembly-1"})
    (approve! actor "d")
    db))

(deftest audit-package-shape
  (let [db (seed-with-one-dispatch)
        pkg (export/audit-package db)]
    (is (= "2511" (:isic pkg)))
    (is (= "cloud-itonami-isic-2511" (:business-id pkg)))
    (is (= :edn-maps (:format pkg)))
    (is (pos? (get-in pkg [:counts :ledger])))
    (is (= 1 (get-in pkg [:counts :dispatches])))
    (is (some #(= "assembly-1" (:id %)) (:assemblies pkg)))
    (is (true? (:assembly-dispatched?
                (first (filter #(= "assembly-1" (:id %)) (:assemblies pkg))))))))

(deftest csv-bundle-has-headers-and-rows
  (let [db (seed-with-one-dispatch)
        bundle (export/package->csv-bundle db)]
    (is (every? bundle ["assemblies.csv" "ledger.csv" "dispatches.csv" "fabrication-certificates.csv"]))
    (is (str/starts-with? (get bundle "assemblies.csv") "id,assembly-name,"))
    (is (re-find #"assembly-1" (get bundle "assemblies.csv")))
    (is (re-find #"JPN-ASM-000000" (get bundle "dispatches.csv")))
    (is (re-find #":actuation/dispatch-assembly" (get bundle "ledger.csv")))))

(deftest empty-store-export-is-usable
  (let [db (store/seed-db)
        pkg (export/audit-package db)
        bundle (export/package->csv-bundle db)]
    (is (= 0 (get-in pkg [:counts :dispatches])))
    (is (= 4 (get-in pkg [:counts :assemblies])))
    (is (str/includes? (get bundle "ledger.csv") "seq,t,op"))))
