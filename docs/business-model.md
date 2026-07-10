# Business Model: Manufacture of Structural Metal Products

## Classification
- Repository: `cloud-itonami-isic-2511`
- ISIC Rev.5: `2511` — manufacture of structural metal products — structural-steel assembly fabrication, weld-quality/NDE screening and fabrication-certificate issuance
- Social impact: structural-safety, supply-resilience, industrial-jobs

## Customer
- independent structural-steel fabricators and contract fab shops needing auditable welding-procedure and inspection records
- contract fabricators producing beams, columns, connections and trusses for multiple building/bridge/plant projects
- general contractors and structural engineers needing verifiable weld quality and fabrication-release history
- building-control bodies and certification schemes needing verifiable fabricator-certification evidence
- programs that cannot accept closed, unauditable MES / quality platforms

## Offer
- welding-procedure-qualification (WPS/PQR) and jurisdiction-scope version management
- robotics-assisted welding, fit-up and NDE inspection records
- assembly fabrication-tolerance-deviation and weld chain-of-custody history
- fabrication-certificate drafts and disclosure records
- role-based access and immutable audit ledger
- CSV/EDN audit package export for inspectors

## Revenue
- self-host setup fee
- managed hosting subscription per fab shop / production line
- support retainer with SLA
- welding/NDE robot integration and maintenance

## Trust Controls
- out-of-spec assemblies are blocked; a fabrication certificate is mandatory for release paths; assembly history is immutable
- a robot action the governor refuses is never dispatched to hardware
- every dispatch, hold, approval and disclosure path is auditable
- sensitive design and production data stays outside Git
- a fabricated welding-procedure-rules citation, incomplete evidence, an
  out-of-spec fabrication-tolerance deviation, or an unresolved NDE
  defect -- each forces a hold, not an override
- fabrication-certificate issuance is logged and escalated, and
  cannot be finalized twice for the same assembly
