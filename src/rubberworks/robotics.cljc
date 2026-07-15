(ns rubberworks.robotics
  "Robot-executed ASTM D395 compression-set / compression-force test
  verification -- the concrete, actor-level realization of
  ADR-2607011000's robotics premise (every cloud-itonami vertical is
  designed on the premise that a robot performs the physical-domain
  work; an independent governor gates any action before it ever reaches
  hardware), delivered NATIVELY onto ADR-2607151600/ADR-2607152000's
  real-engineering-simulation fleet pattern from day one (this vertical,
  isic-2219, is a NEW actor built to that same standard from day one,
  mirroring how `cloud-itonami-isic-2220`/`cloud-itonami-isic-2670`
  deliver it natively rather than retrofitted; closest technical analog:
  `cementmill.robotics`'s 28-day compressive-strength press test and
  `opticsworks.robotics`'s lens-barrel press-fit seating test -- a moving
  rigid body closing onto a static rigid body, force derived from F=m*a
  off the real simulated collision) for THIS actor's own manufacturing-
  process evidence requirement: a rubber-part-batch-shipment proposal
  must cite a real ASTM D395 compression-set test report actually on
  file -- not merely a self-reported checklist string.

  The compression-set-test step of the mission is an ACTUAL time-stepped
  `kotoba-lang/physics-2d` rigid-body simulation of a REAL, standard
  rubber QA test (ASTM D395, Standard Test Methods for Rubber Property --
  Compression Set, Method B): a moving compression-platen `Body2D` (the
  press tool that compresses the specimen at a controlled closing
  velocity) closes onto a static (mass 0, immovable) rubber-specimen
  `Body2D` (a Type 1 button specimen bolted into the compression-set
  test fixture) to the standard's own real 25% compression fraction.
  `world-step` actually integrates/collides/resolves the contact over
  real ticks, and `:sim-peak-compression-force-n` is read directly off
  the ACTUAL simulated velocity trajectory (F = m*a, the SAME technique
  every real-physics sibling in this fleet uses) -- not invented.

  A robot mission (`kotoba.robotics/mission`) walks the rubber-part-batch
  through three steps in the compression-set test cell -- a pre-
  compression durometer (Shore A hardness) check, the compression-set
  press-test cycle itself, and a post-compression thickness-recovery
  scan -- built with `kotoba.robotics/action` + `kotoba.robotics/
  telemetry-proof`, and reports an overall :passed? verdict derived from
  the REAL simulated compression reading (`:sim-peak-compression-force-
  n`, see `compression-telemetry-for`), not a hand-set field.
  `simulation-out-of-tolerance?` independently re-derives that verdict
  from the batch's OWN recorded real telemetry cross-checked against the
  batch's OWN recorded `:compression-force-min-n`/`:compression-force-
  max-n` acceptance band, never from the mission's self-reported result
  -- the SAME 'ground truth, not self-report' discipline `rubberworks.
  registry/rubber-part-batch-durometer-deviation-out-of-range?` uses for
  durometer (Shore A hardness) deviation. `rubberworks.governor`'s
  `robotics-simulation-violations` calls this ns's independent recheck,
  never the stored :passed? value, before any `:actuation/ship-rubber-
  part-batch` proposal may commit.

  Honest scope + citation disclosure (mirrors every real-physics
  sibling's own disclosure style, ADR-2607151600/ADR-2607152000):

  - 2D projection only (`physics-2d` has no 3D solver) -- x is the
    compression-platen's direction of travel (the compression axis);
    world gravity is [0 0] (a horizontal press-closing projection --
    real compression-set test fixtures commonly orient the compression
    axis vertically, but the axis choice itself carries no physical
    consequence for this 2D, gravity-free simulation).
  - the rubber specimen is modeled as a real ASTM D395 Method B Type 1
    button specimen -- a disc NOMINALLY 1.129 in (29.0 mm) diameter x
    0.500 in (12.5 mm) thick. HONEST CONFIDENCE DISCLOSURE: the 25%
    compression fraction itself (Method B's own defining parameter) is a
    HIGH-confidence, well-established figure; the exact disc diameter
    figure above is a plausible, commonly-cited nominal button-specimen
    size, disclosed with MODERATE confidence (this session could not
    verify the precise current-edition decimal figure without live web
    access) -- it is a geometry constant for this simulation's own
    AABB collider sizing, not a citation gating `:required-evidence`
    (see `rubberworks.facts`), so an imprecise nominal diameter does not
    risk fabricating a compliance citation.
  - the specimen is modeled as a STATIC (mass 0) AABB, mirroring
    `cementmill.robotics`'s static cube-specimen / `opticsworks.
    robotics`'s static lens-housing pattern: `physics-2d` treats a
    mass-0 body as having zero inverse mass (an immovable anchor), which
    is also physically apt here -- a real compression-set test fixture
    bolts the specimen platform to the test rig, not free to recoil.
    `physics-2d` has NO material-stiffness/stress-strain model
    whatsoever, so the specimen's own real durometer/hardness cannot
    itself vary the simulated reading (the SAME disclosed limitation
    every real-physics sibling states) -- what DOES vary the reading is
    this batch's own recorded press-run configuration
    (`:compression-platen-mass-kg`, see `compression-telemetry-for`).
  - `crush-travel-m` (the platen's own real closing travel before the
    specimen reaches 25% compression) is DERIVED from the standard's own
    real 25% compression fraction x the specimen's own nominal 12.5mm
    thickness -- a REAL, standard-derived distance, unlike several
    sibling namespaces' own disclosed-arbitrary crush-travel priors.
    `press-closing-velocity-mps` is this ns's own disclosed ANALOG
    closing rate (1.0 m/s) -- real ASTM D395 compression-set testing is
    performed at a slow, controlled, quasi-static rate (the specimen is
    held compressed for a fixed DURATION, commonly 22 hours at a fixed
    temperature, not driven at any particular closing speed at all) --
    `physics-2d`'s impulse solver has NO stress/strain model and no
    concept of a held duration; it only ever produces a meaningful
    collision impulse from an actual closing VELOCITY over a real
    timestep, so this ns picks a fast, disclosed analog rate (the SAME
    disclosed-analog-rate discipline `cementmill.robotics`/`opticsworks.
    robotics` use for their own press-closing velocities), never
    presented as a literal reproduction of any real fixture's actual
    (near-zero, quasi-static) closing speed.
  - `physics-2d`'s impulse resolver has no progressive crush stiffness/
    force-deflection model: whatever tick first detects ANY AABB overlap
    fully zeroes the closing velocity in that ONE tick (given
    `restitution` 0) -- a discrete, instantaneous 'boxcar' stop, the
    SAME disclosed limitation every real-physics sibling states. By
    exact kinematic identity (peak deceleration = closing-velocity / dt
    for a single-tick full stop against an immovable body, the SAME
    verified, documented property every real-physics sibling in this
    fleet establishes -- mass cancels algebraically in `physics-2d`'s
    `resolve-contact` when colliding with a mass-0 body), the peak
    deceleration itself is INDEPENDENT of `:compression-platen-mass-kg`
    -- so `:compression-platen-mass-kg` is the ONLY quantity that scales
    `:sim-peak-compression-force-n` for a fixed closing velocity/crush
    travel (via F = m*a), never the closing velocity or crush travel
    (both fixed constants, shared by every batch).
  - `compression-force-band-n` -- this ns's own disclosed, TWO-SIDED
    REASONED ENGINEERING ESTIMATE for real rubber compression-set/
    compression-force test forces, keyed by durometer (Shore A hardness)
    class. HONEST CONFIDENCE DISCLOSURE: this band is a REASONED
    ENGINEERING ESTIMATE informed by the well-known qualitative
    relationship between a rubber compound's Shore A durometer and its
    compressive stiffness at a fixed strain (softer compounds require
    markedly less force to reach the same 25% compression than harder
    compounds), NOT a verbatim transcription of a single formal
    standard's numeric table (ASTM D395 itself specifies the TEST
    METHOD -- 25% compression, held duration, recovery measurement -- it
    does NOT publish a required compression-FORCE range; that is a
    function of the specific compound/durometer/geometry a given
    manufacturer selects), the SAME moderate-confidence disclosure
    discipline `opticsworks.robotics`'s seating-force bands and
    `cementmill.robotics`'s strength band use for their own reasoned-
    estimate/reused-real-anchor figures. `:soft-gasket-grade` (nominal
    Shore A 40-50, common for gaskets/seals/waterproofing overmolds)
    covers electronics/consumer gasket compounds; `:structural-mount-
    grade` (nominal Shore A 70-90, common for structural mounts/
    bushings) covers automotive rubber components -- the harder compound
    plausibly requiring markedly more compression force at the same 25%
    strain and specimen geometry.
  - `rubber-part-batch-compression-force-out-of-tolerance?` is TWO-SIDED
    (unlike `moldworks.robotics`'s deliberately ONE-SIDED clamp-tonnage
    check): too little compression force is a real defect-risk direction
    (the compound is too soft/under-cured for its intended spec, risking
    a seal that will not maintain adequate contact pressure, or a mount/
    bushing that will not adequately isolate vibration); too much
    compression force is ALSO a real, distinct defect-risk direction
    (the compound is too hard/over-cured or the wrong durometer entirely
    for its intended product class, risking a gasket too stiff to
    conform to a mating surface, or a mount that transmits vibration/
    noise it was specified to isolate) -- a genuine two-sided failure
    mode for a compression-set/compression-force test, the SAME
    discipline `opticsworks.robotics`'s two-sided seating-force check
    establishes.

  Pure data + pure functions -- no real robot I/O, no network.
  `physics-2d/world-step` is itself a pure, fixed-timestep integrator
  (no wall-clock/IO), so this stays exactly as offline/deterministic as
  every other sibling namespace in this actor -- tests and the demo run
  without a network.

  Honest scope: this DOES model a real time-stepped `physics-2d` rigid-
  body trajectory for the compression-platen/rubber-specimen collision
  event, along the platen's own real travel axis, and derives a real
  compression-force reading directly comparable to a real, disclosed
  reasoned-engineering compression-force band. It does NOT model: the
  rubber compound's own stress-relaxation/creep behavior over the
  standard's real held-duration (`physics-2d` has no material-
  stiffness/viscoelastic model at all), 3D specimen/fixture geometry
  (2D projection, flat-plate approximation only), a real load-cell/
  compression-tester/DAQ connection, or a real test-cell servo-motion-
  planning/control system -- still simulation, not control, the same
  'policy, not control' boundary `kotoba.robotics`'s docstring already
  establishes."
  (:require [kotoba.robotics :as robotics]
            [physics-2d :as p2d]))

;; ------------------------- real ASTM D395 compression constants -------------------------

(def ^:const specimen-thickness-mm
  "ASTM D395 Method B's real, standard Type 1 button-specimen nominal
  thickness (0.500 in / 12.5 mm) -- HIGH confidence, this is the
  standard's own defining specimen dimension."
  12.5)

(def ^:const specimen-diameter-mm
  "ASTM D395 Method B's Type 1 button-specimen nominal diameter --
  disclosed with MODERATE confidence (a plausible, commonly-cited
  1.129 in / 29.0 mm figure; this session could not verify the precise
  current-edition decimal figure without live web access -- see ns
  docstring for the full honesty disclosure). A geometry constant for
  this simulation's own AABB collider sizing, not a citation gating
  `rubberworks.facts`'s hard evidence-checklist gate."
  29.0)

(def ^:const compression-fraction
  "ASTM D395 Method B's real, standard 25% compression fraction -- HIGH
  confidence, this is the standard's own defining test parameter."
  0.25)

(def ^:const crush-travel-m
  "The platen's own real closing travel (m) to reach the standard's own
  25% compression fraction of the specimen's own nominal thickness -- a
  REAL, standard-derived distance (`compression-fraction` x
  `specimen-thickness-mm`), unlike several sibling namespaces' own
  disclosed-arbitrary crush-travel priors."
  (* compression-fraction (/ specimen-thickness-mm 1000.0)))

(def ^:const press-closing-velocity-mps
  "The compression-platen's controlled closing velocity (m/s) for THIS
  simulation -- a disclosed ANALOG rate, NOT a literal transcription of
  any real compression-set test fixture's actual (quasi-static, held-
  duration, not speed-driven) closing behavior. See ns docstring for
  why."
  1.0)

(def ^:const dt
  "Per-tick timestep (s) -- derived from THIS simulation's own
  crush-travel/closing-velocity (the nominal transit time across the
  specimen's own 25%-compression crush zone), the SAME principled-not-
  arbitrary identity every real-physics sibling uses for its own `dt`."
  (/ crush-travel-m press-closing-velocity-mps))

(def ^:const platen-half-w-m
  "Compression-platen AABB half-width (m) along the travel axis -- a
  disclosed, arbitrary rigid-body stand-in (8mm full thickness for the
  platen face); `physics-2d` colliders do not deform, so this dimension
  is not a load-bearing physical parameter."
  0.004)

(def ^:const platen-half-h-m
  "Compression-platen AABB half-height (m), lateral -- 40mm full width,
  wider than the specimen's own 29mm diameter so the WHOLE specimen face
  loads, matching how a real compression-set test fixture's platen is
  sized >= the specimen face."
  0.02)

(def ^:const specimen-half-w-m
  "Rubber-specimen AABB half-width (m) along the travel axis -- the
  specimen's own nominal half-thickness."
  (/ (/ specimen-thickness-mm 1000.0) 2.0))

(def ^:const specimen-half-h-m
  "Rubber-specimen AABB half-height (m), lateral -- the specimen's own
  nominal half-diameter."
  (/ (/ specimen-diameter-mm 1000.0) 2.0))

(def ^:const gap-m
  "Compression-platen standoff distance (m) the platen starts behind the
  specimen, so the trajectory captures a real pre-contact approach
  phase, not just the collision tick itself (mirrors every real-physics
  sibling's own gap constant)."
  0.0005)

(def ^:const settle-ticks
  "Extra ticks appended after the platen is expected to reach the
  specimen, so the trajectory also captures post-contact settling -- the
  SAME constant + rationale as every real-physics sibling: `physics-2d`'s
  positional correction removes 80% of any remaining overlap per tick,
  so residual overlap after 15 more ticks is ~3e-11 of whatever it was
  at first contact."
  15)

(def compression-force-band-n
  "Real, disclosed reasoned-engineering-estimate rubber compression-set/
  compression-force bands (Newtons) per durometer (Shore A hardness)
  class -- see ns docstring for the full honest confidence disclosure (a
  moderate-confidence engineering estimate informed by the qualitative
  durometer/compressive-stiffness relationship, not a single formal
  standard's numeric table -- ASTM D395 specifies the TEST METHOD, not a
  required force range). `:soft-gasket-grade` (nominal Shore A 40-50)
  covers electronics/consumer gasket/seal compounds; `:structural-mount-
  grade` (nominal Shore A 70-90) covers automotive structural mount/
  bushing compounds."
  {:soft-gasket-grade     {:min 50.0  :max 300.0  :confidence :reasoned-engineering-estimate}
   :structural-mount-grade {:min 300.0 :max 1200.0 :confidence :reasoned-engineering-estimate}})

(defn compression-force-band-for
  "This ns's own disclosed compression-force band ({:min :max}, Newtons)
  for `product-class` -- used ONLY to seed demo/seed data (see `store/
  demo-data`); the actual governor check (`rubber-part-batch-
  compression-force-out-of-tolerance?` below) reads the batch's OWN
  stored `:compression-force-min-n`/`:compression-force-max-n` fields
  directly (ground truth already on file), never re-derives this formula
  per-batch. See ns docstring for the full honesty disclosure."
  [product-class]
  (select-keys (get compression-force-band-n product-class) [:min :max]))

;; ------------------------------ real simulation ------------------------------

(defn simulate-press
  "Time-steps a REAL `physics-2d` world for ONE ASTM D395 Method B
  compression-set-test cycle: a compression-platen `Body2D` (mass
  `platen-mass-kg`, velocity `press-closing-velocity-mps`) approaches and
  collides with a static (mass 0, immovable) rubber-specimen `Body2D`.
  Returns {:trajectory [{:tick :position :velocity} ...] (platen body
  only) :sim-peak-compression-force-n n :sim-peak-crush-distance-m n
  :ticks n :dt n :closing-velocity-mps n}.

  `:sim-peak-compression-force-n` is `platen-mass-kg` times the PEAK
  magnitude of tick-to-tick velocity change (along the travel axis)
  divided by `dt` -- F = m*a, derived from the ACTUAL simulated velocity
  trajectory (the SAME technique every real-physics sibling in this
  fleet uses). `:sim-peak-crush-distance-m` is the largest AABB
  penetration depth (m) actually observed between the platen's leading
  face and the specimen's near face across the whole trajectory --
  informational (this ns's tolerance check uses the force reading, not
  displacement), derived from the actual simulated positions, not
  invented.

  Pure, deterministic -- the same `platen-mass-kg` always reproduces the
  same telemetry; no IO, no wall-clock."
  [platen-mass-kg]
  (let [v0 press-closing-velocity-mps
        approach-m (+ gap-m platen-half-w-m specimen-half-w-m)
        ticks (long (+ settle-ticks (long (Math/ceil (/ approach-m (* v0 dt))))))
        specimen-x 0.0
        platen-x (- specimen-x specimen-half-w-m platen-half-w-m gap-m)
        platen (p2d/make-body {:position [platen-x 0.0]
                                :velocity [v0 0.0]
                                :mass (double platen-mass-kg)
                                :restitution 0.0
                                :friction 0.0
                                :collider (p2d/make-aabb-collider platen-half-w-m platen-half-h-m)
                                :user-data :compression-platen})
        specimen (p2d/make-body {:position [specimen-x 0.0]
                                  :velocity [0.0 0.0]
                                  :mass 0.0
                                  :restitution 0.0
                                  :friction 0.0
                                  :collider (p2d/make-aabb-collider specimen-half-w-m specimen-half-h-m)
                                  :user-data :rubber-specimen})
        w0 (p2d/world-new [0.0 0.0])
        [w1 pid] (p2d/world-add w0 platen)
        [w2 _sid] (p2d/world-add w1 specimen)
        worlds (reductions (fn [w _] (p2d/world-step w dt)) w2 (range ticks))
        trajectory (mapv (fn [tick world]
                            (let [b (nth (:bodies world) pid)]
                              {:tick tick :position (:position b) :velocity (:velocity b)}))
                          (range (count worlds)) worlds)
        vxs (mapv (comp first :velocity) trajectory)
        peak-decel-mps2 (->> (map (fn [va vb] (Math/abs (/ (- vb va) dt))) vxs (rest vxs))
                              (reduce max 0.0))
        contact-plane-x (- specimen-x specimen-half-w-m)
        penetrations-m (mapv (fn [{:keys [position]}]
                                (max 0.0 (- (+ (first position) platen-half-w-m) contact-plane-x)))
                              trajectory)
        peak-force-n (* (double platen-mass-kg) peak-decel-mps2)]
    {:trajectory trajectory
     :sim-peak-compression-force-n peak-force-n
     :sim-peak-crush-distance-m (reduce max 0.0 penetrations-m)
     :ticks (count trajectory)
     :dt dt
     :closing-velocity-mps v0}))

(defn compression-telemetry-for
  "Runs the REAL `simulate-press` time-stepped `physics-2d` simulation
  for `batch`'s own recorded `:compression-platen-mass-kg` press-run
  configuration and returns the actual simulated telemetry:
  {:sim-peak-compression-force-n n :sim-peak-crush-distance-m n :ticks n
  :dt n :closing-velocity-mps n}. Pure, deterministic -- the same
  `:compression-platen-mass-kg` always reproduces the same telemetry."
  [batch]
  (select-keys (simulate-press (:compression-platen-mass-kg batch))
               [:sim-peak-compression-force-n :sim-peak-crush-distance-m
                :ticks :dt :closing-velocity-mps]))

(def mission-actions
  "The three-step compression-set-test-cell mission every rubber-part-
  batch walks through before `:actuation/ship-rubber-part-batch` is
  proposable. :sense at :none safety, :actuate at :low safety --
  verification/QA handling of a stationary lab specimen, not the moving-
  shipment actuation that is `:actuation/ship-rubber-part-batch` itself
  (always :safety-critical -- see `rubberworks.governor`)."
  [{:step :pre-compression-durometer-check         :kind :sense   :safety :none}
   {:step :compression-set-press-test              :kind :actuate :safety :low}
   {:step :post-compression-thickness-recovery-scan :kind :sense   :safety :none}])

(defn rubber-part-batch-compression-force-out-of-tolerance?
  "Ground-truth check: does `batch`'s own recorded REAL `physics-2d`-
  simulated compression reading (`:sim-peak-compression-force-n`, see
  `compression-telemetry-for`) fall OUTSIDE `batch`'s own recorded
  [:compression-force-min-n :compression-force-max-n] acceptance band?
  TWO-SIDED -- see ns docstring for why both under- and over-compressed
  are real, distinct defect-risk directions for a compression-set/
  compression-force test (unlike `moldworks.robotics`'s deliberately
  one-sided clamp-tonnage check). Needs no mission run or proposal
  inspection once the telemetry and acceptance-band fields are on file
  -- its inputs are permanent fields already on the batch, the same
  shape `rubberworks.registry/rubber-part-batch-durometer-deviation-out-
  of-range?` uses for durometer deviation."
  [{:keys [sim-peak-compression-force-n compression-force-min-n compression-force-max-n]}]
  (and (number? sim-peak-compression-force-n) (number? compression-force-min-n) (number? compression-force-max-n)
       (or (< sim-peak-compression-force-n compression-force-min-n)
           (> sim-peak-compression-force-n compression-force-max-n))))

(defn simulate-compression-set-test
  "Run the robot-executed compression-set-test mission for `batch-id`
  (`batch` is the full record, incl. `:compression-platen-mass-kg`,
  `:compression-force-min-n`, `:compression-force-max-n`). Actually runs
  the REAL engine: `compression-telemetry-for` -- the actual
  `physics-2d`-stepped compression-platen/rubber-specimen collision
  trajectory (`:sim-peak-compression-force-n`).

  Returns {:mission .. :actions [{:action .. :proof ..} ..] :passed?
  bool :sim-peak-compression-force-n n}. Deterministic: :passed? is
  derived from the batch's OWN recorded compression-run configuration
  via the REAL simulated trajectory (`rubber-part-batch-compression-
  force-out-of-tolerance?`), never invented or randomized --
  `kotoba.robotics` mandates no network/IO, and a repeatable simulation
  is what makes the governor's independent recheck
  (`simulation-out-of-tolerance?`) meaningful."
  [batch-id batch]
  (let [telemetry (compression-telemetry-for batch)
        merged (merge batch telemetry)
        out-of-range? (rubber-part-batch-compression-force-out-of-tolerance? merged)
        reading (if out-of-range? :out-of-tolerance :nominal)
        mission (robotics/mission (str "mission-" batch-id "-compression-set-verify")
                                   :robot/compression-set-test-cell-1
                                   :compression-set-verification
                                   :boundaries {:station "rubberworks-compression-set-test-cell"}
                                   :max-steps (count mission-actions))
        actions (mapv (fn [{:keys [step kind safety]}]
                        (let [a (robotics/action (str (:mission/id mission) "-" (name step))
                                                  (:mission/id mission) kind safety
                                                  :params {:step step :batch-id batch-id})]
                          {:action a
                           :proof (robotics/telemetry-proof (:mission/id mission) step reading
                                                             :provenance :simulated)}))
                      mission-actions)]
    {:mission mission
     :actions actions
     :passed? (not out-of-range?)
     :sim-peak-compression-force-n (:sim-peak-compression-force-n telemetry)}))

(defn simulation-out-of-tolerance?
  "Independent ground-truth recheck for the governor: does `batch`'s
  OWN current, on-file real `physics-2d`-simulated compression telemetry
  (`:sim-peak-compression-force-n`) fall outside its own recorded
  acceptance band right now? Ignores whatever :passed? verdict a prior
  mission run stored -- identical in spirit to `rubberworks.registry/
  rubber-part-batch-durometer-deviation-out-of-range?`'s refusal to
  trust a proposal's self-report. Does NOT re-run the simulation -- it
  re-derives the boolean from the real, already-persisted telemetry
  field (`rubberworks.store` persists it on every `:rubber-part-batch/
  upsert`), the same 'ground truth, not self-report' discipline applied
  to the STORED reading, not a fresh recompute."
  [batch]
  (rubber-part-batch-compression-force-out-of-tolerance? batch))
