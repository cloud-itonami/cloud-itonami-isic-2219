(ns rubberworks.store-contract-test
  "The Store contract, run against BOTH backends. Proving MemStore and
  the Datomic-backed (langchain.db) store satisfy the same contract is
  what makes 'swap the SSoT for Datomic / kotoba-server' a
  configuration change, not a rewrite -- see `cloud-itonami-isic-6511`'s
  `underwriting.store-contract-test` for the same pattern on the
  sibling actor."
  (:require [clojure.test :refer [deftest is testing]]
            [rubberworks.robotics :as robotics]
            [rubberworks.store :as store]))

(defn- backends []
  [["MemStore" (store/seed-db)] ["DatomicStore" (store/datomic-seed-db)]])

(deftest read-parity
  (doseq [[label s] (backends)]
    (testing label
      (is (= "Meridian Waterproofing Gasket Batch RG-4401" (:batch-name (store/rubber-part-batch s "batch-1"))))
      (is (= "ELECTRONICS-GASKET" (:jurisdiction (store/rubber-part-batch s "batch-1"))))
      (is (= 0.5 (:durometer-deviation-actual-shore-a (store/rubber-part-batch s "batch-1"))))
      (is (= -3.0 (:durometer-deviation-min-shore-a (store/rubber-part-batch s "batch-1"))))
      (is (= 3.0 (:durometer-deviation-max-shore-a (store/rubber-part-batch s "batch-1"))))
      (is (false? (:rubber-part-batch-defect-unresolved? (store/rubber-part-batch s "batch-1"))))
      (is (= 6.0 (:durometer-deviation-actual-shore-a (store/rubber-part-batch s "batch-3"))))
      (is (true? (:rubber-part-batch-defect-unresolved? (store/rubber-part-batch s "batch-4"))))
      (is (false? (:robotics-sim-verified? (store/rubber-part-batch s "batch-1"))) "no robotics mission has run yet")
      (is (true? (:robotics-sim-verified? (store/rubber-part-batch s "batch-5"))) "seeded as already-on-file")
      (is (true? (:robotics-sim-verified? (store/rubber-part-batch s "batch-6"))) "seeded as already-on-file")
      (is (= 5.0 (:compression-platen-mass-kg (store/rubber-part-batch s "batch-5"))))
      (is (= 0.1 (:compression-platen-mass-kg (store/rubber-part-batch s "batch-6"))))
      (is (> (:sim-peak-compression-force-n (store/rubber-part-batch s "batch-5"))
             (:compression-force-max-n (store/rubber-part-batch s "batch-5")))
          "batch-5's real physics-2d-simulated compression force (over-compressed) exceeds its own max acceptance band")
      (is (< (:sim-peak-compression-force-n (store/rubber-part-batch s "batch-6"))
             (:compression-force-min-n (store/rubber-part-batch s "batch-6")))
          "batch-6's real physics-2d-simulated compression force (under-compressed) falls below its own min acceptance band")
      (is (>= (:sim-peak-compression-force-n (store/rubber-part-batch s "batch-1"))
              (:compression-force-min-n (store/rubber-part-batch s "batch-1")))
          "batch-1's real physics-2d-simulated compression force clears its own min acceptance band")
      (is (<= (:sim-peak-compression-force-n (store/rubber-part-batch s "batch-1"))
              (:compression-force-max-n (store/rubber-part-batch s "batch-1")))
          "batch-1's real physics-2d-simulated compression force clears its own max acceptance band")
      (is (= 160.0 (:sim-peak-compression-force-n (store/rubber-part-batch s "batch-1"))))
      (is (= 640.0 (:sim-peak-compression-force-n (store/rubber-part-batch s "batch-3"))))
      (is (= 1600.0 (:sim-peak-compression-force-n (store/rubber-part-batch s "batch-5"))))
      (is (= 32.0 (:sim-peak-compression-force-n (store/rubber-part-batch s "batch-6"))))
      (is (false? (:rubber-part-batch-shipped? (store/rubber-part-batch s "batch-1"))))
      (is (false? (:material-certified? (store/rubber-part-batch s "batch-1"))))
      (is (= ["batch-1" "batch-2" "batch-3" "batch-4" "batch-5" "batch-6"]
             (mapv :id (store/all-rubber-part-batches s))))
      (is (nil? (store/eol-screen-of s "batch-1")))
      (is (nil? (store/material-spec-verification-of s "batch-1")))
      (is (= [] (store/ledger s)))
      (is (= [] (store/shipment-history s)))
      (is (= [] (store/certificate-history s)))
      (is (zero? (store/next-shipment-sequence s "ELECTRONICS-GASKET")))
      (is (zero? (store/next-certificate-sequence s "ELECTRONICS-GASKET")))
      (is (false? (store/batch-already-shipped? s "batch-1")))
      (is (false? (store/batch-already-certified? s "batch-1"))))))

