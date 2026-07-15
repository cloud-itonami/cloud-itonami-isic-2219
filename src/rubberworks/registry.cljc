(ns rubberworks.registry
  "Pure-function rubber-part-batch-shipment + Rubber Compound Test
  Certificate record construction -- an append-only rubber-products-
  manufacturer book-of-record draft.

  Like every sibling actor's registry, there is no single international
  check-digit standard for a rubber-part-batch-shipment or Rubber
  Compound Test Certificate reference number -- every manufacturer/
  scheme assigns its own reference format. This namespace does NOT
  invent one; it builds a jurisdiction/scheme-scoped sequence number and
  validates the record's required fields, the same honest, non-
  fabricating discipline `rubberworks.facts` uses.

  `rubber-part-batch-durometer-deviation-out-of-range?` continues this
  fleet's two-sided range check family (`testlab.registry/within-
  tolerance?` established the first; `conservation.registry`/`water.
  registry`/`steelworks.registry`/`turbine.registry`/`automotive.
  registry`/`autoparts.registry`/`bodyshop.registry`/`cellworks.
  registry`/`glassworks.registry`/`moldworks.registry`/`opticsworks.
  registry` are further siblings), applying the SAME lo/hi bounds-
  comparison shape to a rubber-part-batch's own measured durometer
  (Shore A hardness) deviation from its own compound spec's nominal
  value -- a real, standard rubber QA metric (Shore A durometer testing),
  distinct from `rubberworks.robotics`'s own compression-force ground-
  truth check (a physics-derived compression-run-process reading, not a
  post-cure hardness measurement).

  This namespace is pure data + pure functions -- no I/O, no network
  call to any real plant/MES control system. It builds the RECORD a
  rubber-products manufacturer would keep, not the act of shipping the
  rubber-part-batch robot action or issuing the Rubber Compound Test
  Certificate itself (that is `rubberworks.operation`'s `:actuation/
  ship-rubber-part-batch`/`:actuation/issue-material-certificate`,
  always human-gated -- see README `Actuation`)."
  (:require [clojure.string :as str]))

(defn- unsigned-certificate
  "Every certificate this actor produces is UNSIGNED -- signature is the
  rubber-products manufacturer's own act, not this actor's. See README
  `Actuation`."
  [kind subject record-id]
  {"@context" ["https://www.w3.org/ns/credentials/v2"]
   "type" ["VerifiableCredential" kind]
   "credentialSubject" {"id" subject "record" record-id}
   "proof" nil
   "issued_by_registry" false
   "status" "draft-unsigned"})

(defn- zero-pad [n w]
  (let [s (str n)]
    (str (apply str (repeat (max 0 (- w (count s))) "0")) s)))

(defn rubber-part-batch-durometer-deviation-out-of-range?
  "Does `batch`'s own `:durometer-deviation-actual-shore-a` fall outside
  its own `[:durometer-deviation-min-shore-a :durometer-deviation-max-
  shore-a]` recorded acceptance-band bounds (a Shore A hardness-points
  deviation from the rubber compound's own nominal spec durometer)? A
  pure ground-truth check against the batch's own permanent fields -- no
  upstream comparison needed, and no physics re-simulation needed
  (distinct from `rubberworks.robotics`'s own compression-force ground-
  truth check). A further sibling in this fleet's two-sided range check
  family (see ns docstring)."
  [{:keys [durometer-deviation-actual-shore-a
           durometer-deviation-min-shore-a
           durometer-deviation-max-shore-a]}]
  (and (number? durometer-deviation-actual-shore-a)
       (number? durometer-deviation-min-shore-a)
       (number? durometer-deviation-max-shore-a)
       (or (< durometer-deviation-actual-shore-a durometer-deviation-min-shore-a)
           (> durometer-deviation-actual-shore-a durometer-deviation-max-shore-a))))

(defn register-rubber-part-batch-shipment
  "Validate + construct the RUBBER-PART-BATCH-SHIPMENT registration
  DRAFT -- the rubber-products manufacturer's own act of dispatching a
  real robot handling/shipment action releasing a rubber-part-batch
  onward to a downstream consumer (the real dual hand-off to BOTH
  `cloud-itonami-isic-2630`'s smartphone protective-case/gasket
  integration and `cloud-itonami-isic-2910`/`cloud-itonami-isic-2920`'s
  automotive weatherstripping/hose/mount integration -- see README
  `Upstream -> downstream hand-off`). Pure function -- does not touch
  any real plant/MES control system; it builds the RECORD a rubber-
  products manufacturer would keep. `rubberworks.governor` independently
  re-verifies the batch's own durometer-deviation sufficiency against
  its own acceptance-band bounds, and a double-shipment for the same
  batch, before this is ever allowed to commit."
  [batch-id jurisdiction sequence]
  (when-not (and batch-id (not= batch-id ""))
    (throw (ex-info "rubber-part-batch-shipment: batch_id required" {})))
  (when-not (and jurisdiction (not= jurisdiction ""))
    (throw (ex-info "rubber-part-batch-shipment: jurisdiction required" {})))
  (when (< sequence 0)
    (throw (ex-info "rubber-part-batch-shipment: sequence must be >= 0" {})))
  (let [shipment-number (str (str/upper-case jurisdiction) "-RPS-" (zero-pad sequence 6))
        record {"record_id" shipment-number
                "kind" "rubber-part-batch-shipment-draft"
                "batch_id" batch-id
                "jurisdiction" jurisdiction
                "immutable" true}]
    {"record" record "shipment_number" shipment-number
     "certificate" (unsigned-certificate "RubberPartBatchShipment" shipment-number shipment-number)}))

(defn register-material-certificate
  "Validate + construct the RUBBER COMPOUND TEST CERTIFICATE
  registration DRAFT -- the rubber-products manufacturer's own act of
  issuing a real Rubber Compound Test Certificate (ASTM D2000/D395
  conformance report + safety compliance) certifying a rubber-part-
  batch's material-spec conformance before onward shipment to either
  downstream consumer. Pure function -- does not touch any real plant/
  MES control system; it builds the RECORD a rubber-products
  manufacturer would keep. `rubberworks.governor` independently re-
  verifies the batch's own end-of-line-defect resolution status, and a
  double-issuance for the same batch, before this is ever allowed to
  commit."
  [batch-id jurisdiction sequence]
  (when-not (and batch-id (not= batch-id ""))
    (throw (ex-info "material-certificate: batch_id required" {})))
  (when-not (and jurisdiction (not= jurisdiction ""))
    (throw (ex-info "material-certificate: jurisdiction required" {})))
  (when (< sequence 0)
    (throw (ex-info "material-certificate: sequence must be >= 0" {})))
  (let [certificate-number (str (str/upper-case jurisdiction) "-RMC-" (zero-pad sequence 6))
        record {"record_id" certificate-number
                "kind" "material-certificate-draft"
                "batch_id" batch-id
                "jurisdiction" jurisdiction
                "immutable" true}]
    {"record" record "certificate_number" certificate-number
     "certificate" (unsigned-certificate "RubberCompoundTestCertificate" certificate-number certificate-number)}))

(defn append [history result]
  (conj (vec history) (get result "record")))
