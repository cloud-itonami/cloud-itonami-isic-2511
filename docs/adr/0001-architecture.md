# ADR-0001: Structural Fabrication Advisor ⊣ Structural Fabrication Governor architecture

- Status: Accepted (2026-07-10)
- Repository: `cloud-itonami-isic-2511` (ISIC Rev.5 `2511`)

## Context

Structural-steel fabrication (welding-procedure qualification, NDE
weld inspection, fabrication-certificate issuance) needs the same
governed-actor pattern as the rest of the cloud-itonami fleet: an
untrusted advisor proposes; an independent governor may HOLD;
high-stakes actuation never auto-commits.

This vertical is the fifth classic heavy-industry manufacturing
vertical in the steel/engines/vehicles/ships cluster, after basic
iron and steel (`cloud-itonami-isic-2410`), engines and turbines
(`cloud-itonami-isic-2811`), motor vehicles (`cloud-itonami-isic-2910`)
and ships and floating structures (`cloud-itonami-isic-3011`), and the
sixth manufacturing-sector full actor overall after aerospace
(`cloud-itonami-isic-3030`).

## Decision

1. Namespaces live under `structuralsteel.*` with the standard
   facts / registry / store / governor / phase / advisor / operation / sim
   shape.
2. Entity is an **assembly** (a fabricated structural-steel assembly:
   beam, column, connection or truss subassembly), not a vehicle,
   aircraft assembly, hull block or steel heat.
3. Dual actuation on the same entity:
   - `:actuation/dispatch-assembly` (robot welding/fit-up dispatch draft)
   - `:actuation/issue-fabrication-certificate` (fabrication-certificate draft)
4. Double-actuation guards use dedicated booleans
   (`:assembly-dispatched?`, `:fabrication-certified?`), never a status
   lifecycle (ADR-2607071320 / 6492 lesson).
5. `assembly-camber-out-of-range?` continues the fleet two-sided
   range check family (after testlab / conservation / water /
   aerospace / steelworks / turbine / automotive), applied here to an
   assembly's own measured camber deviation against its own recorded
   fabrication-tolerance spec bounds.
6. NDE defect unresolved is evaluated unconditionally so
   `:nde-inspection/screen` itself can HARD-hold (parksafety
   ADR-2607071922 Decision 5 discipline).
7. Spec-basis catalog seeds JPN (Building Center of Japan factory-grade
   certification) / USA (AISC 207 + AWS D1.1 self-certification) / GBR
   (BSI / UKCA + CE under BS EN 1090) / DEU (DIN / EN 1090-1 CE marking
   under the Execution Class system) only; missing jurisdictions are
   uncovered, never fabricated.

## Consequences

(+) Structural-steel fabrication gains a forkable OSS operating stack
with auditable governor holds.
(+) Reuses langgraph + store dual-backend parity without new physics.
(−) No physical fab-shop digital-twin tick in this repo (follow-up
domain data, e.g. giemon-factory style layout, is out of scope here).
(−) Fabricator-certification-authority coverage is a starting
catalog, not exhaustive.

## Related

- Superproject fleet ADR for this promotion (structural-fabrication-2511-coverage)
- Sibling architecture: `cloud-itonami-isic-2410` docs/adr/0001,
  `cloud-itonami-isic-2910` docs/adr/0001