(deftest write-and-ledger-parity
  (doseq [[label s] (backends)]
    (testing label
      (testing "partial upsert merges, preserving untouched fields"
        (store/commit-record! s {:effect :rubber-part-batch/upsert
                                 :value {:id "batch-1" :batch-name "Meridian Waterproofing Gasket Batch RG-4401"}})
        (is (= "Meridian Waterproofing Gasket Batch RG-4401" (:batch-name (store/rubber-part-batch s "batch-1"))))
        (is (= "ELECTRONICS-GASKET" (:jurisdiction (store/rubber-part-batch s "batch-1"))) "unrelated field preserved"))
      (testing "robotics-sim result commits via :rubber-part-batch/upsert and reads back"
        (store/commit-record! s {:effect :rubber-part-batch/upsert
                                 :value {:id "batch-1" :robotics-sim-verified? true
                                        :robotics-sim-record {:mission-id "m-1" :passed? true}}})
        (is (true? (:robotics-sim-verified? (store/rubber-part-batch s "batch-1"))))
        (is (= {:mission-id "m-1" :passed? true} (:robotics-sim-record (store/rubber-part-batch s "batch-1"))))
        (is (= "ELECTRONICS-GASKET" (:jurisdiction (store/rubber-part-batch s "batch-1"))) "unrelated field still preserved"))
      (testing "verification / eol-screen payloads commit and read back"
        (store/commit-record! s {:effect :material-spec-verification/set :path ["batch-1"]
                                 :payload {:jurisdiction "ELECTRONICS-GASKET" :checklist ["a" "b"]}})
        (is (= {:jurisdiction "ELECTRONICS-GASKET" :checklist ["a" "b"]} (store/material-spec-verification-of s "batch-1")))
        (store/commit-record! s {:effect :eol-screen/set :path ["batch-1"]
                                 :payload {:batch-id "batch-1" :verdict :resolved}})
        (is (= {:batch-id "batch-1" :verdict :resolved} (store/eol-screen-of s "batch-1"))))
      (testing "rubber-part-batch shipment drafts a record and advances the sequence"
        (store/commit-record! s {:effect :rubber-part-batch/mark-shipped :path ["batch-1"]})
        (is (= "ELECTRONICS-GASKET-RPS-000000" (get (first (store/shipment-history s)) "record_id")))
        (is (= "rubber-part-batch-shipment-draft" (get (first (store/shipment-history s)) "kind")))
        (is (true? (:rubber-part-batch-shipped? (store/rubber-part-batch s "batch-1"))))
        (is (= 1 (count (store/shipment-history s))))
        (is (= 1 (store/next-shipment-sequence s "ELECTRONICS-GASKET")))
        (is (true? (store/batch-already-shipped? s "batch-1")))
        (is (false? (store/batch-already-shipped? s "batch-2"))))
      (testing "Rubber Compound Test Certificate drafts a record and advances the sequence"
        (store/commit-record! s {:effect :rubber-part-batch/mark-certified :path ["batch-1"]})
        (is (= "ELECTRONICS-GASKET-RMC-000000" (get (first (store/certificate-history s)) "record_id")))
        (is (= "material-certificate-draft" (get (first (store/certificate-history s)) "kind")))
        (is (true? (:material-certified? (store/rubber-part-batch s "batch-1"))))
        (is (= 1 (count (store/certificate-history s))))
        (is (= 1 (store/next-certificate-sequence s "ELECTRONICS-GASKET")))
        (is (true? (store/batch-already-certified? s "batch-1")))
        (is (false? (store/batch-already-certified? s "batch-2"))))
      (testing "ledger is append-only and order-preserving"
        (store/append-ledger! s {:op :a :disposition :commit})
        (store/append-ledger! s {:op :b :disposition :hold})
        (is (= [:commit :hold] (mapv :disposition (store/ledger s))))))))

(deftest datomic-empty-store-is-usable
  (let [s (store/datomic-store)]
    (is (nil? (store/rubber-part-batch s "nope")))
    (is (= [] (store/all-rubber-part-batches s)))
    (is (= [] (store/ledger s)))
    (is (= [] (store/shipment-history s)))
    (is (= [] (store/certificate-history s)))
    (is (zero? (store/next-shipment-sequence s "ELECTRONICS-GASKET")))
    (is (zero? (store/next-certificate-sequence s "ELECTRONICS-GASKET")))
    (store/with-rubber-part-batches s {"x" {:id "x" :batch-name "n"
                                     :durometer-deviation-actual-shore-a 0.5
                                     :durometer-deviation-min-shore-a -3.0
                                     :durometer-deviation-max-shore-a 3.0
                                     :rubber-part-batch-defect-unresolved? false
                                     :rubber-part-batch-shipped? false :material-certified? false
                                     :jurisdiction "ELECTRONICS-GASKET" :status :intake}})
    (is (= "n" (:batch-name (store/rubber-part-batch s "x"))))))

(deftest compression-force-band-for-matches-the-stored-demo-bands
  (testing "the demo-data compression-force-min/max fields were seeded from robotics/compression-force-band-for -- keep them in sync"
    (is (= (robotics/compression-force-band-for :soft-gasket-grade)
           {:min (:compression-force-min-n (store/rubber-part-batch (store/seed-db) "batch-1"))
            :max (:compression-force-max-n (store/rubber-part-batch (store/seed-db) "batch-1"))}))
    (is (= (robotics/compression-force-band-for :structural-mount-grade)
           {:min (:compression-force-min-n (store/rubber-part-batch (store/seed-db) "batch-3"))
            :max (:compression-force-max-n (store/rubber-part-batch (store/seed-db) "batch-3"))}))))
