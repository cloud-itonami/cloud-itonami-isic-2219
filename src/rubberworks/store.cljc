(ns rubberworks.store
  "SSoT for the rubber-products-manufacturer actor, behind a `Store`
  protocol so the backend is a swap, not a rewrite -- the same seam
  every prior `cloud-itonami-isic-*` actor in this fleet uses:

    - `MemStore`     -- atom of EDN. The deterministic default for
                        dev/tests/demo (no deps).
    - `DatomicStore` -- backed by `langchain.db`, a Datomic-API-compatible
                        EAV store (datalog q / pull / upsert). Pure `.cljc`,
                        so it runs offline AND can be pointed at a real
                        Datomic Local or a kotoba-server pod by swapping
                        `langchain.db`'s `:db-api` (see langchain.kotoba-db).

  Both implement the same protocol and pass the same contract
  (test/rubberworks/store_contract_test.clj), which is the whole point:
  the actor, the Compression-Seal Governor and the audit ledger never
  know which SSoT they run on.

  Like `moldworks.store`'s dual molding-run-batch-shipment/material-
  certificate history and `opticsworks.store`'s dual optical-module-
  batch-shipment/optical-certificate history, this actor has TWO
  actuation events (shipping a rubber-part-batch onward to a downstream
  consumer, issuing a Rubber Compound Test Certificate) acting on the
  SAME entity (a rubber-part-batch), each with its OWN history
  collection, sequence counter and dedicated double-actuation-guard
  boolean (`:rubber-part-batch-shipped?`/`:material-certified?`, never a
  `:status` value) -- the same discipline every prior sibling
  governor's guards establish, informed by `cloud-itonami-isic-6492`'s
  status-lifecycle bug (ADR-2607071320).

  The ledger stays append-only on every backend: 'which rubber-part-
  batch was screened for an unresolved end-of-line defect, which
  rubber-part-batch shipment was dispatched onward to a downstream
  consumer, which Rubber Compound Test Certificate was issued, on what
  product-class basis, approved by whom' is always a query over an
  immutable log -- the audit trail a community trusting a rubber-
  products manufacturer needs, and the evidence a plant needs if a
  shipment or certificate decision is later disputed."
  (:require #?(:clj  [clojure.edn :as edn]
               :cljs [cljs.reader :as edn])
            [rubberworks.registry :as registry]
            [rubberworks.robotics :as robotics]
            [langchain.db :as d]))

(defprotocol Store
  (rubber-part-batch [s id])
  (all-rubber-part-batches [s])
  (eol-screen-of [s batch-id] "committed end-of-line quality screening verdict for a batch, or nil")
  (material-spec-verification-of [s batch-id] "committed material-spec-rules evidence verification, or nil")
  (ledger [s])
  (shipment-history [s] "the append-only rubber-part-batch-shipment history (rubberworks.registry drafts)")
  (certificate-history [s] "the append-only Rubber Compound Test Certificate history (rubberworks.registry drafts)")
  (next-shipment-sequence [s jurisdiction] "next shipment-number sequence for a product-class scheme")
  (next-certificate-sequence [s jurisdiction] "next certificate-number sequence for a product-class scheme")
  (batch-already-shipped? [s batch-id] "has this rubber-part-batch already been shipped onward?")
  (batch-already-certified? [s batch-id] "has this rubber-part-batch's Rubber Compound Test Certificate already been issued?")
  (commit-record! [s record] "apply a committed op's record to the SSoT")
  (append-ledger! [s fact]   "append one immutable decision fact")
  (with-rubber-part-batches [s batches] "replace/seed the rubber-part-batch directory (map id->batch)"))

;; ----------------------------- demo data -----------------------------

