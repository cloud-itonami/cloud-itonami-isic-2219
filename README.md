# cloud-itonami-isic-2219

Open Business Blueprint for **ISIC Rev.5 2219**: manufacture of other
rubber products -- rubber-part-batch intake, per-product-class
material-spec evidence verification, end-of-line dimensional/porosity/
flash quality screening, robot ASTM D395 compression-set-test
verification and Rubber Compound Test Certificate finalization for a
community molded/extruded-rubber plant.

This repository publishes a rubber-products-manufacturing actor --
rubber-part-batch intake, per-product-class material-spec evidence-
checklist verification, end-of-line defect screening, robot
compression-set-test-cell verification mission and Rubber Compound
Test Certificate issuance -- as an OSS business that any qualified
rubber-products manufacturer can fork, deploy, run, improve and sell,
so a plant keeps its own production and conformance history instead of
renting a closed MES / quality SaaS.

Built on this workspace's
[`langgraph`](https://github.com/kotoba-lang/langgraph)
StateGraph runtime (portable `.cljc`, supervised superstep loop,
interrupts, Datomic/in-mem checkpoints) -- the same actor pattern as
every prior actor in this fleet -- here it is **Rubber Advisor ⊣
Compression-Seal Governor**.

## Scope note: the missing shared upstream stage for BOTH smartphones and vehicles

This repository is scoped to **manufacturing molded/extruded rubber
parts** (ASTM D395 compression-set-test verification, per-product-class
material-spec evidence, end-of-line defect screening, rubber-part-batch
shipment and Rubber Compound Test Certificate issuance). It is not a
device-assembly or vehicle-assembly vertical itself. A rubber-products
manufacturer that produces BOTH smartphone protective cases and
internal gaskets/seals (waterproofing gaskets around ports/buttons) AND
automotive rubber components (weatherstripping/door seals, hoses,
engine/body mounts, grommets) sits directly UPSTREAM of BOTH chains
this fleet has been building out:

- `cloud-itonami-isic-2630` -- manufacture of communication equipment
  (smartphone/communication-device assembly). A smartphone's
  protective case and internal waterproofing gaskets are fundamentally
  rubber-products-manufacturing outputs; this actor's own
  `:actuation/ship-rubber-part-batch` hand-off releases those
  electronics/consumer gasket-and-case batches onward for device
  assembly.
- `cloud-itonami-isic-2910` -- manufacture of motor vehicles (final
  assembly) / `cloud-itonami-isic-2920` -- manufacture of bodies
  (coachwork) for motor vehicles. Weatherstripping/door seals, hoses
  and engine/body mounts are likewise fundamentally rubber-products-
  manufacturing outputs -- `automotive.governor`'s/`bodyshop.
  governor`'s own end-of-line quality gates assume finished, certified
  rubber sealing/mounting components already exist as an input.

This vertical is the natural UNIFYING upstream stage for both chains:
neither a smartphone protective case/gasket nor an automotive
weatherstripping/hose/mount component can ship without a compression-
force-verified, material-spec-certified rubber-part-batch first passing
THIS actor's gates. Distinct from:

- `cloud-itonami-isic-2630` -- device ASSEMBLY (consumes rubber-part-
  batches for protective cases/gaskets, does not produce them).
- `cloud-itonami-isic-2910`/`cloud-itonami-isic-2920` -- vehicle/body
  ASSEMBLY (consumes rubber-part-batches for weatherstripping/hoses/
  mounts, does not produce them).
- `cloud-itonami-isic-2211` -- manufacture of rubber TYRES. Tyres are
  a distinct, already-implemented rubber-products vertical with their
  own material-spec catalog and their own governor (dedicated wear/
  tread-depth/pressure-rating checks a rubber-tyre-specific actor
  covers). THIS actor deliberately covers "other rubber products"
  (seals/gaskets/hoses/mounts/cases) -- never tyres, and
  `rubberworks.facts` cites zero tyre-specific standards.
- `cloud-itonami-isic-2220` -- injection-molded-plastics manufacturing
  (an adjacent but chemically/physically distinct materials vertical
  that may supply this actor's own housing components as an input, not
  a rubber-compounding analog).

## Upstream -> downstream hand-off (2219 -> 2630 / 2910 / 2920)

```text
cloud-itonami-isic-2219 (THIS repo: rubber-part-batch compression-force verification + material-spec cert -> released batch)
  --> cloud-itonami-isic-2630 (smartphone/communication-device assembly: protective-case/gasket integration)
  --> cloud-itonami-isic-2910 / cloud-itonami-isic-2920 (motor-vehicle/body assembly: weatherstripping/hose/mount integration)
```

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot
performs the physical domain work**. Here robots (compression-set-
test-cell handling, end-of-line dimensional/porosity/flash scan)
operate under an actor that proposes actions and an independent
**Compression-Seal Governor** that gates them. The governor never
issues a Rubber Compound Test Certificate itself; `:high`/`:safety-
critical` actions (`:actuation/ship-rubber-part-batch`, `:actuation/
issue-material-certificate`) require human sign-off.

**Robot process simulation is a REAL, time-stepped physics
simulation, not a symbolic field comparison** (native from day one, per
ADR-2607151600/ADR-2607152000's fleet pattern -- this vertical is a NEW
actor built to that standard from day one, not a retrofit):
`rubberworks.robotics` walks every rubber-part-batch through a robot-
executed ASTM D395 compression-set-test mission (`kotoba.robotics`
mission/action/telemetry-proof contracts) -- a real, tested rigid-body
physics engine (`kotoba-lang/physics-2d`) time-steps a moving
compression-platen rigid body closing at a controlled velocity onto a
static rubber-specimen rigid body to the standard's own real 25%
compression fraction, and reads a real peak compression force
(`:sim-peak-compression-force-n`, Newtons) directly off the simulated
collision -- not an invented or hand-set number. The Compression-Seal
Governor independently re-derives the batch's own `:sim-peak-
compression-force-n` against the batch's own recorded `:compression-
force-min-n`/`:compression-force-max-n` acceptance band -- anchored on
a REASONED ENGINEERING ESTIMATE for real rubber compression-force
behavior by durometer (Shore A hardness) class (roughly 50-300 N for a
soft, gasket-grade compound around Shore A 40-50, plausibly 300-1200 N
for a harder, structural-mount-grade compound around Shore A 70-90 --
see `rubberworks.robotics`'s own docstring for the full honest
confidence disclosure) -- never trusting the mission's self-reported
verdict alone.

## Core contract

```text
rubber-part-batch intake + material-spec-rules verify + end-of-line quality screen
  -> Rubber Advisor proposal
  -> Compression-Seal Governor (HARD holds un-overridable)
  -> phase gate (actuation always escalates)
  -> human approval for high stakes
  -> append-only ledger + draft records
```

## Actuation honesty

Shipping a rubber-part-batch onward to a downstream consumer via a
robot handling/dispatch action and issuing a Rubber Compound Test
Certificate produce **unsigned draft records and ledger facts only**.
This actor does not talk to real plant control systems or a downstream
consumer's own intake portal. Signature and hardware dispatch are the
rubber-products manufacturer's own acts.

## Ops

| Op | Effect |
|---|---|
| `:rubber-part-batch/intake` | normalize rubber-part-batch directory patch (phase 3 may auto-commit when clean) |
| `:material-spec-rules/verify` | per-product-class material-spec evidence checklist (ASTM D2000 + ISO 1817 automotive-rubber / IEC 60529 + ASTM D395 electronics-gasket; always human) |
| `:end-of-line-quality/screen` | end-of-line dimensional/porosity/flash defect screen (HARD hold if unresolved) |
| `:robotics/simulate-compression-set-test` | robot ASTM D395 compression-set-test-cell mission (always human; required on file before shipment) |
| `:actuation/ship-rubber-part-batch` | draft rubber-part-batch-shipment record onward to a downstream consumer (always human; HARD hold if robotics-sim missing, independently out-of-tolerance compression force, or durometer deviation out of range) |
| `:actuation/issue-material-certificate` | draft Rubber Compound Test Certificate record (always human) |

## Material-spec schemes (honest coverage)

`rubberworks.facts` seeds two REAL, current, cited product-class
schemes -- see that namespace's own docstring for the full honest
disclosure of why these keys are organized by PRODUCT CLASS, not a
per-country code table (the same structural observation
`moldworks.facts`/`opticsworks.facts` make for their own materials/
optics conformance catalogs):

- **AUTOMOTIVE-RUBBER** -- automotive rubber components
  (weatherstripping/door seals, hoses, engine/body mounts, grommets):
  ASTM D2000 (the real, widely-cited SAE/ASTM line-callout grade-
  classification system for automotive rubber compounds), ISO 1817
  (the real, standard test method for a rubber compound's resistance
  to automotive fluids -- fuel, oil, coolant, brake fluid).
- **ELECTRONICS-GASKET** -- electronics/consumer molded-rubber gaskets
  and protective cases (smartphone waterproofing gaskets, protective-
  case overmolds): IEC 60529 (the real, standard ingress-protection
  rating framework directly relevant to smartphone/device waterproofing
  gaskets), ASTM D395 (the real, standard compression-set/compression-
  force test method `rubberworks.robotics`'s own mission actually
  SIMULATES via a real time-stepped `physics-2d` rigid-body collision,
  not a hand-set field).

A product class not in this table (e.g. the demo's `"MEDDEV-RUBBER"`
scheme) has NO spec-basis and the Compression-Seal Governor HARD-holds
rather than inventing one -- see `rubberworks.facts` for the full
coverage discipline.

## Social / regulatory hand-off

```clojure
(require '[rubberworks.store :as store]
         '[rubberworks.export :as export])

(def db (store/seed-db))
(export/audit-package db)           ;; EDN maps for downstream-consumer/audit hand-off
(export/package->csv-bundle db)     ;; CSV bundle (rubber-part-batches/ledger/shipments/material-certificates)
```

Operator console (static sample): `docs/samples/operator-console.html`.

## Develop

```bash
clojure -M:dev:test
clojure -M:lint
clojure -M:dev:run
```

## License

AGPL-3.0-or-later — see `LICENSE`.

## Operator console (Pages)

After enabling GitHub Pages (Settings → Pages → GitHub Actions), the
static console is at:

https://cloud-itonami.github.io/cloud-itonami-isic-2219/

Local: open `docs/index.html` or `docs/samples/operator-console.html`.

## Export audit package (CLI)

```bash
clojure -M:dev:export
# or: clojure -M:dev:export /tmp/audit-2219
```

Writes CSV files under `out/audit-package/` (or the given directory).

## Render the real compression-set-test simulation (CLI)

```bash
clojure -M:dev:render-export
```

Runs the REAL `physics-2d` ASTM D395 compression-set-test simulation
(`rubberworks.robotics/simulate-press`) and writes the compression-
platen/rubber-specimen rigid bodies' actual per-tick trajectory as
`scene-data.json` (both to `/tmp/render-2219/` and `docs/samples/`) for
a render harness to visualize -- see `docs/samples/scene-data.json`.
