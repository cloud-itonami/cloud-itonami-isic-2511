(ns structuralsteel.facts-test
  (:require [clojure.test :refer [deftest is]]
            [structuralsteel.facts :as facts]))

(deftest jpn-has-a-spec-basis
  (is (some? (facts/spec-basis "JPN")))
  (is (string? (:provenance (facts/spec-basis "JPN")))))

(deftest pol-has-a-spec-basis-with-the-same-shape-as-the-other-jurisdictions
  (let [pol (facts/spec-basis "POL")]
    (is (some? pol))
    (is (= "Poland" (:name pol)))
    (is (= (set (keys pol)) (set (keys (facts/spec-basis "JPN")))))
    (is (string? (:owner-authority pol)))
    (is (string? (:legal-basis pol)))
    (is (string? (:national-spec pol)))
    (is (string? (:provenance pol)))
    (is (= 4 (count (:required-evidence pol))))))

(deftest pol-required-evidence-is-satisfied-by-its-own-checklist
  (let [all (facts/evidence-checklist "POL")]
    (is (facts/required-evidence-satisfied? "POL" all))
    (is (not (facts/required-evidence-satisfied? "POL" (rest all))))))

(deftest unknown-jurisdiction-has-no-fabricated-spec-basis
  (is (nil? (facts/spec-basis "ATL"))))

(deftest coverage-never-reports-a-missing-jurisdiction-as-covered
  (let [report (facts/coverage ["JPN" "ATL" "GBR"])]
    (is (= 2 (:covered report)))
    (is (= ["ATL"] (:missing-jurisdictions report)))
    (is (= ["GBR" "JPN"] (:covered-jurisdictions report)))))

(deftest required-evidence-satisfied-needs-every-item
  (let [all (facts/evidence-checklist "JPN")]
    (is (facts/required-evidence-satisfied? "JPN" all))
    (is (not (facts/required-evidence-satisfied? "JPN" (rest all))))
    (is (not (facts/required-evidence-satisfied? "ATL" all)) "no spec-basis -> never satisfied")))
