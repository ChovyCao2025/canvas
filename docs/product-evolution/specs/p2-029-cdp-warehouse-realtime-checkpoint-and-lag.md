# P2-029 - CDP Warehouse Realtime Checkpoint And Lag Spec

Priority: P2
Sequence: 029
Source: `docs/product-evolution/specs/p2-022-cdp-warehouse-ingestion-and-aggregation.md`, `docs/product-evolution/specs/p2-025-cdp-warehouse-realtime-retry-buffer.md`, `docs/product-evolution/specs/p2-026-cdp-warehouse-quality-and-reconciliation.md`
Implementation plan: `../plans/p2-029-cdp-warehouse-realtime-checkpoint-and-lag-plan.md`

Status: Historical plan evidence records implementation and verification; runtime verification plus commit and merge status was not verified in this docs-only audit.

## Goal

Add a durable realtime warehouse checkpoint so operators can see the latest CDP event successfully delivered to the warehouse ODS path, recent failures, and retry backlog.

## Current Baseline

- CDP ingestion attempts a best-effort Doris ODS write after MySQL persistence.
- Failed realtime warehouse writes are stored in a retry buffer.
- Quality checks compare MySQL and Doris counts over a window, but there is no realtime delivery checkpoint showing the latest delivered event id and delivery lag.

## In Scope

- A MySQL checkpoint table for realtime CDP warehouse streams.
- A service that records realtime delivery successes and failures.
- Ingestion-path integration after direct Doris sink success/failure.
- Retry-path integration after retry success/failure.
- An API endpoint for realtime checkpoint and retry backlog status.

## Out Of Scope

- Flink checkpoint state.
- Kafka consumer offsets.
- Exactly-once stream processing.
- External alerting integrations.
- Replacing the retry buffer.

## Runtime Semantics

1. The checkpoint is tenant scoped.
2. The CDP event ODS stream key is `CDP_EVENT_ODS`.
3. A successful direct sink or retry delivery increments delivered count and records latest event id, event time, received time, and delivered time.
4. A failed direct sink or retry increments failure count and records the latest failure message.
5. The realtime status API combines checkpoint rows with retry backlog counts.
6. Checkpoint writes must never reject customer event ingestion.

## Functional Requirements

1. Checkpoint rows must be unique by `(tenant_id, stream_key)`.
2. Checkpoint rows must preserve latest event id, message id, event code, event time, received time, delivered time, delivery source, delivered count, failure count, and last failure details.
3. Ingestion sink success must record a delivered checkpoint.
4. Ingestion sink failure must record a failure checkpoint and keep existing retry enqueue behavior.
5. Retry success must record a delivered checkpoint.
6. Retry failure must record a failure checkpoint before retry/dead handling.
7. Realtime status must report retry pending/retrying/sending and dead counts for the tenant.

## Technical Scope

- `backend/canvas-engine/src/main/resources/db/migration/V196__cdp_warehouse_realtime_checkpoint.sql`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpWarehouseRealtimeCheckpointDO.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpWarehouseRealtimeCheckpointMapper.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseRealtimeCheckpointService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseRealtimeController.java`
- Integration in `CdpEventIngestionService`.
- Integration in `CdpWarehouseRealtimeRetryService`.

## Acceptance Criteria

- Schema test proves checkpoint table, unique key, and freshness index exist.
- Service tests prove success/failure checkpoint writes and status aggregation.
- Ingestion tests prove success and failure paths record checkpoint without rejecting the event.
- Retry tests prove success and failure paths record checkpoint.
- Controller tests prove tenant-scoped realtime status delegation.
- Focused warehouse backend tests pass.

## Rollout

1. Deploy the additive checkpoint migration.
2. Enable Doris-backed realtime mirroring in staging.
3. Validate `/warehouse/realtime/status` after direct events and retry recovery.
4. Use the endpoint as input for future alerting.

## Rollback

- Stop calling the realtime status endpoint.
- Leave the checkpoint table in place; it does not affect ingestion success.
