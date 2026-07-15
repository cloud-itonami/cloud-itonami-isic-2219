# ADR-0001: Rubber Advisor ⊣ Compression-Seal Governor architecture

- Status: Accepted (2026-07-15)
- Repository: `cloud-itonami-isic-2219` (ISIC Rev.5 `2219`)

## Context

Other-rubber-products manufacturing (ASTM D395 compression-set-test
verification, per-product-class material-spec evidence verification,
end-of-line dimensional/porosity/flash inspection, Rubber Compound Test
Certificate issuance) needs the same governed-actor pattern as the rest
of the cloud-itonami fleet: an untrusted advisor proposes; an
independent governor may HOLD; high-stakes actuation never
auto-commits.

The industry-registry entry for `2219` had sat with no repo, no
business model, no actor (the industry registry's own dead placeholder
repo, `gftdcojp/cloud-itonami-C2219`, had no physics simulation and no
real rubber material-spec evidence catalog). A value-chain review found
`cloud-itonami-isic-2630` (communication-equipment/smartphone assembly)
and `cloud-itonami-isic-2910`/`cloud-itonami-isic-2920` (motor-vehicle/
body assembly) both implemented, each consuming finished rubber
sealing/mounting/gasket components as an input, but the rubber-products
manufacturing stage feeding BOTH chains had no real actor -- the same
"missing shared upstream stage" gap `cloud-itonami-isic-2220`
(injection-molded plastics) and `cloud-itonami-isic-2670` (optical/
camera modules) closed for both chains' other shared component needs.
This vertical is deliberately DISTINCT from `cloud-itonami-isic-2211`
(rubber tyres, already implemented separately, its own material-spec
catalog and its own governor) -- this actor covers "other rubber
products" (seals/gaskets/hoses/mounts/cases), never tyres.

This vertical adopts ADR-2607151600/ADR-2607152000's real-engineering-
simulation fleet pattern NATIVELY from day one -- mirroring how
`cloud-itonami-isic-2220`/`cloud-itonami-isic-2720`/`cloud-itonami-
isic-2310`/`cloud-itonami-isic-2670` were each built real-physics-first.

## Decision

1. Namespaces live under `rubberworks.*` with the standard facts /
   registry / store / governor / phase / advisor / operation / sim /
   robotics / export shape.
2. Entity is a **rubber-part-batch** (a manufactured lot of molded/
   extruded rubber parts of one compound/spec -- either an electronics/
   consumer gasket-and-case batch or an automotive rubber-component
   batch), not a finished device or a finished vehicle.
3. Dual actuation on the same entity:
   - `:actuation/ship-rubber-part-batch` (robot rubber-part-batch-
     shipment dispatch draft, onward to a downstream consumer -- the
     real dual hand-off to BOTH `cloud-itonami-isic-2630`'s smartphone
     protective-case/gasket integration and `cloud-itonami-isic-2910`/
     `cloud-itonami-isic-2920`'s automotive weatherstripping/hose/mount
     integration)
   - `:actuation/issue-material-certificate` (Rubber Compound Test
     Certificate draft: ASTM D2000/D395 conformance report + safety
     compliance)
4. Double-actuation guards use dedicated booleans (`:rubber-part-
   batch-shipped?`, `:material-certified?`), never a status lifecycle
   (ADR-2607071320 / 6492 lesson).
5. `rubber-part-batch-durometer-deviation-out-of-range?` continues the
   fleet two-sided range check family, applied here to a batch's own
   measured durometer (Shore A hardness) deviation from its own
   compound spec's nominal value -- a real end-of-line/post-cure QA
   metric, distinct from the physics-derived compression-force check.
