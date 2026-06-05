# Datasource Migration Plan

Date: 2026-06-05

Status: P3-04 migration planning artifact. No datasource split is approved by this file.

## Flyway Ownership

| Group | Future Flyway directory | Startup order | Notes |
|---|---|---|---|
| ops | `db/migration/ops` | 1 | Creates tenant, users, auth/options, audit/evidence, and task primitives. |
| control | `db/migration/control` | 2 | Creates canvas authoring, definitions, integration metadata, projects, tags, and forms. |
| runtime | `db/migration/runtime` | 3 | Creates execution, request, trace, DLQ, wait, idempotency, and retention tables. |
| CDP/customer | `db/migration/cdp_customer` | 4 | Creates CDP, audience, notification, reach, consent, suppression, and customer tables. |
| analytics | `db/migration/analytics` | 5 | Creates analytics, warehouse, BI, and AI-derived tables. |

Current state: all groups are still in one Flyway stream. Do not move migration files until each target group has its own datasource, backup owner, restore drill, and migration status dashboard.

## Migration Phases

1. Freeze ownership map and table movement order.
2. Add datasource-group tags to table inventory and runbooks.
3. Add read-only dual-connection proof for one non-critical analytics table.
4. Add outbox/reconciliation for every cross-group write.
5. Run backup and restore drill per target group.
6. Move one group only after rollback proof exists.
7. Keep old datasource read-only during compatibility window.
8. Remove old reads only after reconciliation is clean for the full retention window.

## Rollback

| Area | Rollback rule |
|---|---|
| schema rollback | Do not edit applied migrations. Add forward rollback migrations or disable new application paths. Restore from backup only with DBA approval and evidence. |
| data copy | Keep source datasource authoritative until checksum, row count, tenant count, and sampled payload checks pass. Roll back by disabling target reads and replaying from source. |
| routing rollback | Feature flag datasource routing per group. Roll back by switching group route to source datasource and stopping target writes. |
| application deployment | Use `docs/architecture/evidence/runbooks/deploy-rollback.md` for image/config rollback. |
| reconciliation state | Reconciliation jobs must be idempotent. Roll back by freezing new target writes and replaying the last clean source watermark. |

## Monitoring

Required signals before any split:

- pool health for every datasource group;
- migration status by group and Flyway version;
- replication lag if CDC or replica copy is used;
- event backlog for outbox topics;
- reconciliation failure count and oldest unresolved drift;
- tenant row-count drift by group;
- backup age and last restore drill result;
- query latency and connection saturation by group.

## Evidence Paths

Each promoted movement must write evidence under:

- `docs/architecture/evidence/datasource/<group>-ownership.md`
- `docs/architecture/evidence/datasource/<group>-migration-render.md`
- `docs/architecture/evidence/datasource/<group>-backup-restore.md`
- `docs/architecture/evidence/datasource/<group>-reconciliation.md`
- `docs/architecture/evidence/datasource/<group>-rollback.md`

## Stop Rules

Stop the migration if any condition is true:

- tenant row counts differ without accepted explanation;
- pool health is degraded after routing;
- migration status is unknown or failed;
- replication lag exceeds the accepted window;
- event backlog grows without drain;
- any reconciliation failure affects customer-visible behavior;
- rollback owner is unavailable;
- backup or restore evidence is missing.
