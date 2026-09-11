(ns rubberworks.governor-contract-test
  "The governor contract as executable tests -- the rubber-products-
  manufacturer analog of `automotive.governor-contract-test`
  (`cloud-itonami-isic-2910`) / `opticsworks.governor-contract-test`
  (`cloud-itonami-isic-2670`). The single invariant under test:

    Rubber Advisor never ships a rubber-part-batch or issues a Rubber
    Compound Test Certificate the Compression-Seal Governor would
    reject, `:actuation/ship-rubber-part-batch`/`:actuation/issue-
    material-certificate` NEVER auto-commit at any phase, `:rubber-
    part-batch/intake` (no direct capital risk) MAY auto-commit when
    clean, and every decision (commit OR hold) leaves exactly one
    ledger fact.

  NOTE on independence (see `rubberworks.governor`/`rubberworks.
  robotics` ns docstrings): UNLIKE `cementmill.governor`'s co-firing
  compressive-strength checks (which key off the SAME field family),
  this actor's `robotics-simulation-violations` (compression-force,
  physics-derived) and `rubber-part-batch-durometer-deviation-out-of-
  range-violations` (durometer, a distinct post-cure hardness
  measurement) key off TWO DISTINCT fields -- the same 'two distinct
  fields' shape `automotive.governor`/`opticsworks.governor` use. Tests
  below isolate each rule by construction (batch-3 for durometer alone,
  batch-5/batch-6 for the physics-derived over-/under-compression checks
  alone)."
  (:require [clojure.test :refer [deftest is testing]]
            [langgraph.graph :as g]
            [rubberworks.store :as store]
            [rubberworks.operation :as op]))

(defn- fresh []
  (let [db (store/seed-db)]
    [db (op/build db)]))

(def operator {:actor-id "op-1" :actor-role :quality-engineer :phase 3})

(defn- exec-op [actor tid request context]
  (g/run* actor {:request request :context context} {:thread-id tid}))

(defn- approve! [actor tid]
  (g/run* actor {:approval {:status :approved :by "op-1"}} {:thread-id tid :resume? true}))

(defn- verify!
  "Walks `subject` through verify -> approve, leaving a requirements
  verification on file. Uses distinct thread-ids per call site by
  suffixing `tid-prefix`."
  [actor tid-prefix subject]
  (exec-op actor (str tid-prefix "-verify") {:op :material-spec-rules/verify :subject subject} operator)
  (approve! actor (str tid-prefix "-verify")))

(defn- screen!
  "Walks `subject` through end-of-line screening -> approve, leaving a
  screening on file. Only safe to call for a batch whose end-of-line
  status has already resolved -- an unresolved finding HARD-holds the
  screen itself (see `end-of-line-defect-unresolved-is-held-and-
  unoverridable`)."
  [actor tid-prefix subject]
  (exec-op actor (str tid-prefix "-screen") {:op :end-of-line-quality/screen :subject subject} operator)
  (approve! actor (str tid-prefix "-screen")))

(defn- simulate-robotics!
  "Walks `subject` through the robot compression-set-test-cell
  verification mission -> approve, leaving `:robotics-sim-verified?` on
  file. This ACTUALLY runs the real `physics-2d`-backed compression-
  collision simulation for the batch's own :compression-platen-mass-kg
  (ADR-2607152000) -- only meaningful to call for a batch whose real
  simulated compression reading is actually within tolerance -- an
  out-of-tolerance batch still gets :robotics-sim-verified? recorded
  (per whatever the mission itself found: false), but `rubberworks.
  governor`'s independent recheck HARD-holds regardless."
  [actor tid-prefix subject]
  (exec-op actor (str tid-prefix "-robotics") {:op :robotics/simulate-compression-set-test :subject subject} operator)
  (approve! actor (str tid-prefix "-robotics")))

(deftest clean-intake-auto-commits
  (let [[db actor] (fresh)
        res (exec-op actor "t1"
                  {:op :rubber-part-batch/intake :subject "batch-1"
                   :patch {:id "batch-1" :batch-name "Meridian Waterproofing Gasket Batch RG-4401"}} operator)]
    (is (= :commit (get-in res [:state :disposition])))
    (is (= "Meridian Waterproofing Gasket Batch RG-4401" (:batch-name (store/rubber-part-batch db "batch-1"))) "SSoT actually updated")
    (is (= 1 (count (store/ledger db))))))

(deftest requirements-verify-always-needs-approval
  (testing "verify is never in any phase's :auto set -- always human approval, even when clean"
    (let [[db actor] (fresh)
          res (exec-op actor "t2" {:op :material-spec-rules/verify :subject "batch-1"} operator)]
      (is (= :interrupted (:status res)))
      (let [r2 (approve! actor "t2")]
        (is (= :commit (get-in r2 [:state :disposition])))
        (is (some? (store/material-spec-verification-of db "batch-1")))))))

