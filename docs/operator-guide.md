# Operator Guide

## First Deployment
1. Register quality engineers, plants, rubber-part-batches, personnel
   and robots.
2. Import historical rubber-part-batch / end-of-line / material-spec-
   certification records.
3. Run read-only validation and robot mission dry-runs.
4. Configure material-spec-certification evidence checklists and human
   sign-off paths.
5. Publish a dry-run audit export.

## Minimum Production Controls
- governor gate on every robot action before dispatch
- human sign-off for `:high`/`:safety-critical` robot actions (e.g.
  compression-set-test verification on rubber-part-batches, Rubber
  Compound Test Certificate issuance)
- audit export for every shipment, sign-off and disclosure
- backup manual process

## Certification
Certified operators must prove robot-safety integrity, evidence-backed
records and human review for safety-affecting actions.

## Operating states
intake : material-spec-rules-verify : end-of-line-quality-screen : robotics-simulate-compression-set-test : approve : ship-rubber-part-batch : issue-material-certificate : audit

## Audit export (social operation)

After a production session, export the append-only package for
downstream-consumer quality auditors or internal compliance:

```clojure
(require '[rubberworks.store :as store]
         '[rubberworks.export :as export])
(export/audit-package store)        ; EDN maps
(export/package->csv-bundle store)  ; CSV files as string map
```

Drafts remain **unsigned** — signing and shipment to the downstream
device/vehicle assembler's own intake are the rubber-products
manufacturer's own acts (see README Actuation honesty).

Static UI sample: `docs/samples/operator-console.html`.