(defn- with-compression-telemetry
  "Merges REAL compression-set-test-verification telemetry onto a demo
  rubber-part-batch's base fields -- `rubberworks.robotics/compression-
  telemetry-for` actually runs `simulate-press`'s `physics-2d`-stepped
  simulation for this batch's own `:compression-platen-mass-kg`
  (ADR-2607151600/ADR-2607152000), so even the 'already on file' seed
  data (as if from an earlier real compression-set-test report) is
  genuinely simulation-derived, never hand-typed doubles."
  [base]
  (merge base (select-keys (robotics/compression-telemetry-for base)
                           [:sim-peak-compression-force-n])))

(defn demo-data
  "A small, self-contained rubber-part-batch set covering both
  actuation lifecycles (shipping a batch onward to a downstream
  consumer, issuing a Rubber Compound Test Certificate) so the actor +
  tests run offline. `:compression-platen-mass-kg` (ADR-2607151600/
  ADR-2607152000) is a permanent batch press-run-configuration field
  (like `:durometer-deviation-actual-shore-a`); `:sim-peak-compression-
  force-n` is the REAL `rubberworks.robotics/simulate-press`-computed
  telemetry for that field (`with-compression-telemetry`), the ground
  truth `rubberworks.robotics/simulation-out-of-tolerance?` independently
  rechecks against the batch's own recorded `:compression-force-min-n`/
  `:compression-force-max-n` acceptance band.

  batch-1 -- electronics/consumer gasket-and-case batch (soft-gasket-
    grade, 0.5kg compression-platen mass -> 160.0 N, clears its own
    [50,300] N acceptance band with margin) -- the clean, fully-
    processable electronics batch.
  batch-2 -- electronics/consumer gasket-and-case batch (soft-gasket-
    grade, same in-band 160.0 N compression force), but recorded
    against a product class (\"MEDDEV-RUBBER\", a medical-device rubber
    componentry scheme) `rubberworks.facts` genuinely does NOT cover --
    the no-spec-basis negative control.
  batch-3 -- automotive rubber-component batch (structural-mount-grade,
    2.0kg -> 640.0 N, clears its own [300,1200] N band), but its own
    recorded durometer (Shore A hardness) deviation (6.0 points) falls
    outside its own [-3,3] point acceptance band -- a genuine post-cure
    hardness defect distinct from the compression-force physics check.
  batch-4 -- electronics/consumer gasket-and-case batch (soft-gasket-
    grade, real, covered \"ELECTRONICS-GASKET\" scheme), compression
    force and durometer deviation both clean, but an unresolved end-of-
    line defect (dimensional/porosity/flash visual reject) is on file.
  batch-5 -- automotive rubber-component batch (structural-mount-grade),
    DELIBERATELY recorded with a much HEAVIER `:compression-platen-
    mass-kg` (5.0kg) than its own [300,1200] N band can clear (real
    simulated compression reading 1600.0 N, OVER the 1200 N max -- an
    over-compressed press-run-configuration inconsistency risking a
    too-hard/wrong-durometer compound for its intended mount/bushing
    spec) that the real, re-run simulation catches on independent
    recheck even though `:robotics-sim-verified?` was seeded `true`
    (\"already on file\", i.e. someone/something marked it passed
    without this real check ever having run) -- the rubber-products-
    manufacturer analog of opticsworks' batch-5 over-pressed lens-
    seating misconfiguration.
  batch-6 -- electronics/consumer gasket-and-case batch (soft-gasket-
    grade), DELIBERATELY recorded with a much LIGHTER `:compression-
    platen-mass-kg` (0.1kg) than its own [50,300] N band requires (real
    simulated compression reading 32.0 N, UNDER the 50 N min -- an
    under-compressed press-run-configuration inconsistency risking a
    too-soft/under-cured compound that will not seal adequately) that
    the real, re-run simulation ALSO catches on independent recheck
    even though `:robotics-sim-verified?` was seeded `true` -- the
    opposite-direction failure this actor's own two-sided compression-
    force check must also catch (unlike `moldworks.robotics`'s
    deliberately one-sided clamp-tonnage check)."
  []
  {:rubber-part-batches
   (into {}
         (map (fn [v] [(:id v) (with-compression-telemetry v)]))
         [{:id "batch-1" :batch-name "Meridian Waterproofing Gasket Batch RG-4401"
           :product-class :soft-gasket-grade
           :compression-platen-mass-kg 0.5
           :compression-force-min-n 50.0 :compression-force-max-n 300.0
           :durometer-deviation-actual-shore-a 0.5
           :durometer-deviation-min-shore-a -3.0
           :durometer-deviation-max-shore-a 3.0
           :rubber-part-batch-defect-unresolved? false
           :robotics-sim-verified? false :robotics-sim-record nil
           :rubber-part-batch-shipped? false :material-certified? false
           :jurisdiction "ELECTRONICS-GASKET" :status :intake}
          {:id "batch-2" :batch-name "Atlas Protective-Case Overmold Batch RG-1180"
           :product-class :soft-gasket-grade
           :compression-platen-mass-kg 0.5
           :compression-force-min-n 50.0 :compression-force-max-n 300.0
           :durometer-deviation-actual-shore-a 0.5
           :durometer-deviation-min-shore-a -3.0
           :durometer-deviation-max-shore-a 3.0
           :rubber-part-batch-defect-unresolved? false
           :robotics-sim-verified? false :robotics-sim-record nil
           :rubber-part-batch-shipped? false :material-certified? false
           :jurisdiction "MEDDEV-RUBBER" :status :intake}
          {:id "batch-3" :batch-name "田中ウェザーストリップ材料バッチ RG-2215"
           :product-class :structural-mount-grade
           :compression-platen-mass-kg 2.0
           :compression-force-min-n 300.0 :compression-force-max-n 1200.0
           :durometer-deviation-actual-shore-a 6.0
           :durometer-deviation-min-shore-a -3.0
           :durometer-deviation-max-shore-a 3.0
           :rubber-part-batch-defect-unresolved? false
           :robotics-sim-verified? false :robotics-sim-record nil
           :rubber-part-batch-shipped? false :material-certified? false
           :jurisdiction "AUTOMOTIVE-RUBBER" :status :intake}
          {:id "batch-4" :batch-name "佐藤スマートフォン防水ガスケットバッチ RG-3330"
           :product-class :soft-gasket-grade
           :compression-platen-mass-kg 0.5
           :compression-force-min-n 50.0 :compression-force-max-n 300.0
           :durometer-deviation-actual-shore-a 0.5
           :durometer-deviation-min-shore-a -3.0
           :durometer-deviation-max-shore-a 3.0
           :rubber-part-batch-defect-unresolved? true
           :robotics-sim-verified? false :robotics-sim-record nil
           :rubber-part-batch-shipped? false :material-certified? false
           :jurisdiction "ELECTRONICS-GASKET" :status :intake}
          {:id "batch-5" :batch-name "鈴木エンジンマウントブッシュ材料バッチ RG-1118"
           :product-class :structural-mount-grade
           :compression-platen-mass-kg 5.0
           :compression-force-min-n 300.0 :compression-force-max-n 1200.0
           :durometer-deviation-actual-shore-a 0.5
           :durometer-deviation-min-shore-a -3.0
           :durometer-deviation-max-shore-a 3.0
           :rubber-part-batch-defect-unresolved? false
           :robotics-sim-verified? true :robotics-sim-record nil
           :rubber-part-batch-shipped? false :material-certified? false
           :jurisdiction "AUTOMOTIVE-RUBBER" :status :intake}
          {:id "batch-6" :batch-name "Nakamura Port-Seal Gasket Batch RG-5502"
           :product-class :soft-gasket-grade
           :compression-platen-mass-kg 0.1
           :compression-force-min-n 50.0 :compression-force-max-n 300.0
           :durometer-deviation-actual-shore-a 0.5
           :durometer-deviation-min-shore-a -3.0
           :durometer-deviation-max-shore-a 3.0
           :rubber-part-batch-defect-unresolved? false
           :robotics-sim-verified? true :robotics-sim-record nil
           :rubber-part-batch-shipped? false :material-certified? false
           :jurisdiction "ELECTRONICS-GASKET" :status :intake}])})

;; ----------------------------- shared commit logic -----------------------------

(defn- ship-rubber-part-batch!
  "Backend-agnostic `:rubber-part-batch/mark-shipped` -- looks up the
  batch via the protocol and drafts the rubber-part-batch-shipment
  record, and returns {:result .. :batch-patch ..} for the caller to
  persist."
  [s batch-id]
  (let [a (rubber-part-batch s batch-id)
        seq-n (next-shipment-sequence s (:jurisdiction a))
        result (registry/register-rubber-part-batch-shipment batch-id (:jurisdiction a) seq-n)]
    {:result result
     :batch-patch {:rubber-part-batch-shipped? true
                  :shipment-number (get result "shipment_number")}}))

(defn- issue-material-certificate!
  "Backend-agnostic `:rubber-part-batch/mark-certified` -- looks up the
  batch via the protocol and drafts the Rubber Compound Test
  Certificate record, and returns {:result .. :batch-patch ..} for the
  caller to persist."
  [s batch-id]
  (let [a (rubber-part-batch s batch-id)
        seq-n (next-certificate-sequence s (:jurisdiction a))
        result (registry/register-material-certificate batch-id (:jurisdiction a) seq-n)]
    {:result result
     :batch-patch {:material-certified? true
                  :certificate-number (get result "certificate_number")}}))

;; ----------------------------- MemStore (default) -----------------------------

(defrecord MemStore [a]
  Store
  (rubber-part-batch [_ id] (get-in @a [:rubber-part-batches id]))
  (all-rubber-part-batches [_] (sort-by :id (vals (:rubber-part-batches @a))))
  (eol-screen-of [_ id] (get-in @a [:eol-screens id]))
  (material-spec-verification-of [_ batch-id] (get-in @a [:verifications batch-id]))
  (ledger [_] (:ledger @a))
  (shipment-history [_] (:shipments @a))
  (certificate-history [_] (:certificates @a))
  (next-shipment-sequence [_ jurisdiction] (get-in @a [:shipment-sequences jurisdiction] 0))
  (next-certificate-sequence [_ jurisdiction] (get-in @a [:certificate-sequences jurisdiction] 0))
  (batch-already-shipped? [_ batch-id] (boolean (get-in @a [:rubber-part-batches batch-id :rubber-part-batch-shipped?])))
  (batch-already-certified? [_ batch-id] (boolean (get-in @a [:rubber-part-batches batch-id :material-certified?])))
  (commit-record! [s {:keys [effect path value payload]}]
    (case effect
      :rubber-part-batch/upsert
      (swap! a update-in [:rubber-part-batches (:id value)] merge value)

      :material-spec-verification/set
      (swap! a assoc-in [:verifications (first path)] payload)

      :eol-screen/set
      (swap! a assoc-in [:eol-screens (first path)] payload)

      :rubber-part-batch/mark-shipped
      (let [batch-id (first path)
            {:keys [result batch-patch]} (ship-rubber-part-batch! s batch-id)
            jurisdiction (:jurisdiction (rubber-part-batch s batch-id))]
        (swap! a (fn [state]
                   (-> state
                       (update-in [:shipment-sequences jurisdiction] (fnil inc 0))
                       (update-in [:rubber-part-batches batch-id] merge batch-patch)
                       (update :shipments registry/append result))))
        result)

      :rubber-part-batch/mark-certified
      (let [batch-id (first path)
            {:keys [result batch-patch]} (issue-material-certificate! s batch-id)
            jurisdiction (:jurisdiction (rubber-part-batch s batch-id))]
        (swap! a (fn [state]
                   (-> state
                       (update-in [:certificate-sequences jurisdiction] (fnil inc 0))
                       (update-in [:rubber-part-batches batch-id] merge batch-patch)
                       (update :certificates registry/append result))))
        result)
      nil)
    s)
  (append-ledger! [_ fact] (swap! a update :ledger conj fact) fact)
  (with-rubber-part-batches [s batches] (when (seq batches) (swap! a assoc :rubber-part-batches batches)) s))

(defn seed-db
  "A MemStore seeded with the demo rubber-part-batch set. The
  deterministic default."
  []
  (->MemStore (atom (assoc (demo-data)
                           :verifications {} :eol-screens {} :ledger []
                           :shipment-sequences {} :shipments []
                           :certificate-sequences {} :certificates []))))

;; ----------------------------- DatomicStore (langchain.db) -----------------------------

(def ^:private schema
  "DataScript/Datomic-style schema: only constraint attrs are declared.
  Map/compound values (verification/eol-screen payloads, ledger facts,
  shipment/certificate records) are stored as EDN strings so
  `langchain.db` doesn't expand them into sub-entities -- the same
  convention every sibling actor's store uses."
  {:rubber-part-batch/id              {:db/unique :db.unique/identity}
   :verification/batch-id             {:db/unique :db.unique/identity}
   :eol-screen/batch-id               {:db/unique :db.unique/identity}
   :ledger/seq                        {:db/unique :db.unique/identity}
   :shipment/seq                      {:db/unique :db.unique/identity}
   :certificate/seq                   {:db/unique :db.unique/identity}
   :shipment-sequence/jurisdiction    {:db/unique :db.unique/identity}
   :certificate-sequence/jurisdiction {:db/unique :db.unique/identity}})

(defn- enc [v] (pr-str v))
(defn- dec* [s] (when s (edn/read-string s)))

(defn- batch->tx [{:keys [id batch-name product-class
                          compression-platen-mass-kg sim-peak-compression-force-n
                          compression-force-min-n compression-force-max-n
                          durometer-deviation-actual-shore-a
                          durometer-deviation-min-shore-a
                          durometer-deviation-max-shore-a
                          rubber-part-batch-defect-unresolved? robotics-sim-verified? robotics-sim-record
                          rubber-part-batch-shipped? material-certified?
                          jurisdiction status shipment-number certificate-number]}]
  (cond-> {:rubber-part-batch/id id}
    batch-name                                        (assoc :rubber-part-batch/batch-name batch-name)
    product-class                                      (assoc :rubber-part-batch/product-class product-class)
    compression-platen-mass-kg                         (assoc :rubber-part-batch/compression-platen-mass-kg compression-platen-mass-kg)
    (some? sim-peak-compression-force-n)                (assoc :rubber-part-batch/sim-peak-compression-force-n sim-peak-compression-force-n)
    compression-force-min-n                             (assoc :rubber-part-batch/compression-force-min-n compression-force-min-n)
    compression-force-max-n                             (assoc :rubber-part-batch/compression-force-max-n compression-force-max-n)
    durometer-deviation-actual-shore-a                  (assoc :rubber-part-batch/durometer-deviation-actual-shore-a durometer-deviation-actual-shore-a)
    durometer-deviation-min-shore-a                     (assoc :rubber-part-batch/durometer-deviation-min-shore-a durometer-deviation-min-shore-a)
    durometer-deviation-max-shore-a                     (assoc :rubber-part-batch/durometer-deviation-max-shore-a durometer-deviation-max-shore-a)
    (some? rubber-part-batch-defect-unresolved?)        (assoc :rubber-part-batch/defect-unresolved? rubber-part-batch-defect-unresolved?)
    (some? robotics-sim-verified?)                      (assoc :rubber-part-batch/robotics-sim-verified? robotics-sim-verified?)
    (some? robotics-sim-record)                         (assoc :rubber-part-batch/robotics-sim-record (enc robotics-sim-record))
    (some? rubber-part-batch-shipped?)                  (assoc :rubber-part-batch/shipped? rubber-part-batch-shipped?)
    (some? material-certified?)                         (assoc :rubber-part-batch/certified? material-certified?)
    jurisdiction                                        (assoc :rubber-part-batch/jurisdiction jurisdiction)
    status                                              (assoc :rubber-part-batch/status status)
    shipment-number                                     (assoc :rubber-part-batch/shipment-number shipment-number)
    certificate-number                                  (assoc :rubber-part-batch/certificate-number certificate-number)))

(def ^:private batch-pull
  [:rubber-part-batch/id :rubber-part-batch/batch-name :rubber-part-batch/product-class
   :rubber-part-batch/compression-platen-mass-kg :rubber-part-batch/sim-peak-compression-force-n
   :rubber-part-batch/compression-force-min-n :rubber-part-batch/compression-force-max-n
   :rubber-part-batch/durometer-deviation-actual-shore-a
   :rubber-part-batch/durometer-deviation-min-shore-a
   :rubber-part-batch/durometer-deviation-max-shore-a
   :rubber-part-batch/defect-unresolved? :rubber-part-batch/robotics-sim-verified? :rubber-part-batch/robotics-sim-record
   :rubber-part-batch/shipped? :rubber-part-batch/certified?
   :rubber-part-batch/jurisdiction :rubber-part-batch/status
   :rubber-part-batch/shipment-number :rubber-part-batch/certificate-number])

(defn- pull->batch [m]
  (when (:rubber-part-batch/id m)
    {:id (:rubber-part-batch/id m) :batch-name (:rubber-part-batch/batch-name m)
     :product-class (:rubber-part-batch/product-class m)
     :compression-platen-mass-kg (:rubber-part-batch/compression-platen-mass-kg m)
     :sim-peak-compression-force-n (:rubber-part-batch/sim-peak-compression-force-n m)
     :compression-force-min-n (:rubber-part-batch/compression-force-min-n m)
     :compression-force-max-n (:rubber-part-batch/compression-force-max-n m)
     :durometer-deviation-actual-shore-a (:rubber-part-batch/durometer-deviation-actual-shore-a m)
     :durometer-deviation-min-shore-a (:rubber-part-batch/durometer-deviation-min-shore-a m)
     :durometer-deviation-max-shore-a (:rubber-part-batch/durometer-deviation-max-shore-a m)
     :rubber-part-batch-defect-unresolved? (boolean (:rubber-part-batch/defect-unresolved? m))
     :robotics-sim-verified? (boolean (:rubber-part-batch/robotics-sim-verified? m))
     :robotics-sim-record (dec* (:rubber-part-batch/robotics-sim-record m))
     :rubber-part-batch-shipped? (boolean (:rubber-part-batch/shipped? m))
     :material-certified? (boolean (:rubber-part-batch/certified? m))
     :jurisdiction (:rubber-part-batch/jurisdiction m) :status (:rubber-part-batch/status m)
     :shipment-number (:rubber-part-batch/shipment-number m) :certificate-number (:rubber-part-batch/certificate-number m)}))

(defrecord DatomicStore [conn]
  Store
  (rubber-part-batch [_ id]
    (pull->batch (d/pull (d/db conn) batch-pull [:rubber-part-batch/id id])))
  (all-rubber-part-batches [_]
    (->> (d/q '[:find [?id ...] :where [?e :rubber-part-batch/id ?id]] (d/db conn))
         (map #(pull->batch (d/pull (d/db conn) batch-pull [:rubber-part-batch/id %])))
         (sort-by :id)))
  (eol-screen-of [_ id]
    (dec* (d/q '[:find ?p . :in $ ?aid
                :where [?k :eol-screen/batch-id ?aid] [?k :eol-screen/payload ?p]]
              (d/db conn) id)))
  (material-spec-verification-of [_ batch-id]
    (dec* (d/q '[:find ?p . :in $ ?aid
                :where [?a :verification/batch-id ?aid] [?a :verification/payload ?p]]
              (d/db conn) batch-id)))
  (ledger [_]
    (->> (d/q '[:find ?s ?f :where [?e :ledger/seq ?s] [?e :ledger/fact ?f]] (d/db conn))
         (sort-by first)
         (mapv (comp dec* second))))
  (shipment-history [_]
    (->> (d/q '[:find ?s ?r :where [?e :shipment/seq ?s] [?e :shipment/record ?r]] (d/db conn))
         (sort-by first)
         (mapv (comp dec* second))))
  (certificate-history [_]
    (->> (d/q '[:find ?s ?r :where [?e :certificate/seq ?s] [?e :certificate/record ?r]] (d/db conn))
         (sort-by first)
         (mapv (comp dec* second))))
  (next-shipment-sequence [_ jurisdiction]
    (or (d/q '[:find ?n . :in $ ?j
              :where [?e :shipment-sequence/jurisdiction ?j] [?e :shipment-sequence/next ?n]]
            (d/db conn) jurisdiction)
        0))
  (next-certificate-sequence [_ jurisdiction]
    (or (d/q '[:find ?n . :in $ ?j
              :where [?e :certificate-sequence/jurisdiction ?j] [?e :certificate-sequence/next ?n]]
            (d/db conn) jurisdiction)
        0))
  (batch-already-shipped? [s batch-id]
    (boolean (:rubber-part-batch-shipped? (rubber-part-batch s batch-id))))
  (batch-already-certified? [s batch-id]
    (boolean (:material-certified? (rubber-part-batch s batch-id))))
  (commit-record! [s {:keys [effect path value payload]}]
    (case effect
      :rubber-part-batch/upsert
      (d/transact! conn [(batch->tx value)])

      :material-spec-verification/set
      (d/transact! conn [{:verification/batch-id (first path) :verification/payload (enc payload)}])

      :eol-screen/set
      (d/transact! conn [{:eol-screen/batch-id (first path) :eol-screen/payload (enc payload)}])

      :rubber-part-batch/mark-shipped
      (let [batch-id (first path)
            {:keys [result batch-patch]} (ship-rubber-part-batch! s batch-id)
            jurisdiction (:jurisdiction (rubber-part-batch s batch-id))
            next-n (inc (next-shipment-sequence s jurisdiction))]
        (d/transact! conn
                     [(batch->tx (assoc batch-patch :id batch-id))
                      {:shipment-sequence/jurisdiction jurisdiction :shipment-sequence/next next-n}
                      {:shipment/seq (count (shipment-history s)) :shipment/record (enc (get result "record"))}])
        result)

      :rubber-part-batch/mark-certified
      (let [batch-id (first path)
            {:keys [result batch-patch]} (issue-material-certificate! s batch-id)
            jurisdiction (:jurisdiction (rubber-part-batch s batch-id))
            next-n (inc (next-certificate-sequence s jurisdiction))]
        (d/transact! conn
                     [(batch->tx (assoc batch-patch :id batch-id))
                      {:certificate-sequence/jurisdiction jurisdiction :certificate-sequence/next next-n}
                      {:certificate/seq (count (certificate-history s)) :certificate/record (enc (get result "record"))}])
        result)
      nil)
    s)
  (append-ledger! [s fact]
    (d/transact! conn [{:ledger/seq (count (ledger s)) :ledger/fact (enc fact)}])
    fact)
  (with-rubber-part-batches [s batches]
    (when (seq batches) (d/transact! conn (mapv batch->tx (vals batches)))) s))

(defn datomic-store
  "A DatomicStore (langchain.db backend) seeded from `data`
  ({:rubber-part-batches ..}); empty when omitted."
  ([] (datomic-store {}))
  ([{:keys [rubber-part-batches]}]
   (let [s (->DatomicStore (d/create-conn schema))]
     (with-rubber-part-batches s rubber-part-batches))))

(defn datomic-seed-db
  "A DatomicStore seeded with the demo rubber-part-batch set -- the
  Datomic-backed analog of `seed-db`, used to prove protocol parity."
  []
  (datomic-store (demo-data)))
