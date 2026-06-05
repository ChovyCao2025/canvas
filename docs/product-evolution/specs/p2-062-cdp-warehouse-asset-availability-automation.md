# P2-062 - CDP Warehouse Asset Availability Automation Spec

Priority: P2
Sequence: 062
Source: `docs/product-evolution/specs/p2-060-cdp-warehouse-consumer-availability-contracts.md`, `docs/product-evolution/specs/p2-022-cdp-warehouse-ingestion-and-aggregation.md`, `docs/product-evolution/specs/p2-032-cdp-warehouse-realtime-pipeline-runtime.md`
Implementation plan: `../plans/p2-062-cdp-warehouse-asset-availability-automation-plan.md`

## Goal

Automatically publish asset availability observations from successful offline warehouse aggregation and realtime pipeline checkpoint evidence so P2-060 consumer contracts can depend on production job evidence rather than only operator-entered observations.

## Current Baseline

- P2-060 stores table, dataset, and metric availability observations through manual APIs.
- P2-061 gates BI and manual audience execution through P2-060 consumer contracts.
- P2-022 offline aggregation records run and watermark metadata but does not publish asset availability observations.
- P2-032 realtime pipelines record checkpoint/runtime health but do not publish sink asset availability observations.
- Consumer contracts can therefore be wired to gates, but production jobs do not yet feed those gates automatically.

## In Scope

- Record `OFFLINE` `TABLE` and `DATASET` availability observations after a successful DWD/DWS aggregation run.
- Record `REALTIME` `TABLE` availability observations after a realtime pipeline checkpoint report.
- Map offline assets to the existing warehouse tables and catalog dataset keys.
- Use checkpoint sink references as realtime table asset keys.
- Preserve aggregation and checkpoint success when availability observation writing is unavailable or fails.
- Add focused service tests for success paths and best-effort side effects.

## Out Of Scope

- New Flyway schema.
- Automatic BI, dashboard, or audience contract discovery.
- UI.
- Replacing P2-060 manual observation APIs.
- Starting, stopping, or deploying external stream processing jobs.

## Runtime Semantics

1. Offline aggregation writes observations only after both DWD and DWS SQL statements succeed.
2. Offline observations use `availabilityMode=OFFLINE`, `status=PASS`, `availableUntil=windowEnd`, `evidenceSource=AGGREGATE_JOB`, and the aggregation run as evidence.
3. Offline observations include:
   - `TABLE:canvas_dwd.cdp_user_event_fact`
   - `TABLE:canvas_dws.user_event_metric_daily`
   - `DATASET:cdp_dwd_user_event_fact`
   - `DATASET:cdp_dws_user_event_metric_daily`
4. Realtime checkpoint observations use `assetType=TABLE`, `assetKey=pipeline.sinkRef`, `availabilityMode=REALTIME`, `evidenceSource=REALTIME_CHECKPOINT`, and the checkpoint id as evidence.
5. Realtime observations use the checkpoint report status. Missing watermark evidence records a `FAIL` asset observation so stale prior PASS evidence is not left active for consumer contracts.
6. Realtime `availableUntil` is the checkpoint watermark when present, otherwise the checkpoint time.
7. Availability observation side effects are best effort and must not reject the underlying aggregation run or checkpoint report.
8. Existing P2-060 manual observation and contract APIs continue independently.

## Functional Requirements

1. A successful offline aggregation automatically makes DWD/DWS table and dataset assets available for the aggregated window.
2. A failed or skipped offline aggregation does not publish PASS asset observations.
3. A realtime checkpoint automatically updates sink table availability for consumer contracts.
4. Missing realtime watermark evidence blocks consumer contracts through a FAIL asset observation.
5. Observation side-effect failures are logged and do not break core warehouse job recording.

## Technical Scope

- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseAggregationService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseRealtimePipelineService.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseAggregationServiceTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseRealtimePipelineServiceTest.java`

## Acceptance Criteria

- P2-062 spec and plan are indexed.
- No Flyway migration is added.
- Offline aggregation service tests prove successful aggregation records four asset observations.
- Offline aggregation service tests prove availability side-effect failure does not change aggregate success.
- Realtime pipeline service tests prove checkpoint reporting records sink table availability.
- Realtime pipeline service tests prove missing watermark records FAIL asset availability.
- Focused backend tests pass.
- Warehouse/BI/audience regression passes.

## Rollout

1. Deploy after P2-060 and P2-061.
2. Register consumer contracts for critical BI and audience consumers using DWD/DWS table or dataset assets.
3. Enable staging aggregation and realtime checkpoint reporters.
4. Compare automated observations with P2-055 window-level availability decisions.
5. Move critical consumers from manual observations to automated evidence.

## Rollback

- Remove the optional availability service injection or stop calling the automated observation paths.
- P2-060 manual observation APIs and P2-061 contract gates continue to work.
