# P2-026 - CDP Warehouse Quality And Reconciliation Spec

Priority: P2
Sequence: 026
Source: `docs/product-evolution/specs/p2-022-cdp-warehouse-ingestion-and-aggregation.md`, `docs/product-evolution/specs/p2-024-cdp-warehouse-operations-api-and-scheduler.md`, `docs/product-evolution/specs/p2-025-cdp-warehouse-realtime-retry-buffer.md`
Implementation plan: `../plans/p2-026-cdp-warehouse-quality-and-reconciliation-plan.md`

Status: Historical plan evidence records implementation and verification; runtime verification plus commit and merge status was not verified in this docs-only audit.

## Goal

Add a production data-quality gate for the CDP warehouse: operators must be able to compare authoritative MySQL accepted event counts with Doris ODS counts for bounded windows, inspect aggregation lag, persist check results, and schedule conservative recurring checks.

## Current Baseline

- P2-022 mirrors accepted CDP events to Doris and can backfill/aggregate bounded windows.
- P2-024 exposes warehouse status and job operations.
- P2-025 records failed near-real-time delivery attempts and retries them.
- There is no durable warehouse data-quality check ledger or count reconciliation API.

## In Scope

- A MySQL check ledger for warehouse quality results.
- ODS count reconciliation for bounded tenant/time windows.
- Aggregation watermark lag checks.
- Read API for recent quality checks.
- Operator API for manual reconciliation and lag checks.
- Disabled-by-default scheduler for recurring lag and recent-window ODS checks.
- Tests for schema, count comparison, Doris-disabled behavior, lag status, scheduler gating, and controller delegation.

## Out Of Scope

- Full data catalog, lineage graph, or column-level quality rules.
- Multi-table reconciliation beyond CDP ODS and aggregate watermark lag.
- Alert delivery integrations.
- Multi-node distributed scheduling locks.
- Flink CDC or exactly-once stream validation.

## Architecture

The service treats MySQL `cdp_event_log` as the source of truth and Doris ODS as the analytical projection. Each check writes a durable row with the check type, window, source count, warehouse count, diff, status, and details.

```text
Operator / Scheduler
    |
    v
CdpWarehouseQualityService
    |
    +--> MySQL cdp_event_log count
    +--> Doris ODS count
    +--> cdp_warehouse_watermark lag
    +--> cdp_warehouse_quality_check ledger
```

## Runtime Semantics

1. ODS reconciliation requires `from < to` and uses tenant scope.
2. When Doris JDBC is disabled, reconciliation records `SKIPPED` rather than failing the operator path.
3. Count diff within tolerance records `PASS`.
4. Count diff above tolerance records `WARN`.
5. Aggregation lag checks read `CDP_EVENT_AGGREGATE/WINDOW_END`.
6. Missing aggregate watermark records `WARN`.
7. The scheduler is disabled by default and guarded against overlapping cycles.

## Functional Requirements

1. Quality check rows must preserve tenant, check type, status, counts, diff, window, threshold, details, checked time, and operator.
2. ODS reconciliation must compare accepted MySQL `cdp_event_log` count with Doris `canvas_ods.cdp_event_log` count for the same tenant/window.
3. ODS reconciliation must be bounded and reject invalid windows.
4. Aggregation lag checks must classify lag as `PASS` when lag minutes are within threshold and `WARN` when above threshold or missing.
5. Recent checks API must be tenant scoped and bounded.
6. Scheduler must be disabled by default and overlap guarded.

## Technical Scope

- `backend/canvas-engine/src/main/resources/db/migration/V193__cdp_warehouse_quality_checks.sql`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpWarehouseQualityCheckDO.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpWarehouseQualityCheckMapper.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseQualityService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseQualityScheduler.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseQualityController.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseQualitySchemaTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseQualityServiceTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseQualitySchedulerTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/web/CdpWarehouseQualityControllerTest.java`

## Acceptance Criteria

- Schema test proves the quality check ledger and indexes exist.
- Service tests prove ODS reconciliation records `PASS`, `WARN`, and `SKIPPED`.
- Service tests prove aggregation lag status from watermark.
- Controller tests prove tenant-scoped manual quality endpoints.
- Scheduler tests prove disabled and overlapping cycles skip work.
- Focused backend tests pass for P2-026 plus P2-022/P2-024/P2-025 warehouse regressions.

## Rollout

1. Deploy the quality check table and API with scheduler disabled.
2. Run manual ODS reconciliation for a small staging tenant window.
3. Compare `WARN` checks against P2-025 retry rows and P2-024 watermarks.
4. Enable scheduler with conservative windows after manual checks are stable.
5. Add external alerting only after quality rows prove useful thresholds.

## Rollback

- Disable `canvas.warehouse.quality.enabled`.
- Stop calling the quality endpoints.
- Keep check rows for audit; they do not affect ingestion, retries, backfill, or aggregation.
