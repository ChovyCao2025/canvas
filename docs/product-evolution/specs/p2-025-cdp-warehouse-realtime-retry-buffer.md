# P2-025 - CDP Warehouse Realtime Retry Buffer Spec

Priority: P2
Sequence: 025
Source: `docs/product-evolution/specs/p2-022-cdp-warehouse-ingestion-and-aggregation.md`, `docs/product-evolution/specs/p2-024-cdp-warehouse-operations-api-and-scheduler.md`
Implementation plan: `../plans/p2-025-cdp-warehouse-realtime-retry-buffer-plan.md`

## Goal

Make near-real-time CDP warehouse delivery repairable without waiting for manual backfill: when the warehouse sink fails during ingestion, the service must record a retryable delivery row, retry due rows on a bounded schedule, and mark exhausted rows dead for operator investigation.

## Current Baseline

- P2-022 mirrors accepted `cdp_event_log` rows to Doris ODS through `CdpWarehouseEventSink`.
- Sink failures are logged but not persisted.
- P2-024 can backfill by id, but operators need to discover and run repairs manually.
- The internal `CDP_EVENT_INGESTED` RocketMQ event exists, but no warehouse-specific consumer or retry ledger owns Doris delivery.

## In Scope

- A MySQL retry ledger for failed near-real-time warehouse deliveries.
- A service that enqueues failed event log ids idempotently.
- A bounded retry worker that claims due rows, loads the authoritative `cdp_event_log` row, calls the shared warehouse sink, and marks success/retry/dead.
- A scheduler that is disabled by default and overlap guarded.
- Tests for schema, enqueue idempotency, retry success, retry dead-lettering, scheduler gating, and ingestion failure enqueue behavior.

## Out Of Scope

- Flink CDC, Flink SQL, Kafka, Canal, Debezium, or exactly-once checkpointing.
- Replacing the existing inline best-effort sink call.
- Multi-node distributed locking.
- Operator UI for retry rows.
- Cross-table CDC for full warehouse synchronization.

## Architecture

The retry buffer sits behind the current P2-022 ingestion hook. MySQL remains the authoritative CDP event log. Doris remains an analytical projection. Failed near-real-time projection attempts become durable retry rows keyed by tenant and event log id.

```text
CDP ingest -> MySQL cdp_event_log
    |
    +--> inline CdpWarehouseEventSink
           |
           +-- success: done
           +-- failure: CdpWarehouseRealtimeRetryService.enqueueFailure

CdpWarehouseRealtimeRetryScheduler
    |
    v
CdpWarehouseRealtimeRetryService.retryDue
    |
    +--> claim retry row
    +--> load cdp_event_log
    +--> CdpWarehouseEventSink
    +--> mark SUCCESS / RETRY / DEAD
```

## Runtime Semantics

1. CDP ingestion still accepts the event once MySQL insert succeeds.
2. Warehouse sink failures still do not roll back ingestion.
3. A failed sink attempt inserts or updates one retry row per tenant and event log id.
4. Retry processing only claims due `PENDING` or `RETRY` rows.
5. A successful retry marks the row `SUCCESS` and clears retry scheduling.
6. A failed retry increments attempt count and schedules the next retry.
7. Rows at or above max attempts are marked `DEAD`.

## Functional Requirements

1. Retry rows must be unique by `(tenant_id, event_log_id)`.
2. Enqueue must preserve event log id, tenant id, message id, event code, first error, last error, attempt count, next retry time, and status.
3. Retry processing must use the shared `CdpWarehouseEventSink`.
4. Retry processing must skip missing event log rows by marking the retry row `DEAD`.
5. Retry processing must bound each cycle by a configured limit.
6. Scheduler must be disabled by default and must prevent overlapping cycles in one JVM.
7. Ingestion tests must prove sink failure creates a retry row without rejecting the accepted event.

## Technical Scope

- `backend/canvas-engine/src/main/resources/db/migration/V192__cdp_warehouse_realtime_retry.sql`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpWarehouseRealtimeRetryDO.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpWarehouseRealtimeRetryMapper.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseRealtimeRetryService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseRealtimeRetryScheduler.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpEventIngestionService.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseRealtimeRetrySchemaTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseRealtimeRetryServiceTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseRealtimeRetrySchedulerTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/CdpEventIngestionWarehouseRetryTest.java`

## Acceptance Criteria

- Schema test proves retry table, uniqueness, status index, and due-time index exist.
- Service tests prove enqueue is idempotent for the same tenant/event log id.
- Service tests prove retry success marks `SUCCESS`.
- Service tests prove retry failure schedules `RETRY` and max attempts mark `DEAD`.
- Scheduler tests prove disabled and overlapping cycles skip work.
- Ingestion test proves warehouse sink failure enqueues retry and ingestion still succeeds.
- Focused backend tests pass for P2-025 plus P2-022/P2-024 regressions.

## Rollout

1. Deploy retry table and service with scheduler disabled.
2. Confirm failed inline sink attempts create retry rows in staging.
3. Enable retry scheduler with low cycle limit and conservative max attempts.
4. Monitor retry statuses, P2-024 run/watermark status, and Doris ODS counts.
5. Keep offline backfill available for broad repairs.

## Rollback

- Disable `canvas.warehouse.realtime-retry.enabled`.
- Continue using P2-022 inline sink and P2-024 manual backfill.
- Leave retry rows in place for audit or later replay.
