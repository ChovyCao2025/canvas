# P3-04 Multi Datasource Isolation Evidence

Date: 2026-06-05

## Verdict

The current repository remains a single datasource and single Flyway stream. P3-04 now has an ownership map, transaction boundary map, and migration plan, but physical datasource isolation is blocked until P0 tenant/data-security gates and cross-group reconciliation proof are complete.

## Created Documents

- `docs/architecture/decisions/work-products/p3-04-multi-datasource/datasource-ownership-map.md`
- `docs/architecture/decisions/work-products/p3-04-multi-datasource/datasource-transaction-boundary-map.md`
- `docs/architecture/decisions/work-products/p3-04-multi-datasource/datasource-migration-plan.md`

## Inventory Commands

```bash
rg -n "CREATE TABLE" backend/canvas-engine/src/main/resources/db/migration > /tmp/canvas_tables.txt
test -s /tmp/canvas_tables.txt
perl -ne 'if (/CREATE TABLE(?: IF NOT EXISTS)?\s+`?([A-Za-z0-9_]+)/) { print "$1\n" }' backend/canvas-engine/src/main/resources/db/migration/*.sql | sort -u
rg -n "@Transactional|insert\(|update\(|delete\(" backend/canvas-engine/src/main/java/org/chovy/canvas > /tmp/canvas_write_flows.txt
test -s /tmp/canvas_write_flows.txt
```

## Summary

- Target datasource groups: control, runtime, CDP/customer, analytics, and ops.
- Table assignment is recorded in `docs/architecture/decisions/work-products/p3-04-multi-datasource/datasource-ownership-map.md`.
- Cross-group write flows are classified as same-datasource, outbox, saga, reconciliation, or blocked in `docs/architecture/decisions/work-products/p3-04-multi-datasource/datasource-transaction-boundary-map.md`.
- Migration, rollback, and monitoring rules are recorded in `docs/architecture/decisions/work-products/p3-04-multi-datasource/datasource-migration-plan.md`.

## Blockers

- Shared mapper package still allows cross-group table access.
- Canvas Authoring and Execution Runtime still share graph/publish/execution state.
- Execution Runtime still creates CDP users directly.
- Runtime system alerts still call notification services directly.
- Trace, event, CDP, delivery, and BI/warehouse data need stronger tenant, PII, retention, and deletion evidence.

## Verification Commands

```bash
test -f docs/architecture/decisions/work-products/p3-04-multi-datasource/datasource-ownership-map.md
rg -n "control|runtime|CDP/customer|analytics|ops|tenant|PII|retention|backup" docs/architecture/decisions/work-products/p3-04-multi-datasource/datasource-ownership-map.md
test -f docs/architecture/decisions/work-products/p3-04-multi-datasource/datasource-transaction-boundary-map.md
rg -n "outbox|saga|reconciliation|blocked|idempotency|rollback owner" docs/architecture/decisions/work-products/p3-04-multi-datasource/datasource-transaction-boundary-map.md
test -f docs/architecture/decisions/work-products/p3-04-multi-datasource/datasource-migration-plan.md
rg -n "Flyway|startup order|schema rollback|data copy|routing rollback|pool health|migration status|replication lag|reconciliation failure" docs/architecture/decisions/work-products/p3-04-multi-datasource/datasource-migration-plan.md
```

Result: all documentation checks passed.

No files were staged or committed.