(deftest fabricated-product-class-is-held
  (testing "a material-spec-rules/verify proposal with no official spec-basis -> HOLD, never reaches a human"
    (let [[db actor] (fresh)
          res (exec-op actor "t3"
                    {:op :material-spec-rules/verify :subject "batch-1" :no-spec? true} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:no-spec-basis} (-> (store/ledger db) first :basis)))
      (is (nil? (store/material-spec-verification-of db "batch-1")) "no verification written"))))

(deftest ship-rubber-part-batch-without-verification-is-held
  (testing "actuation/ship-rubber-part-batch before any requirements verification -> HOLD (evidence incomplete)"
    (let [[db actor] (fresh)
          res (exec-op actor "t4" {:op :actuation/ship-rubber-part-batch :subject "batch-1"} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:evidence-incomplete} (-> (store/ledger db) first :basis))))))

(deftest durometer-deviation-out-of-range-is-held-isolated-from-robotics
  (testing "batch-3's own durometer deviation (6.0 points) falls outside its own [-3,3] acceptance-band bounds -> HOLD, isolated from the robotics check by running the compression-set-test mission first (its compression force is clean/in-band)"
    (let [[db actor] (fresh)
          _ (verify! actor "t5pre" "batch-3")
          _ (simulate-robotics! actor "t5pre2" "batch-3")
          res (exec-op actor "t5" {:op :actuation/ship-rubber-part-batch :subject "batch-3"} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:rubber-part-batch-durometer-deviation-out-of-range} (-> (store/ledger db) last :basis)))
      (is (not (some #{:robotics-simulation-out-of-tolerance} (-> (store/ledger db) last :basis)))
          "batch-3's compression force is in-range, so check 3 does not co-fire here")
      (is (empty? (store/shipment-history db))))))

(deftest end-of-line-defect-unresolved-is-held-and-unoverridable
  (testing "an unresolved end-of-line defect on a batch -> HOLD, and never reaches request-approval -- exercised via :end-of-line-quality/screen DIRECTLY, not via the actuation op against an unscreened batch (see this actor's governor ns docstring / automotive's [cloud-itonami-isic-2910] and its own prior siblings' ADR-0001s)"
    (let [[db actor] (fresh)
          res (exec-op actor "t6" {:op :end-of-line-quality/screen :subject "batch-4"} operator)]
      (is (= :hold (get-in res [:state :disposition])) "settles immediately, no interrupt")
      (is (not= :interrupted (:status res)))
      (is (some #{:end-of-line-defect-unresolved} (-> (store/ledger db) first :basis)))
      (is (nil? (store/eol-screen-of db "batch-4")) "no clearance written"))))

(deftest ship-rubber-part-batch-always-escalates-then-human-decides
  (testing "a clean, fully-verified, in-spec batch still ALWAYS interrupts for human approval -- actuation/ship-rubber-part-batch is never auto"
    (let [[db actor] (fresh)
          _ (verify! actor "t7pre" "batch-1")
          _ (simulate-robotics! actor "t7pre2" "batch-1")
          r1 (exec-op actor "t7" {:op :actuation/ship-rubber-part-batch :subject "batch-1"} operator)]
      (is (= :interrupted (:status r1)) "pauses for human approval even when governor-clean")
      (testing "approve -> commit, shipment record drafted"
        (let [r2 (approve! actor "t7")]
          (is (= :commit (get-in r2 [:state :disposition])))
          (is (true? (:rubber-part-batch-shipped? (store/rubber-part-batch db "batch-1"))))
          (is (= 1 (count (store/shipment-history db))) "one draft shipment record"))))))

(deftest issue-material-certificate-always-escalates-then-human-decides
  (testing "a clean, fully-verified, resolved-defect batch still ALWAYS interrupts for human approval -- actuation/issue-material-certificate is never auto"
    (let [[db actor] (fresh)
          _ (verify! actor "t8pre" "batch-1")
          _ (screen! actor "t8pre2" "batch-1")
          r1 (exec-op actor "t8" {:op :actuation/issue-material-certificate :subject "batch-1"} operator)]
      (is (= :interrupted (:status r1)) "pauses for human approval even when governor-clean")
      (testing "approve -> commit, certificate record drafted"
        (let [r2 (approve! actor "t8")]
          (is (= :commit (get-in r2 [:state :disposition])))
          (is (true? (:material-certified? (store/rubber-part-batch db "batch-1"))))
          (is (= 1 (count (store/certificate-history db))) "one draft certificate record"))))))

(deftest ship-rubber-part-batch-double-shipment-is-held
  (testing "shipping the same batch twice -> HOLD on the second attempt"
    (let [[db actor] (fresh)
          _ (verify! actor "t9pre" "batch-1")
          _ (simulate-robotics! actor "t9pre2" "batch-1")
          _ (exec-op actor "t9a" {:op :actuation/ship-rubber-part-batch :subject "batch-1"} operator)
          _ (approve! actor "t9a")
          res (exec-op actor "t9" {:op :actuation/ship-rubber-part-batch :subject "batch-1"} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:already-shipped} (-> (store/ledger db) last :basis)))
      (is (= 1 (count (store/shipment-history db))) "still only the one earlier shipment"))))

(deftest issue-material-certificate-double-issuance-is-held
  (testing "issuing the same batch's Rubber Compound Test Certificate twice -> HOLD on the second attempt"
    (let [[db actor] (fresh)
          _ (verify! actor "t10pre" "batch-1")
          _ (screen! actor "t10pre2" "batch-1")
          _ (exec-op actor "t10a" {:op :actuation/issue-material-certificate :subject "batch-1"} operator)
          _ (approve! actor "t10a")
          res (exec-op actor "t10" {:op :actuation/issue-material-certificate :subject "batch-1"} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:already-certified} (-> (store/ledger db) last :basis)))
      (is (= 1 (count (store/certificate-history db))) "still only the one earlier certificate issuance"))))

(deftest robotics-simulation-always-needs-approval
  (testing "robotics/simulate-compression-set-test is never in any phase's :auto set -- always human approval, even when clean"
    (let [[db actor] (fresh)
          res (exec-op actor "t11" {:op :robotics/simulate-compression-set-test :subject "batch-1"} operator)]
      (is (= :interrupted (:status res)))
      (let [r2 (approve! actor "t11")]
        (is (= :commit (get-in r2 [:state :disposition])))
        (is (true? (:robotics-sim-verified? (store/rubber-part-batch db "batch-1"))))))))

(deftest ship-rubber-part-batch-without-robotics-simulation-is-held
  (testing "actuation/ship-rubber-part-batch before the robot compression-set-test-cell mission ever ran -> HOLD (robotics-simulation-missing) -- batch-1's durometer deviation is in-range so this isolates the check-3 'missing' rule from check-4"
    (let [[db actor] (fresh)
          _ (verify! actor "t12pre" "batch-1")
          res (exec-op actor "t12" {:op :actuation/ship-rubber-part-batch :subject "batch-1"} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:robotics-simulation-missing} (-> (store/ledger db) last :basis)))
      (is (not (some #{:rubber-part-batch-durometer-deviation-out-of-range} (-> (store/ledger db) last :basis)))
          "batch-1's durometer deviation is in-range, so check 4 does not co-fire here")
      (is (empty? (store/shipment-history db))))))

(deftest robotics-simulation-out-of-tolerance-over-compressed-is-held
  (testing "batch-5 has a robotics-sim already on file, but its own REAL physics-2d-simulated compression telemetry (:sim-peak-compression-force-n -- ADR-2607152000) falls OUTSIDE its own real [:compression-force-min-n :compression-force-max-n] band on INDEPENDENT recheck -> HOLD, never trusts the on-file verdict alone. batch-5 is DELIBERATELY compression-tested with an unrealistically heavy 5.0kg platen-mass configuration in the demo fixture (rubberworks.store/demo-data) -- a genuine press-run-record inconsistency (over-compressed, 1600.0N > 1200.0N max) the real, re-run simulation catches."
    (let [[db actor] (fresh)
          _ (verify! actor "t13pre" "batch-5")
          res (exec-op actor "t13" {:op :actuation/ship-rubber-part-batch :subject "batch-5"} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:robotics-simulation-out-of-tolerance} (-> (store/ledger db) last :basis)))
      (is (= 1600.0 (:sim-peak-compression-force-n (store/rubber-part-batch db "batch-5"))))
      (is (empty? (store/shipment-history db))))))

(deftest robotics-simulation-out-of-tolerance-under-compressed-is-held
  (testing "batch-6 has a robotics-sim already on file, but its own REAL physics-2d-simulated compression telemetry falls BELOW its own real [:compression-force-min-n :compression-force-max-n] band on INDEPENDENT recheck -> HOLD. batch-6 is DELIBERATELY compression-tested with an unrealistically light 0.1kg platen-mass configuration -- a genuine under-compressed (32.0N < 50.0N min) press-run-record inconsistency the real, re-run simulation ALSO catches -- the opposite-direction failure this actor's own two-sided compression-force check must catch (unlike a one-sided clamp-tonnage check)."
    (let [[db actor] (fresh)
          _ (verify! actor "t14pre" "batch-6")
          res (exec-op actor "t14" {:op :actuation/ship-rubber-part-batch :subject "batch-6"} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:robotics-simulation-out-of-tolerance} (-> (store/ledger db) last :basis)))
      (is (= 32.0 (:sim-peak-compression-force-n (store/rubber-part-batch db "batch-6"))))
      (is (empty? (store/shipment-history db))))))

(deftest every-decision-leaves-one-ledger-fact
  (testing "write-only-through-ledger: N operations -> N ledger facts"
    (let [[db actor] (fresh)]
      (exec-op actor "a" {:op :rubber-part-batch/intake :subject "batch-1"
                          :patch {:id "batch-1" :batch-name "Meridian Waterproofing Gasket Batch RG-4401"}} operator)
      (exec-op actor "b" {:op :material-spec-rules/verify :subject "batch-1" :no-spec? true} operator)
      (is (= 2 (count (store/ledger db)))
          "one commit + one hold, both recorded"))))
