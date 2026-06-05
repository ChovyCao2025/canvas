# P2-033 - CDP Warehouse Realtime Pipeline Incidents Spec

Priority: P2
Sequence: 033
Source: `docs/product-evolution/specs/p2-030-cdp-warehouse-quality-incident-loop.md`, `docs/product-evolution/specs/p2-032-cdp-warehouse-realtime-pipeline-runtime.md`
Implementation plan: `../plans/p2-033-cdp-warehouse-realtime-pipeline-incidents-plan.md`

## Goal

Connect realtime warehouse pipeline runtime health to the existing incident loop so failed, stale, or lagging CDC/Flink/Kafka-style pipelines become operator-visible incidents.

## Current Baseline

- P2-030 records quality check incidents and exposes list/ack/resolve APIs.
- P2-032 records realtime pipeline contracts and checkpoint evidence, and evaluates runtime status as `PASS`, `WARN`, or `FAIL`.
- P2-032 explicitly leaves incident routing as a later slice.

## In Scope

- Incident creation from realtime pipeline `WARN` and `FAIL` states.
- Automatic incident recording when checkpoint reports evaluate to `WARN` or `FAIL`.
- Manual scan API for stale pipeline status that is only visible when runtime status is evaluated.
- Tests proving the realtime pipeline incident loop is tenant scoped and best effort.

## Out Of Scope

- New incident table or a parallel alert table.
- External alert routing to Slack, PagerDuty, Feishu, or email.
- Assignment, SLA escalation, or notification fanout.
- Live Flink REST API polling.

## Runtime Semantics

1. Realtime pipeline incidents reuse `cdp_warehouse_incident`.
2. Incident keys are deduplicated by `REALTIME_PIPELINE:{pipeline_key}`.
3. `FAIL` pipeline status maps to `CRITICAL` severity.
4. `WARN` pipeline status maps to `WARN` severity.
5. `PASS` pipelines do not open incidents.
6. Checkpoint report incident creation is best effort and must not reject runtime evidence ingestion.
7. Manual scan reads current pipeline status and records incidents for every non-pass pipeline.

## Functional Requirements

1. `CdpWarehouseIncidentService` must support realtime pipeline incident inputs.
2. `CdpWarehouseRealtimePipelineService.reportCheckpoint` must record an incident when the evaluated checkpoint status is `WARN` or `FAIL`.
3. Incident side effects from checkpoint reporting must be best effort.
4. A scan service must convert stale, lagging, missing-evidence, and failed runtime status rows into incidents.
5. A tenant-scoped API must trigger scan and return total, opened, skipped, and failed counts.
6. Existing quality incident behavior must remain unchanged.

## Technical Scope

- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseIncidentService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseRealtimePipelineService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseRealtimePipelineIncidentService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseRealtimePipelineIncidentController.java`
- Tests under `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/` and `backend/canvas-engine/src/test/java/org/chovy/canvas/web/`.

## Acceptance Criteria

- Incident service tests prove realtime pipeline `WARN`/`FAIL` incidents are opened and `PASS` is ignored.
- Pipeline service tests prove checkpoint report warnings/failures record incidents best effort.
- Scan service tests prove non-pass runtime rows create incidents and pass rows are skipped.
- Controller tests prove tenant-scoped scan API.
- Focused backend tests pass.
- Existing quality incident and realtime pipeline tests continue to pass.

## Rollout

1. Deploy the service change.
2. Continue collecting P2-032 checkpoint evidence.
3. Use `/warehouse/realtime/pipelines/incidents/scan` in staging to create incidents for current runtime status.
4. Schedule scan in a later slice once operator workflow is validated.

## Rollback

- Stop calling the scan endpoint.
- Checkpoint reporting continues to record P2-032 runtime evidence even if incident side effects fail.
- Existing P2-030 quality incidents remain available.
