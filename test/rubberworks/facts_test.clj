(ns rubberworks.facts-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [rubberworks.facts :as facts]))

(deftest automotive-rubber-has-a-spec-basis
  (is (some? (facts/spec-basis "AUTOMOTIVE-RUBBER")))
  (is (string? (:provenance (facts/spec-basis "AUTOMOTIVE-RUBBER")))))

(deftest electronics-gasket-has-a-spec-basis
  (is (some? (facts/spec-basis "ELECTRONICS-GASKET"))))

(deftest unknown-product-class-has-no-fabricated-spec-basis
  (is (nil? (facts/spec-basis "MEDDEV-RUBBER"))))

(deftest coverage-never-reports-a-missing-product-class-as-covered
  (let [report (facts/coverage ["AUTOMOTIVE-RUBBER" "MEDDEV-RUBBER" "ELECTRONICS-GASKET"])]
    (is (= 2 (:covered report)))
    (is (= ["MEDDEV-RUBBER"] (:missing-jurisdictions report)))
    (is (= ["AUTOMOTIVE-RUBBER" "ELECTRONICS-GASKET"] (:covered-jurisdictions report)))))

(deftest required-evidence-satisfied-needs-every-item
  (let [all (facts/evidence-checklist "AUTOMOTIVE-RUBBER")]
    (is (facts/required-evidence-satisfied? "AUTOMOTIVE-RUBBER" all))
    (is (not (facts/required-evidence-satisfied? "AUTOMOTIVE-RUBBER" (rest all))))
    (is (not (facts/required-evidence-satisfied? "MEDDEV-RUBBER" all)) "no spec-basis -> never satisfied")))

(deftest automotive-rubber-cites-astm-d2000-and-iso-1817
  (let [sb (facts/spec-basis "AUTOMOTIVE-RUBBER")]
    (is (str/includes? (:legal-basis sb) "ASTM D2000"))
    (is (str/includes? (:legal-basis sb) "ISO 1817"))))

(deftest electronics-gasket-cites-iec-60529-and-astm-d395
  (let [sb (facts/spec-basis "ELECTRONICS-GASKET")]
    (is (str/includes? (:legal-basis sb) "IEC 60529"))
    (is (str/includes? (:legal-basis sb) "ASTM D395"))))

(deftest distinct-from-tyres-vertical
  ;; This actor covers "other rubber products" (seals/gaskets/hoses/
  ;; mounts/cases) -- distinct from cloud-itonami-isic-2211 (rubber
  ;; tyres, already implemented separately). Neither product-class
  ;; scheme in this catalog mentions tyres.
  (doseq [scheme (keys facts/catalog)]
    (is (not (str/includes? (str/lower-case (:name (facts/spec-basis scheme))) "tyre")))
    (is (not (str/includes? (str/lower-case (:name (facts/spec-basis scheme))) "tire")))))
