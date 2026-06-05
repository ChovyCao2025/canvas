# Spec: Multi Datasource Isolation

Source package: `docs/architecture/todo/p3/platform-evolution/`

Coverage matrix: `docs/architecture/todo/coverage-matrix.md`

## Verification Status

Implemented as datasource ownership, transaction-boundary, and migration planning artifacts. Physical datasource isolation remains blocked until tenant/data-security and reconciliation gates pass. Evidence and maps are in `docs/architecture/evidence/p3-04-multi-datasource-isolation.md`, `docs/architecture/datasource-ownership-map.md`, `docs/architecture/datasource-transaction-boundary-map.md`, and `docs/architecture/datasource-migration-plan.md`.

## Source Documents

- `docs/architecture/archive/evolution/multi-datasource-isolation.md`
- `docs/architecture/archive/evolution/target-architecture-overview.md`
- `docs/architecture/archive/reference/database-schema.md`

## Scope

Plan the split between canvas control data, execution/runtime data, and analytics/customer data. The goal is to reduce blast radius and operational coupling while avoiding distributed transaction traps.

## Acceptance Criteria

- Target datasource groups and owned tables are explicitly mapped.
- Cross-datasource writes use events, outbox, saga, or repairable reconciliation instead of implicit distributed transactions.
- Flyway migration ownership and startup order are defined per datasource.
- Tenant visibility and query scope rules remain enforceable after the split.
- Monitoring includes pool health, replication lag if any, migration status, and cross-datasource reconciliation status.
