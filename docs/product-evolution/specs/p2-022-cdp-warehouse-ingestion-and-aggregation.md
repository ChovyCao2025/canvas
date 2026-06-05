# P2-022 - CDP Warehouse Ingestion And Aggregation Spec

Priority: P2
Sequence: 022
Source: `docs/product-evolution/specs/p2-021-cdp-olap-audience-materialization.md`, `docs/architecture/archive/evolution/data-platform-architecture.md`
Implementation plan: `../plans/p2-022-cdp-warehouse-ingestion-and-aggregation-plan.md`

## Goal

Close the first operational data-warehouse loop for CDP events: accepted track events must reach Doris ODS in near real time, be replayable from MySQL for offline repair, and feed bounded DWD/DWS aggregation jobs that P2-021 audience materialization can query.

## Current Baseline

- `CdpEventIngestionService` writes accepted track events to `cdp_event_log` and publishes an internal RocketMQ event.
- `DorisStreamLoader` only streams canvas execution traces.
- P2-021 added Doris ODS/DWD/DWS CDP audience DDL and audience materialization boundaries.
- P2-021 explicitly leaves the event mirroring consumer or backfill job as future work.
- Doris is optional and must not become a hard dependency for CDP ingestion.

## In Scope

- A CDP warehouse sink abstraction so ingestion, replay, and future consumers can write the same row contract.
- A Doris Stream Load implementation for `canvas_ods.cdp_event_log`.
- Best-effort near-real-time mirroring from accepted CDP ingestion into the warehouse sink.
- Offline backfill from MySQL `cdp_event_log` to Doris ODS by monotonically increasing event log id.
- Bounded Doris SQL aggregation jobs from ODS to DWD and DWS tables.
- MySQL run ledger and watermark tables for replay/aggregation observability.
- Tests for row serialization, best-effort failure behavior, backfill paging, bounded aggregate SQL, and schema coverage.

## Out Of Scope

- Full Kafka or Canal CDC deployment.
- Flink SQL jobs, Flink state, or exactly-once streaming checkpoints.
- A full lakehouse layer, object storage, or data catalog.
- Operator UI for backfill and job history.
- Replacing P2-021 materialization logic or runtime TAGGER membership.

## Architecture

This slice keeps MySQL as the authoritative audit log and Doris as an analytical projection. CDP ingestion writes MySQL first. After the durable write succeeds, it sends the same row to a warehouse sink. Sink failure is logged but does not reject the user event.

Offline replay uses the same sink and reads MySQL by id, which gives production a deterministic repair path for missed Stream Load attempts. Aggregation jobs run bounded SQL windows in Doris, using explicit `from` and `to` timestamps so they can be scheduled, replayed, and audited.

```text
CDP track API
    |
    v
MySQL cdp_event_log
    |
    +--> near-real-time CdpWarehouseEventSink --> Doris ODS
    |
    +--> offline CdpWarehouseBackfillService ----^

Doris ODS
    |
    v
CdpWarehouseAggregationService
    |
    +--> Doris DWD cdp_user_event_fact
    +--> Doris DWS user_event_metric_daily
    +--> MySQL run ledger and watermark
```

## Runtime Semantics

1. Ingestion validates and inserts the event into `cdp_event_log`.
2. Discovery and internal publishing remain unchanged.
3. A best-effort sink attempts to stream the accepted row to Doris ODS.
4. Sink failures are counted in logs and never roll back MySQL ingestion.
5. Backfill reads accepted rows by `id > lastId`, page-limited, and calls the same sink.
6. Aggregation jobs reject unbounded windows and no-op when Doris is disabled.
7. Successful aggregation updates a warehouse watermark for the job and records a run row.

## Data Model

### MySQL

- `cdp_warehouse_sync_run`
  - Records `REALTIME`, `BACKFILL`, and `AGGREGATE` runs, source range, status, row counts, and bounded errors.
- `cdp_warehouse_watermark`
  - Tracks per-tenant and per-job low-cardinality watermarks such as last synced event id or last aggregated timestamp.

### Doris

The P2-021 Doris tables remain authoritative for this slice:

- `canvas_ods.cdp_event_log`
- `canvas_dwd.cdp_user_event_fact`
- `canvas_dws.user_event_metric_daily`

This spec extends their operational use rather than replacing their DDL.

## Production Controls

- Doris writes are feature-gated by `canvas.doris.enabled`.
- Stream Load must use Doris-compatible field names and datetime formatting.
- Backfill page size is bounded and cannot run with a non-positive limit.
- Aggregation windows must include both start and end timestamps and `from < to`.
- SQL aggregation must include tenant predicates when a tenant id is provided.
- Error messages stored in the run ledger are capped.

## Functional Requirements

1. Accepted CDP events must be serialized into Doris ODS field names without losing tenant, event id, user ids, session, device, platform, properties, event time, or received time.
2. Ingestion must continue to accept events when the warehouse sink fails.
3. Backfill must page accepted events by increasing id and return loaded and failed counts.
4. Aggregation must execute bounded ODS-to-DWD and DWD-to-DWS SQL for a supplied time window.
5. Aggregation must no-op safely when Doris is disabled.
6. Sync run and watermark tables must exist for production observability.
7. P2-021 audience materialization contracts must remain unchanged.

## Technical Scope

- `backend/canvas-engine/src/main/resources/db/migration/V190__cdp_warehouse_runs_and_watermarks.sql`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpWarehouseSyncRunDO.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpWarehouseWatermarkDO.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpWarehouseSyncRunMapper.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpWarehouseWatermarkMapper.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseEventSink.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/doris/DorisCdpEventStreamLoader.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseBackfillService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseAggregationService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpEventIngestionService.java`

## Acceptance Criteria

- Schema tests prove run and watermark metadata exist.
- Unit tests prove CDP event Stream Load JSON lines contain Doris-compatible field names and timestamps.
- Unit tests prove disabled Doris loader skips work.
- Unit tests prove ingestion calls the warehouse sink after MySQL insert and remains successful when the sink throws.
- Unit tests prove backfill pages accepted rows by id and uses the shared sink.
- Unit tests prove aggregation rejects invalid windows, no-ops without Doris, and executes bounded DWD/DWS SQL with tenant scope.
- Focused backend test command passes for all P2-022 tests and touched P2-021 regression tests.

## Rollout

1. Deploy metadata migration.
2. Enable `canvas.doris.enabled` in staging with Doris ODS tables already created.
3. Run a small backfill by tenant and compare MySQL accepted count with Doris ODS count.
4. Enable best-effort realtime mirroring.
5. Schedule bounded aggregation windows and monitor run ledger plus watermarks.

## Rollback

- Disable `canvas.doris.enabled`.
- Keep CDP ingestion on MySQL and internal RocketMQ.
- Stop aggregation scheduling.
- Leave run and watermark tables in place for audit.
