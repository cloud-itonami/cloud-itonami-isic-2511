# Operator Guide

## First Deployment
1. Register welding/quality engineers, fab shops, assemblies, personnel and robots.
2. Import historical assembly / NDE / welding-procedure records.
3. Run read-only validation and robot mission dry-runs.
4. Configure welding-procedure-qualification evidence checklists and human sign-off paths.
5. Publish a dry-run audit export.

## Minimum Production Controls
- governor gate on every robot action before dispatch
- human sign-off for `:high`/`:safety-critical` robot actions (e.g. welding/fit-up on safety-critical assemblies, fabrication-certificate issuance)
- audit export for every dispatch, sign-off and disclosure
- backup manual process

## Certification
Certified operators must prove robot-safety integrity, evidence-backed
records and human review for safety-affecting actions.

## Operating states
intake : welding-procedure-verify : nde-inspection-screen : approve : dispatch-assembly : issue-fabrication-certificate : audit

## Audit export (social operation)

After a production session, export the append-only package for
certification-body inspectors or internal compliance:

```clojure
(require '[structuralsteel.store :as store]
         '[structuralsteel.export :as export])
(export/audit-package store)        ; EDN maps
(export/package->csv-bundle store)  ; CSV files as string map
```

Drafts remain **unsigned** — signing and submission to a
certification body are the structural-steel fabricator's own act (see
README Actuation honesty).

Static UI sample: `docs/samples/operator-console.html`.
