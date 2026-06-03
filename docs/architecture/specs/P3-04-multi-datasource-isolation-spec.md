# Spec: Multi Datasource Isolation

Source package: `docs/architecture/todo/p3/platform-evolution/`

Coverage matrix: `docs/architecture/todo/coverage-matrix.md`

## Verification Status

Planning material with confirmed prerequisites. Current data security and tenant isolation issues remain P0; physical datasource isolation is a later platform evolution.

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
