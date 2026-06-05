# P2-049 - CDP Warehouse Offline Cycle Orchestration Spec

Priority: P2
Sequence: 049
Source: `docs/product-evolution/specs/p2-022-cdp-warehouse-ingestion-and-aggregation.md`, `docs/product-evolution/specs/p2-024-cdp-warehouse-operations-api-and-scheduler.md`, `docs/product-evolution/specs/p2-028-cdp-warehouse-scheduler-lease.md`
Implementation plan: `../plans/p2-049-cdp-warehouse-offline-cycle-orchestration-plan.md`

## Goal

Add an offline warehouse orchestration layer that plans and runs a tenant-scoped CDP warehouse cycle with explicit dependency gates: accepted-event backfill must succeed before bounded aggregation is allowed to run.

## Current Baseline

- P2-022 added CDP event backfill and Doris aggregation.
- P2-024 added operations APIs and a scheduler that runs backfill then aggregation.
- P2-028 added tenant-scoped scheduler leases.
- Current scheduler execution is sequential but not dependency-aware: aggregation can still be invoked after a failed backfill.
- Operators cannot preview the next offline cycle before running it.

## In Scope

- Add an offline cycle plan API that previews the next backfill and aggregation window from watermarks.
- Add an offline cycle run API that executes backfill and aggregation with a dependency gate.
- Persist the orchestration cycle in the existing `cdp_warehouse_sync_run` ledger with job type `OFFLINE_CYCLE`.
- Update the warehouse scheduler to call the orchestration entrypoint.
- Add focused service, scheduler, and controller tests.

## Out Of Scope

- A new orchestration table.
- Multi-tenant fanout scheduling.
- Cross-dataset DAG scheduling.
- Live Airflow, DolphinScheduler, Flink, or Kubernetes integration.
- UI.

## Runtime Semantics

1. A cycle is tenant scoped.
2. Plan reads `CDP_EVENT_BACKFILL/LAST_EVENT_ID` and `CDP_EVENT_AGGREGATE/WINDOW_END` watermarks.
3. Backfill step is always planned as `READY`.
4. Aggregation step is planned as `WAITING_FOR_BACKFILL` when the next bounded window is due.
5. Aggregation step is planned as `SKIPPED` when no aggregation window is due.
6. Run inserts an `OFFLINE_CYCLE` row in `cdp_warehouse_sync_run` before executing steps.
7. Backfill failure marks the cycle `FAILED` and blocks aggregation.
8. Backfill success allows aggregation to run only when the planned window is due.
9. Aggregation failure marks the cycle `FAILED`.
10. Successful backfill plus successful or not-due aggregation marks the cycle `SUCCESS`.

## Functional Requirements

1. Operators can preview the next offline cycle with tenant, backfill limit, aggregation window, and step plans.
2. Operators can run the offline cycle manually through a tenant-scoped API.
3. Scheduler uses the same orchestration behavior as manual run.
4. The cycle run records source id, aggregation window, status, loaded rows, failed rows, error message, start, finish, and operator in the existing run ledger.
5. Aggregation is not invoked after failed backfill.
6. Existing direct backfill and aggregate APIs remain available.

## Technical Scope

- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseOperationsService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseScheduler.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseController.java`
- Tests under `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/` and `backend/canvas-engine/src/test/java/org/chovy/canvas/web/`.

## Acceptance Criteria

- P2-049 spec and plan are indexed.
- No Flyway migration is added.
- Plan API returns backfill and aggregation step plans from watermarks.
- Run API records an `OFFLINE_CYCLE` run.
- Aggregation is blocked when backfill fails.
- Scheduler delegates to offline cycle orchestration.
- Focused backend tests pass.
- Warehouse/BI/audience regression passes.

## Rollout

1. Deploy service and API changes.
2. Call plan API in staging and compare watermarks with expected next cycle.
3. Run manual cycle for one tenant and inspect `OFFLINE_CYCLE` ledger row.
4. Enable scheduler after verifying the orchestration row and step result.
5. Keep direct backfill and aggregation APIs for emergency repair.

## Rollback

- Revert scheduler to direct backfill plus aggregation if needed.
- Existing backfill, aggregation, watermarks, and run rows remain valid.
- `OFFLINE_CYCLE` rows are audit evidence and do not affect lower-level jobs.
