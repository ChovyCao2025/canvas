# P2-032 - CDP Warehouse Realtime Pipeline Runtime Spec

Priority: P2
Sequence: 032
Source: `docs/product-evolution/specs/p2-029-cdp-warehouse-realtime-checkpoint-and-lag.md`, `docs/product-evolution/specs/p2-031-cdp-warehouse-physical-table-governance.md`
Implementation plan: `../plans/p2-032-cdp-warehouse-realtime-pipeline-runtime-plan.md`

## Goal

Add a production control plane for realtime warehouse pipelines so CDC, Flink SQL, Kafka/RocketMQ, and Doris sink jobs can publish durable runtime evidence: offset, checkpoint id, watermark, lag, delivery semantics, and health.

## Current Baseline

- P2-025 and P2-029 cover direct CDP event warehouse delivery retry and a delivery checkpoint for the current best-effort Stream Load path.
- P2-031 governs Doris physical table contracts.
- There is still no durable model for external realtime jobs such as Flink CDC, Flink SQL, Kafka/RocketMQ consumers, or Doris Routine Load.

## In Scope

- Realtime pipeline contract table with built-in contracts for CDP ODS, trace ODS, DWD event fact, and DWS daily metrics streams.
- Checkpoint event ledger that records checkpoint id, source partition, source offset, committed offset, watermark, lag, row count, status, and reporter.
- Service logic to merge tenant defaults with tenant overrides.
- Runtime health evaluation using delivery semantics, max lag, checkpoint age, and failure reports.
- Tenant-scoped APIs to list/upsert pipeline contracts, report checkpoints, and view realtime pipeline status.

## Out Of Scope

- Deploying a Flink cluster or Kafka Connect cluster.
- Starting/stopping external jobs.
- Consuming binlog or Kafka messages inside canvas-engine.
- Live Flink REST API polling.
- Automatic incident routing; P2-030 remains the incident loop.

## Runtime Semantics

1. Built-in realtime pipeline contracts live at tenant `0`.
2. Tenant-specific contracts override built-ins by `pipeline_key`.
3. Runtime checkpoint events are written under the current tenant.
4. Reporting a checkpoint updates the latest runtime state on the contract row that matched the tenant scope.
5. A reported `FAIL` status makes the runtime status `FAIL`.
6. Missing exactly-once evidence, excessive lag, or stale checkpoint age makes the runtime status `WARN`.
7. A healthy checkpoint with required evidence and bounded lag is `PASS`.

## Functional Requirements

1. Pipeline contracts must capture source type/reference, processor type, sink type/reference, delivery semantics, checkpoint interval, max lag, max checkpoint age, lifecycle status, owner, and config JSON.
2. Built-in contracts must cover:
   - `mysql_cdp_event_log_to_doris_ods`
   - `mysql_canvas_trace_to_doris_ods`
   - `doris_ods_cdp_event_to_dwd_fact`
   - `doris_dwd_user_fact_to_dws_metric_daily`
3. Checkpoint reports must be idempotent by `(tenant_id, pipeline_key, checkpoint_id, source_partition)`.
4. Exactly-once contracts must warn when checkpoint id or committed offset is missing.
5. Runtime status must include latest checkpoint id, offsets, watermark, lag, status, and recent checkpoint evidence.
6. APIs must use tenant context and never expose other tenant runtime evidence.

## Technical Scope

- `backend/canvas-engine/src/main/resources/db/migration/V199__cdp_warehouse_realtime_pipeline_runtime.sql`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpWarehouseStreamPipelineDO.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpWarehouseStreamCheckpointDO.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpWarehouseStreamPipelineMapper.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpWarehouseStreamCheckpointMapper.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseRealtimePipelineService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseRealtimePipelineController.java`

## Acceptance Criteria

- Schema tests prove contract/checkpoint tables, indexes, and built-in pipeline seeds exist.
- Service tests prove upsert validation, tenant override list behavior, checkpoint evidence recording, exactly-once warnings, lag warnings, and failure status.
- Controller tests prove tenant-scoped list, upsert, checkpoint report, and status APIs.
- Focused backend tests pass.
- Existing warehouse realtime checkpoint/retry tests continue to pass.

## Rollout

1. Deploy the additive migration.
2. Register staging Flink CDC/Flink SQL job metadata or use built-in contracts.
3. Configure external jobs to call `/warehouse/realtime/pipelines/checkpoints`.
4. Watch `/warehouse/realtime/pipelines/status` alongside existing `/warehouse/realtime/status`.
5. Use P2-030 incident routing in a later slice to escalate stale or failed pipeline status.

## Rollback

- Stop checkpoint reporting from external jobs.
- Leave runtime evidence tables in place for audit.
- Existing direct Stream Load retry and checkpoint logic continues independently.
