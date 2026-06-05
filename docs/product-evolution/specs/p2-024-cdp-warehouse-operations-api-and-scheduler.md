# P2-024 - CDP Warehouse Operations API And Scheduler Spec

Priority: P2
Sequence: 024
Source: `docs/product-evolution/specs/p2-022-cdp-warehouse-ingestion-and-aggregation.md`
Implementation plan: `../plans/p2-024-cdp-warehouse-operations-api-and-scheduler-plan.md`

## Goal

Turn the P2-022 CDP warehouse jobs into an operable production surface: operators and schedulers must be able to inspect watermarks, inspect recent runs, manually trigger bounded backfill or aggregation, and let the service advance warehouse jobs without duplicate overlapping executions.

## Current Baseline

- P2-022 writes accepted CDP events to Doris ODS through a shared sink.
- P2-022 can backfill MySQL `cdp_event_log` rows by id and aggregate bounded Doris windows.
- Run and watermark tables exist, but there is no read API, manual trigger API, or automatic scheduler around them.
- A warehouse outage still must not block CDP ingestion or canvas execution.

## In Scope

- A `CdpWarehouseOperationsService` that exposes manual backfill, manual aggregation, status, recent runs, and scheduler-safe incremental execution.
- A `CdpWarehouseController` with bounded operator endpoints.
- A scheduler component gated by configuration and protected against overlapping runs in the same JVM.
- Watermark-driven default backfill and aggregation windows.
- Tests for API delegation, status shape, scheduler gating, and overlap prevention.

## Out Of Scope

- Flink CDC, Kafka/Canal/Debezium, or exactly-once streaming checkpoints.
- Multi-node distributed locks.
- Operator frontend screens.
- Full data quality rules, lineage graph, or data catalog.
- Replacing P2-022 sink, backfill, or aggregation internals.

## Architecture

P2-024 keeps P2-022 job implementations as the execution boundary and adds an operational layer above them. The operations service reads watermarks and run ledger rows, computes safe default windows, delegates actual writes to the existing services, and returns small DTOs fit for API and scheduler usage.

```text
Operator / Scheduler
    |
    v
CdpWarehouseOperationsService
    |
    +--> CdpWarehouseBackfillService
    +--> CdpWarehouseAggregationService
    +--> CdpWarehouseSyncRunMapper
    +--> CdpWarehouseWatermarkMapper
```

## Runtime Semantics

1. `GET /warehouse/status` returns recent runs and current watermarks for the current tenant.
2. `POST /warehouse/backfill` manually replays accepted event rows using an explicit or watermark-derived start id.
3. `POST /warehouse/aggregate` manually runs a bounded aggregation window.
4. The scheduler can run incremental backfill and aggregation when enabled.
5. The scheduler does not start a new cycle while the previous cycle is still running in the same JVM.
6. Default windows are bounded and never run without tenant scope.

## Functional Requirements

1. Status must include current tenant id, recent sync runs, and watermarks.
2. Manual backfill must require a positive bounded limit and return the P2-022 result.
3. Manual aggregation must require `from < to` and return the P2-022 result.
4. Incremental backfill must use `CDP_EVENT_BACKFILL/LAST_EVENT_ID` when no explicit start id is supplied.
5. Incremental aggregation must use `CDP_EVENT_AGGREGATE/WINDOW_END` and cap the next window by the configured scheduler window.
6. Scheduler execution must be feature-gated and must skip overlapping cycles.
7. API methods must run blocking mapper/job work on the bounded elastic scheduler.

## Technical Scope

- `docs/product-evolution/specs/p2-024-cdp-warehouse-operations-api-and-scheduler.md`
- `docs/product-evolution/plans/p2-024-cdp-warehouse-operations-api-and-scheduler-plan.md`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseOperationsService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseScheduler.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseController.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseOperationsServiceTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseSchedulerTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/web/CdpWarehouseControllerTest.java`

## Acceptance Criteria

- Service tests prove status returns recent runs and watermarks for a tenant.
- Service tests prove incremental backfill starts from the last event id watermark.
- Service tests prove incremental aggregation advances from the aggregate watermark with a bounded window.
- Scheduler tests prove disabled scheduling skips work and overlapping runs do not execute twice.
- Controller tests prove manual triggers and status APIs delegate through the operations service under tenant scope.
- Focused backend tests pass for P2-024 and existing P2-022 warehouse tests.

## Rollout

1. Deploy API and scheduler disabled by default.
2. Validate status endpoint against existing P2-022 run and watermark tables.
3. Manually run a small backfill and aggregation window in staging.
4. Enable scheduler in staging with conservative page/window settings.
5. Compare MySQL accepted counts, Doris ODS counts, DWD/DWS row counts, and run ledger status.

## Rollback

- Disable `canvas.warehouse.scheduler.enabled`.
- Stop using the operator endpoints.
- Keep P2-022 ingestion/backfill/aggregation services and run tables in place for audit.
