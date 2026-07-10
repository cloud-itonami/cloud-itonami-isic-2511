# cloud-itonami-isic-2511

Open Business Blueprint for **ISIC Rev.5 2511**: manufacture of
structural metal products -- structural-steel assembly fabrication,
weld-quality/NDE screening and fabrication-certificate issuance for a
community structural-steel fabricator.

This repository publishes a structural-steel-fabrication actor --
assembly intake, per-jurisdiction welding-procedure-qualification
rules verification, NDE-defect screening, robot assembly-dispatch and
fabrication-certificate finalization -- as an OSS business that any
qualified structural-steel fabricator can fork, deploy, run, improve
and sell, so a fab shop keeps its own construction and inspection
history instead of renting a closed MES / quality SaaS.

Built on this workspace's
[`langgraph`](https://github.com/kotoba-lang/langgraph)
StateGraph runtime (portable `.cljc`, supervised superstep loop,
interrupts, Datomic/in-mem checkpoints) -- the same actor pattern as
every prior actor in this fleet -- here it is **Structural Fabrication
Advisor ⊣ Structural Fabrication Governor**.

## Scope note: fabrication, not steel-making or erection

This repository is scoped to **fabricating** structural-steel
assemblies (beams, columns, connections, trusses) for buildings,
bridges and industrial plant from mill-supplied steel -- welding
procedure qualification, NDE inspection, fabrication-certificate
issuance. It is neither an upstream steel-making vertical nor an
on-site construction/erection vertical. Distinct from:

- `cloud-itonami-isic-2410` — basic iron and steel **manufacturing**
  (the upstream mill that casts the heats this fab shop buys steel
  from; heat chemistry and mill-cert, not weld fabrication)
- `cloud-itonami-isic-2512` — manufacture of tanks, reservoirs and
  containers of metal (adjacent structural-metal-products class, not
  yet an implemented actor)
- `cloud-itonami-isic-3011` — building of ships and floating
  structures (a different structural-fabrication domain: hull blocks
  for ships, not building/bridge steel)
- on-site construction/erection ISICs -- this repo never dispatches a
  crane or a field crew; it drafts the fab-shop's own release records

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot
performs the physical domain work**. Here robots (welding, fit-up,
NDE scan) operate under an actor that proposes actions and an
independent **Structural Fabrication Governor** that gates them. The
governor never issues a fabrication certificate itself; `:high`/
`:safety-critical` actions (`:actuation/dispatch-assembly`,
`:actuation/issue-fabrication-certificate`) require human sign-off.

## Core contract

```text
assembly intake + welding-procedure rules verify + NDE-inspection screen
  -> Structural Fabrication Advisor proposal
  -> Structural Fabrication Governor (HARD holds un-overridable)
  -> phase gate (actuation always escalates)
  -> human approval for high stakes
  -> append-only ledger + draft records
```

## Actuation honesty

Dispatching a welding/fit-up robot and issuing a fabrication
certificate produce **unsigned draft records and ledger facts only**.
This actor does not talk to real fab-shop control systems or
certification-body portals. Signature and hardware dispatch are the
structural-steel fabricator's own acts.

## Ops

| Op | Effect |
|---|---|
| `:assembly/intake` | normalize assembly directory patch (phase 3 may auto-commit when clean) |
| `:welding-procedure/verify` | per-jurisdiction WPS/PQR + fabricator-certification evidence checklist (always human) |
| `:nde-inspection/screen` | NDE weld-defect screen (HARD hold if unresolved) |
| `:actuation/dispatch-assembly` | draft assembly-dispatch record (always human) |
| `:actuation/issue-fabrication-certificate` | draft fabrication-certificate record (always human) |

## Social / regulatory hand-off

```clojure
(require '[structuralsteel.store :as store]
         '[structuralsteel.export :as export])

(def db (store/seed-db))
(export/audit-package db)           ;; EDN maps for inspector/certification-body hand-off
(export/package->csv-bundle db)     ;; CSV bundle (assemblies/ledger/dispatches/fabrication-certificates)
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

https://cloud-itonami.github.io/cloud-itonami-isic-2511/

Local: open `docs/index.html` or `docs/samples/operator-console.html`.

## Export audit package (CLI)

```bash
clojure -M:dev:export
# or: clojure -M:dev:export /tmp/audit-2511
```

Writes CSV files under `out/audit-package/` (or the given directory).
