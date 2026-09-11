(ns rubberworks.registry-test
  (:require [clojure.test :refer [deftest is]]
            [rubberworks.registry :as r]))

;; ----------------------------- rubber-part-batch-durometer-deviation-out-of-range? -----------------------------

(deftest not-out-of-range-when-within-bounds
  (is (not (r/rubber-part-batch-durometer-deviation-out-of-range? {:durometer-deviation-actual-shore-a 0.5 :durometer-deviation-min-shore-a -3.0 :durometer-deviation-max-shore-a 3.0})))
  (is (not (r/rubber-part-batch-durometer-deviation-out-of-range? {:durometer-deviation-actual-shore-a -3.0 :durometer-deviation-min-shore-a -3.0 :durometer-deviation-max-shore-a 3.0})))
  (is (not (r/rubber-part-batch-durometer-deviation-out-of-range? {:durometer-deviation-actual-shore-a 3.0 :durometer-deviation-min-shore-a -3.0 :durometer-deviation-max-shore-a 3.0}))))

(deftest out-of-range-when-below-minimum-or-above-maximum
  (is (r/rubber-part-batch-durometer-deviation-out-of-range? {:durometer-deviation-actual-shore-a -4.0 :durometer-deviation-min-shore-a -3.0 :durometer-deviation-max-shore-a 3.0}))
  (is (r/rubber-part-batch-durometer-deviation-out-of-range? {:durometer-deviation-actual-shore-a 6.0 :durometer-deviation-min-shore-a -3.0 :durometer-deviation-max-shore-a 3.0})))

(deftest out-of-range-is-false-on-missing-fields
  (is (not (r/rubber-part-batch-durometer-deviation-out-of-range? {})))
  (is (not (r/rubber-part-batch-durometer-deviation-out-of-range? {:durometer-deviation-actual-shore-a 6.0}))))

;; ----------------------------- register-rubber-part-batch-shipment -----------------------------

(deftest shipment-is-a-draft-not-a-real-shipment
  (let [result (r/register-rubber-part-batch-shipment "batch-1" "ELECTRONICS-GASKET" 0)]
    (is (nil? (get-in result ["certificate" "proof"])))
    (is (= (get-in result ["certificate" "issued_by_registry"]) false))
    (is (= (get-in result ["certificate" "status"]) "draft-unsigned"))))

(deftest shipment-assigns-shipment-number
  (let [result (r/register-rubber-part-batch-shipment "batch-1" "ELECTRONICS-GASKET" 7)]
    (is (= (get result "shipment_number") "ELECTRONICS-GASKET-RPS-000007"))
    (is (= (get-in result ["record" "batch_id"]) "batch-1"))
    (is (= (get-in result ["record" "kind"]) "rubber-part-batch-shipment-draft"))
    (is (= (get-in result ["record" "immutable"]) true))))

(deftest shipment-validation-rules
  (is (thrown? Exception (r/register-rubber-part-batch-shipment "" "ELECTRONICS-GASKET" 0)))
  (is (thrown? Exception (r/register-rubber-part-batch-shipment "batch-1" "" 0)))
  (is (thrown? Exception (r/register-rubber-part-batch-shipment "batch-1" "ELECTRONICS-GASKET" -1))))

;; ----------------------------- register-material-certificate -----------------------------

(deftest certificate-is-a-draft-not-real-certification
  (let [result (r/register-material-certificate "batch-1" "ELECTRONICS-GASKET" 0)]
    (is (nil? (get-in result ["certificate" "proof"])))
    (is (= (get-in result ["certificate" "issued_by_registry"]) false))
    (is (= (get-in result ["certificate" "status"]) "draft-unsigned"))))

(deftest certificate-assigns-certificate-number
  (let [result (r/register-material-certificate "batch-1" "ELECTRONICS-GASKET" 3)]
    (is (= (get result "certificate_number") "ELECTRONICS-GASKET-RMC-000003"))
    (is (= (get-in result ["record" "batch_id"]) "batch-1"))
    (is (= (get-in result ["record" "kind"]) "material-certificate-draft"))
    (is (= (get-in result ["record" "immutable"]) true))))

(deftest certificate-validation-rules
  (is (thrown? Exception (r/register-material-certificate "" "ELECTRONICS-GASKET" 0)))
  (is (thrown? Exception (r/register-material-certificate "batch-1" "" 0)))
  (is (thrown? Exception (r/register-material-certificate "batch-1" "ELECTRONICS-GASKET" -1))))

(deftest history-is-append-only
  (let [c1 (r/register-rubber-part-batch-shipment "batch-1" "ELECTRONICS-GASKET" 0)
        hist (r/append [] c1)
        c2 (r/register-rubber-part-batch-shipment "batch-2" "ELECTRONICS-GASKET" 1)
        hist2 (r/append hist c2)]
    (is (= 2 (count hist2)))
    (is (= "ELECTRONICS-GASKET-RPS-000000" (get-in hist2 [0 "record_id"])))
    (is (= "ELECTRONICS-GASKET-RPS-000001" (get-in hist2 [1 "record_id"])))))
