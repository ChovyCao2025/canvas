# P2-069 - CDP Warehouse Realtime Physical E2E Certification Spec

Priority: P2
Sequence: 069
Source: `docs/product-evolution/specs/p2-032-cdp-warehouse-realtime-pipeline-runtime.md`, `docs/product-evolution/specs/p2-047-cdp-warehouse-realtime-job-control-plane.md`, `docs/product-evolution/specs/p2-066-cdp-warehouse-physical-e2e-certification.md`, `docs/product-evolution/specs/p2-068-cdp-warehouse-e2e-certification-scheduler-and-gate.md`
Implementation plan: `../plans/p2-069-cdp-warehouse-realtime-physical-e2e-certification-plan.md`

Status: Historical plan evidence records implementation and verification; runtime verification plus commit and merge status was not verified in this docs-only audit.

## Goal

Extend warehouse physical E2E certification so production promotion can require fresh realtime pipeline checkpoint and realtime job heartbeat evidence, not only Doris JDBC and live table contract proof.

## Current Baseline

- P2-032 stores realtime pipeline contracts and checkpoint health for CDC/Flink/Kafka-style jobs.
- P2-047 stores realtime job instances, heartbeats, desired state, and action audit.
- P2-066 certifies production readiness, Doris connectivity, and live table contracts.
- P2-067 persists certification history.
- P2-068 gates production promotion on fresh persisted PASS certification evidence.
- The remaining gap is that the physical E2E certification can still pass without proving realtime pipeline and external realtime job health in the same certification run.

## In Scope

- Add `requireRealtime` to immediate E2E certification, certification run history, scheduler config, and gate evaluation.
- Reuse `CdpWarehouseRealtimePipelineService.status` for active realtime pipeline checkpoint health.
- Reuse `CdpWarehouseRealtimeJobControlService.status` for external realtime job heartbeat and desired-state health.
- Add realtime evidence keys to certification evidence:
  - `realtime_pipeline_status`
  - `realtime_job_status`
- Persist realtime pipeline and job summaries as JSON in certification run history.
- Fail closed when `requireRealtime=true` and realtime services are missing, no active pipeline/job evidence exists, or any realtime summary has WARN/FAIL.
- Preserve dry-run diagnostics when `requireRealtime=false`: missing realtime proof returns WARN, while configured realtime WARN/FAIL still appears in evidence.
- Add focused service, schema, run history, scheduler, gate, and controller tests.

## Out Of Scope

- Starting, stopping, or deploying Flink, Kafka, Kafka Connect, RocketMQ, Doris routine-load, or Kubernetes jobs.
- Polling Flink REST, Kafka Connect REST, Kafka admin APIs, or Kubernetes APIs.
- Creating a new realtime pipeline runtime implementation.
- Replacing P2-032, P2-047, P2-048, P2-066, P2-067, or P2-068 APIs.
- UI.

## Runtime Semantics

1. `requireRealtime` defaults to `true` for immediate certification, manual persisted runs, scheduled runs, and gate checks.
2. `requireRealtime=false` is only a diagnostic dry-run mode and must not be treated as production PASS evidence when a production gate requests realtime proof.
3. The certification first runs existing P2-066 logical readiness, Doris connectivity, and live table inspection logic.
4. It then evaluates realtime pipeline status for the current tenant with a bounded recent checkpoint limit.
5. It then evaluates realtime job status for the current tenant with a bounded heartbeat-age threshold and limit.
6. Pipeline evidence is PASS only when at least one active pipeline is evaluated and all active pipelines are PASS.
7. Job evidence is PASS only when at least one realtime job instance is evaluated and all jobs are PASS.
8. If `requireRealtime=true`, missing realtime service, zero active pipelines, zero job instances, WARN, or FAIL produces non-PASS certification evidence.
9. Overall certification status remains worst evidence status: FAIL beats WARN beats PASS.
10. Persisted run gate matching must require a run with `requireRealtime=true` whenever the gate request requires realtime proof.

## Functional Requirements

1. Operators can request immediate physical E2E certification with explicit realtime proof requirement.
2. Certification output includes realtime pipeline status evidence.
3. Certification output includes realtime job heartbeat status evidence.
4. Certification history persists `requireRealtime` and realtime status summaries.
5. Scheduled certification can be configured to require realtime proof.
6. Gate evaluation rejects runs that did not require realtime proof when the gate request requires realtime proof.
7. Existing callers that only use `requirePhysical` remain source-compatible through overloads, but API defaults require realtime proof.
8. Unit tests must not require real Doris, Flink, Kafka, or Kafka Connect.

## Technical Scope

- Add migration `backend/canvas-engine/src/main/resources/db/migration/V247__cdp_warehouse_e2e_realtime_evidence.sql`
- Modify `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpWarehouseE2eCertificationRunDO.java`
- Modify `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehousePhysicalE2eCertificationService.java`
- Modify `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseE2eCertificationRunService.java`
- Modify `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseE2eCertificationGateService.java`
- Modify `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseE2eCertificationScheduler.java`
- Modify `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehousePhysicalE2eCertificationController.java`
- Modify `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseE2eCertificationRunController.java`
- Modify `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseE2eCertificationGateController.java`
- Add or update focused tests under `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/` and `backend/canvas-engine/src/test/java/org/chovy/canvas/web/`

## Acceptance Criteria

- P2-069 spec and plan are indexed.
- Migration test proves certification history has `require_realtime`, `realtime_pipeline_status_json`, and `realtime_job_status_json`.
- Physical certification service tests prove PASS when production readiness, Doris, live tables, realtime pipelines, and realtime jobs pass.
- Physical certification service tests prove FAIL when realtime proof is required and no realtime pipeline or job evidence exists.
- Physical certification service tests prove WARN in dry-run mode when realtime services are missing.
- Run service tests prove realtime requirement and realtime summaries are persisted.
- Gate tests prove realtime-required gates reject runs that did not require realtime proof.
- Controller tests prove `requireRealtime` request binding for immediate certification, persisted run trigger, and gate.
- Scheduler tests prove configured `requireRealtime` is delegated to run service.
- Focused backend tests pass.
- Main compile passes.
- Warehouse/BI/audience regression passes.

## Rollout

1. Deploy migration and code with scheduler still disabled.
2. In staging, ensure P2-032 pipelines and P2-047 realtime jobs report healthy checkpoint and heartbeat evidence.
3. Run immediate `/warehouse/e2e-certification?requirePhysical=true&requireRealtime=true`.
4. Run persisted certification and verify `/warehouse/e2e-certification/gate?requireRealtime=true` passes only for realtime-certified runs.
5. Enable scheduled certification with `canvas.warehouse.e2e-certification-scheduler.require-realtime=true`.
6. Treat realtime-required gate PASS as production promotion evidence for CDP/OLAP realtime warehouse consumers.

## Rollback

- Set `requireRealtime=false` on diagnostic calls while investigating realtime control-plane rollout.
- Disable scheduled certification if realtime evidence is not yet stable.
- Existing P2-066 Doris-only physical evidence remains available through service overloads and persisted rows with `require_realtime=0`.