6. `rubberworks.robotics` delivers a REAL, time-stepped `physics-2d`
   rigid-body ASTM D395 compression-set-test simulation from day one
   (not a symbolic field comparison, and not a retrofit): a moving
   compression-platen `Body2D` closes at a controlled velocity onto a
   static rubber-specimen `Body2D` to the standard's own real 25%
   compression fraction; `:sim-peak-compression-force-n` is read
   directly off the actual simulated collision trajectory. The governor
   HARD-holds if the mission never ran, OR if an independent recompute
   of the batch's own `:sim-peak-compression-force-n` falls outside the
   batch's own recorded `:compression-force-min-n`/`:compression-force-
   max-n` acceptance band (a REASONED ENGINEERING ESTIMATE for real
   rubber compression-force behavior by durometer class, disclosed
   HONESTLY as a moderate-confidence estimate, not a single formal
   standard's numeric threshold -- ASTM D395 itself specifies the TEST
   METHOD, not a required force range) -- never trusting the mission's
   self-reported verdict. TWO-SIDED, like `opticsworks.robotics`'s
   seating-force check: too little compression force risks an under-
   cured/too-soft compound that will not seal adequately; too much
   risks an over-cured/wrong-durometer compound too stiff for its
   intended product class.
7. Material-spec scheme catalog (`rubberworks.facts`) seeds
   AUTOMOTIVE-RUBBER (ASTM D2000 + ISO 1817) and ELECTRONICS-GASKET
   (IEC 60529 + ASTM D395) only; missing product classes (e.g. medical-
   device rubber componentry) are uncovered, never fabricated.
8. End-of-line defect (dimensional/porosity/flash) unresolved is
   evaluated unconditionally so `:end-of-line-quality/screen` itself can
   HARD-hold (parksafety ADR-2607071922 Decision 5 discipline, same as
   `moldworks.governor`'s/`automotive.governor`'s/`opticsworks.
   governor`'s end-of-line-defect-unresolved checks).

## Consequences

(+) The rubber-products manufacturing stage gains a forkable OSS
operating stack with auditable governor holds, closing a gap common to
BOTH the smartphone-assembly and vehicle-assembly value chains -- the
SAME dual-downstream-hand-off shape `cloud-itonami-isic-2220`/
`cloud-itonami-isic-2720`/`cloud-itonami-isic-2310`/`cloud-itonami-
isic-2670` established for plastics, batteries, glass and optical
modules.
(+) Delivers a REAL time-stepped physics simulation (not a symbolic
comparison) as a native part of this actor's initial build, extending
ADR-2607151600/ADR-2607152000's fleet pattern to a NEW actor rather than
retrofitting an existing symbolic one -- and anchors its tolerance band
on a REAL, disclosed reasoned-engineering-estimate by durometer class
(50-300 N for soft, gasket-grade compounds; 300-1200 N for harder,
structural-mount-grade compounds), honestly disclosed as a moderate-
confidence estimate rather than a single formal-standard number (ASTM
D395 specifies the test method, not a force range).
(+) Genuine dual-downstream hand-off value: the same rubber-part-batch-
shipment/certificate shape serves both `cloud-itonami-isic-2630` and
`cloud-itonami-isic-2910`/`cloud-itonami-isic-2920` without this actor
needing to know which downstream consumer a given shipment goes to.
(+) Explicitly scoped away from `cloud-itonami-isic-2211` (rubber
tyres, a distinct, already-implemented vertical) -- no tyre-specific
citation appears anywhere in `rubberworks.facts`/`rubberworks.robotics`.
(-) No physical plant digital-twin tick beyond the single compression-
set-test physics check in this repo (a stress-relaxation/creep-over-
held-duration simulation is out of scope here -- `physics-2d` has no
viscoelastic material model at all).
(-) Material-spec-scheme coverage is a starting catalog (2 product
classes), not exhaustive, and does not capture every product class a
rubber-products manufacturer might produce (e.g. industrial/marine
rubber goods).
(-) `physics-2d` is a 2D projection with no material-stiffness/
deformation model, and the compression-platen/rubber-specimen pair is
approximated as flat-plate AABBs (a disclosed simplification
necessitated by `physics-2d`'s narrowphase) -- see `rubberworks.
robotics`'s own docstring for the full disclosure.

## Verification

`clojure -M:dev:test`: 50 tests, 257 assertions, 0 failures, 0 errors.
`clojure -M:lint`: 0 errors, 0 warnings. `clojure -M:dev:run`: completes
with no exceptions, exercising every HARD-hold path including the
genuinely-failing physics-derived over-/under-compression fixtures
(batch-5: 5.0kg platen mass -> 1600.0N, over its own [300,1200]N band;
batch-6: 0.1kg platen mass -> 32.0N, under its own [50,300]N band).
Real, screenshotted WebGPU + WebGL2 render proof of the simulated
compression-platen/rubber-specimen trajectory: see `docs/samples/
scene-data.json` (the real per-tick trajectory `rubberworks.robotics/
simulate-press` produced) and this ADR's accompanying commit for
render-harness screenshot paths.

## Related

- ADR-2607011000 (robotics premise + ISIC coverage)
- ADR-2607151600 (real engineering-simulation integration, automotive
  pilot)
- ADR-2607152000 (real engineering-simulation fleet extension)
- Sibling architecture: `cloud-itonami-isic-2394` `src/cementmill/
  robotics.cljc` (closest technical analog: a press-platen rigid body
  closing onto a static specimen rigid body, force derived from F=m*a
  off the real simulated collision, real-standard-derived crush
  travel), `cloud-itonami-isic-2670` `src/opticsworks/robotics.cljc`
  (two-sided seating-force band + product-class facts-catalog
  structure + render-export/render-harness pattern)
