# P1 - Operator Visibility And Testability

## Sources

- `product-strategy-supplementary-dimensions-2026-05-31.md`
- `product-interaction-directions-2026-06-01.md`
- `product-interaction-directions-2026-06-02.md`

## Why P1

The backend already has several useful capabilities that operators cannot see or use. Exposing them has high ROI after P0 safety work.

## Work Items

- Add execution request management UI where backend controllers already exist.
- Add message sending record search and detail UI.
- Add consent, suppression, channel preference, and frequency policy management UI.
- Add circuit breaker status panel and operational runtime visibility.
- Add canary publish UI using existing canary APIs.
- Add dry-run visualization using existing dry-run execution behavior.
- Add audience estimate before publish.
- Add version history and diff UI where backend version endpoints exist.
- Add canvas editor efficiency improvements: search/locate node, fit view, batch selection, unified context menu, and advanced setting collapse.
- Add table filtering, row selection, CSV/Excel export, fixed operation columns, and column customizer.

## Deferred From Sources

- Real-time collaborative editing, CRDT, full report builder, advanced chart drill-down, and mobile editor are P2/P3.
- User preference infrastructure is deferred to P2 despite the interaction dependency note.

## Dependencies

- Canary UI depends on confirming current canary endpoint contracts.
- Dry-run visualization depends on returned node execution structure; if insufficient, add only the minimal backend enrichment.
- Table export should enforce max row limits before enabling large exports.

## Acceptance Criteria

- Operators can inspect execution requests, message records, runtime breaker state, and canary state from UI.
- Operators can estimate audience and simulate execution before publishing.
- Operators can compare versions and identify material graph/config changes.
- Main operational lists support search/filter, row selection, and export.
