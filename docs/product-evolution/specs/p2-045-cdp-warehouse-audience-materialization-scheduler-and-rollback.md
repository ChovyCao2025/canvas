# P2-045 - CDP Warehouse Audience Materialization Scheduler And Rollback Spec

Priority: P2
Sequence: 045
Source: `docs/product-evolution/specs/p2-021-cdp-olap-audience-materialization.md`, `docs/product-evolution/specs/p2-028-cdp-warehouse-scheduler-lease.md`, `docs/product-evolution/specs/p2-035-cdp-warehouse-audience-materialization-operations.md`
Implementation plan: `../plans/p2-045-cdp-warehouse-audience-materialization-scheduler-and-rollback-plan.md`

## Goal

Close the production loop for CDP OLAP audience materialization by adding lease-protected scheduled refreshes and guarded bitmap version rollback, so offline and hybrid audiences can stay fresh and operators can safely recover from bad materialized versions.

## Current Baseline

- P2-021 provides stable user indexes, versioned Redis bitmaps, OLAP rule compilation, and materialization runs.
- P2-028 provides a tenant-scoped distributed warehouse job lease.
- P2-035 exposes manual audience materialization and recent run history.
- Audience materialization still relies on manual triggers.
- Latest audience membership is selected from the highest DB `READY` bitmap version, so rollback must change DB version state rather than only changing Redis pointers.

## In Scope

- Add an additive rollback audit table.
- Add bitmap rollback semantics that mark newer `READY` versions as `ROLLED_BACK`.
- Add an operator API to rollback an audience to a previous ready bitmap version.
- Add due-audience refresh logic for enabled `OFFLINE_BATCH` and `HYBRID` audience definitions.
- Add a disabled-by-default scheduled refresh job protected by `CdpWarehouseJobLeaseService`.
- Add an operator API to run one due-refresh cycle on demand.
- Add focused schema, service, scheduler, store, and controller tests.

## Out Of Scope

- Replacing P2-021 bitmap storage.
- Changing online TAGGER evaluation.
- A new workflow engine.
- UI.
- Multi-tenant fan-out scheduling beyond the configured scheduler tenant for this slice.
- Real Kafka/Flink/CDC connector changes.

## Runtime Semantics

1. Scheduled refresh only considers tenant-scoped, enabled audience definitions with `evaluationStrategy` equal to `OFFLINE_BATCH` or `HYBRID`.
2. A definition with no previous successful materialization is due immediately.
3. A definition with a valid cron expression is due when the next cron fire after the last successful run is less than or equal to scheduler `now`.
4. A definition without a valid cron expression is skipped by the due-refresh service.
5. Each due definition is materialized through the existing `AudienceMaterializationService`; failures are counted and do not block the remaining due definitions.
6. The scheduler is disabled by default and uses a local overlap guard plus the existing warehouse lease when available.
7. Rollback requires a target `READY` bitmap version for the same tenant and audience.
8. Rollback marks newer `READY` versions as `ROLLED_BACK`, records an audit row, and updates the Redis latest pointer to the target version.
9. Rollback to the current latest ready version is allowed as an idempotent no-op audit event.

## Functional Requirements

1. Operators can call a rollback endpoint with `targetVersion`, `operator`, and `reason`.
2. Rollback rejects missing, non-positive, or non-`READY` target versions.
3. After rollback, `selectLatestReady` resolves to the rollback target instead of newer rolled-back versions.
4. Rollback records tenant, audience, target version, rolled-back version count, operator, reason, and status.
5. Operators can trigger a bounded due-refresh cycle manually.
6. The scheduler is disabled by default, lease protected, and overlap guarded.
7. Focused tests prove rollback validation, DB-state rollback, scheduler due selection, lease skipping, and controller tenant scoping.

## Technical Scope

- `backend/canvas-engine/src/main/resources/db/migration/V204__cdp_audience_materialization_rollback.sql`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AudienceBitmapRollbackDO.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/AudienceBitmapRollbackMapper.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/AudienceBitmapVersionMapper.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/AudienceDefinitionMapper.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/AudienceMaterializationRunMapper.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/VersionedAudienceBitmapStore.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/analytics/AudienceMaterializationOperationsService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/analytics/AudienceMaterializationScheduleService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/analytics/CdpWarehouseAudienceMaterializationScheduler.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseAudienceMaterializationController.java`
- Focused tests for the above behavior.

## Acceptance Criteria

- P2-045 spec and plan are indexed.
- Migration is additive and does not edit old Flyway migrations.
- Rollback changes DB version state, not only Redis latest pointer.
- Scheduled materialization uses existing materialization service and warehouse lease.
- Manual due-refresh API is bounded and tenant scoped.
- Focused backend tests pass.
- Warehouse/BI/audience regression passes.

## Rollout

1. Deploy migration and service changes with scheduler disabled.
2. Use manual due-refresh endpoint in lower environments to validate cron and materialization behavior.
3. Enable `canvas.warehouse.audience-materialization-scheduler.enabled=true` per tenant when warehouse freshness SLOs are configured.
4. Use rollback endpoint for bad bitmap versions before re-running materialization.

## Rollback

- Disable `canvas.warehouse.audience-materialization-scheduler.enabled`.
- Stop calling the rollback endpoint.
- Existing `ROLLED_BACK` rows remain excluded from latest membership because latest selection only reads `READY` versions.
