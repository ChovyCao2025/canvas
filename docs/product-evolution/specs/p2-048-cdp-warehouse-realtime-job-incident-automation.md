# P2-048 - CDP Warehouse Realtime Job Incident Automation Spec

Priority: P2
Sequence: 048
Source: `docs/product-evolution/specs/p2-033-cdp-warehouse-realtime-pipeline-incidents.md`, `docs/product-evolution/specs/p2-036-cdp-warehouse-readiness-and-slo-summary.md`, `docs/product-evolution/specs/p2-047-cdp-warehouse-realtime-job-control-plane.md`
Implementation plan: `../plans/p2-048-cdp-warehouse-realtime-job-incident-automation-plan.md`

Status: Historical plan evidence records implementation and verification; runtime verification plus commit and merge status was not verified in this docs-only audit.

## Goal

Connect realtime warehouse job health to the existing incident and readiness loops so external Flink, Kafka Connect, and Doris routine-load style job failures become operator-visible without manual status polling.

## Current Baseline

- P2-033 routes realtime pipeline runtime `WARN` and `FAIL` states into warehouse incidents.
- P2-036 exposes warehouse readiness across offline sync, realtime pipelines, incidents, BI datasources, and audience materialization.
- P2-038 schedules readiness incident scans behind the warehouse lease.
- P2-047 stores concrete realtime job instance heartbeats, desired state, runtime state, and action audit.
- P2-047 job health is queryable, but it is not yet routed into incidents or readiness.

## In Scope

- Add realtime job incident recording through `cdp_warehouse_incident`.
- Add a manual tenant-scoped realtime job incident scan API.
- Add a disabled-by-default scheduler with warehouse lease and overlap guard.
- Include realtime job health in warehouse readiness realtime status.
- Add focused service, scheduler, controller, and readiness tests.

## Out Of Scope

- New incident table or parallel alert lifecycle.
- External alert fanout to Slack, PagerDuty, Feishu, email, or SMS.
- Calling Flink REST, Kafka Connect REST, Kubernetes APIs, or Doris APIs.
- Starting, stopping, or repairing jobs directly from canvas-engine.
- UI.

## Runtime Semantics

1. Realtime job incidents reuse `cdp_warehouse_incident`.
2. Incident keys are deduplicated by `REALTIME_JOB:{pipeline_key}:{job_key}`.
3. `FAIL` job health maps to `CRITICAL` severity.
4. `WARN` job health maps to `WARN` severity.
5. `PASS` jobs do not open incidents.
6. Scan reads `CdpWarehouseRealtimeJobControlService.status(...)` and records incidents for every non-pass job.
7. Scan is best effort: one failed incident write must not stop the rest of the scan.
8. Scheduler is disabled by default and uses `CdpWarehouseJobLeaseService` when available.
9. Readiness realtime status must fail when either pipeline status or job status has failures.
10. Readiness realtime status must warn when either pipeline status or job status has warnings or no runtime evidence exists.

## Functional Requirements

1. `CdpWarehouseIncidentService` must support realtime job incident inputs.
2. A scan service must convert realtime job `WARN` and `FAIL` health into incidents.
3. A tenant-scoped API must trigger scan and return total, opened, skipped, and failed counts.
4. A scheduler must run the scan only when enabled, protected by tenant-scoped lease and overlap guard.
5. `CdpWarehouseReadinessService` must evaluate realtime readiness from both pipeline status and job status.
6. Existing pipeline incident behavior must remain unchanged.

## Technical Scope

- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseIncidentService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseRealtimeJobIncidentService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseRealtimeJobIncidentScheduler.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseReadinessService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseRealtimeJobIncidentController.java`
- Focused tests under `backend/canvas-engine/src/test/java/org/chovy/canvas/`.

## Acceptance Criteria

- P2-048 spec and plan are indexed.
- No new Flyway migration is added because P2-030 and P2-047 already provide required persistence.
- Incident service tests prove realtime job `WARN`/`FAIL` incidents are opened and `PASS` is ignored.
- Scan service tests prove non-pass jobs create incidents and pass jobs are skipped.
- Scheduler tests prove disabled, enabled, lease-denied, and overlap behavior.
- Controller tests prove tenant-scoped scan API.
- Readiness tests prove realtime job failures affect warehouse readiness.
- Focused backend tests pass.
- Warehouse/BI/audience regression passes.

## Rollout

1. Deploy the service and API change with the scheduler disabled.
2. Run `/warehouse/realtime/jobs/incidents/scan` in staging after P2-047 heartbeats are reporting.
3. Validate incident deduplication and readiness status.
4. Enable `canvas.warehouse.realtime-job-incident-scheduler.enabled` for the target tenant.
5. Configure external alert fanout in a later slice if incident rows prove useful thresholds.

## Rollback

- Disable the scheduler.
- Stop calling the scan endpoint.
- P2-047 job heartbeat, action audit, and status APIs remain available.
- Existing incidents remain auditable and can be acknowledged or resolved through current incident APIs.
