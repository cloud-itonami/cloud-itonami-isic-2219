# Contributing

`cloud-itonami-isic-2219` accepts contributions to the OSS blueprint,
capability bindings, policy tests, documentation and operator model.

## Development
The capability layer lives in `kotoba-lang/*` libraries. This repo holds the
business blueprint and operator contracts.

```bash
clojure -M:dev:test
clojure -M:lint
```

## Rules
- Do not commit real operating, personal or credential data.
- Keep robot dispatch, records and disclosures behind the
  Compression-Seal Governor.
- Treat workflows as high-risk: add tests for robot-safety gating,
  record integrity, disclosure and audit logging.
- Document any new business-model or operator assumption in `docs/`.
- Never fabricate a material-spec citation (ASTM D2000 / ISO 1817 /
  IEC 60529 / ASTM D395 or any other). If you are not confident of a
  product class's requirements or a numeric engineering-estimate
  figure (e.g. a compression-force tolerance band for a given
  durometer/compound), leave it out or disclose the uncertainty
  explicitly in `rubberworks.facts`/`rubberworks.robotics`
  coverage/docstrings -- never invent one or present an unconfident
  citation as certain.

## Pull Requests
PRs should describe: what behavior changed, which policy invariant is
affected, how it was tested, whether operator or certification docs need
updates.
