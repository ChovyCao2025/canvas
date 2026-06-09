# P2-047 - CDP Warehouse Realtime Job Control Plane Spec

Priority: P2
Sequence: 047
Source: `docs/product-evolution/specs/p2-032-cdp-warehouse-realtime-pipeline-runtime.md`, `docs/product-evolution/specs/p2-033-cdp-warehouse-realtime-pipeline-incidents.md`, `docs/product-evolution/specs/p2-046-cdp-warehouse-realtime-schema-evolution-guard.md`
Implementation plan: `../plans/p2-047-cdp-warehouse-realtime-job-control-plane-plan.md`

Status: Historical plan evidence records implementation and verification; runtime verification plus commit and merge status was not verified in this docs-only audit.

## Goal

Add a production control plane for external realtime warehouse jobs so Flink CDC, Flink SQL, Kafka Connect, and Doris routine-load style jobs can report heartbeats, expose stale or failed runtime state, and receive auditable pause, resume, and restart requests.

## Current Baseline

- P2-032 stores realtime pipeline contracts and checkpoint runtime evidence.
- P2-033 turns realtime pipeline WARN/FAIL status into warehouse incidents.
- P2-046 adds schema-version evidence and schema compatibility guards.
- There is still no durable model for concrete external job instances, job heartbeat freshness, desired runtime state, or operator action audit.

## In Scope

- Add realtime job instance registry and heartbeat ledger state.
- Add action audit rows for `PAUSE`, `RESUME`, and `RESTART`.
- Add status evaluation for stale heartbeat, failed runtime, and desired-state mismatch.
- Add APIs for heartbeat reporting, status listing, action requests, pending action polling, acknowledgement, and completion.
- Keep this as a control plane: external jobs remain responsible for executing the requested action.
- Add focused schema, service, and controller tests.

## Out Of Scope

- Starting or stopping a real Flink cluster.
- Calling Flink REST, Kafka Connect REST, or Kubernetes APIs.
- Replacing P2-032 checkpoint reporting.
- New incident routing for job actions.
- UI.

## Runtime Semantics

1. A job instance is scoped by tenant, pipeline key, and job key.
2. Heartbeat upserts the job instance and records runtime status, engine metadata, deployment reference, heartbeat payload, error message, and owner.
3. Runtime status is normalized to upper case.
4. `FAILED` runtime status evaluates to `FAIL`.
5. Missing or stale heartbeat evaluates to `WARN`.
6. Desired/runtime mismatch evaluates to `WARN`.
7. Healthy non-stale jobs evaluate to `PASS`.
8. Operator action requests create a `PENDING` action row and update the job instance desired status.
9. `PAUSE` sets desired status to `PAUSED`; `RESUME` and `RESTART` set desired status to `RUNNING`.
10. External jobs poll pending actions and report acknowledgement or completion.

## Functional Requirements

1. External jobs can report heartbeat under the current tenant.
2. Operators can view job status by tenant and optionally by pipeline key.
3. Operators can request `PAUSE`, `RESUME`, or `RESTART` with reason and requester.
4. Job action rows store requested, acknowledged, and completed timestamps.
5. External jobs can list pending actions for their pipeline/job key.
6. External jobs can acknowledge and complete actions without deleting audit evidence.
7. Tests prove tenant scoping, stale/failure/diff status evaluation, action lifecycle, and controller behavior.

## Technical Scope

- `backend/canvas-engine/src/main/resources/db/migration/V206__cdp_warehouse_realtime_job_control.sql`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpWarehouseStreamJobInstanceDO.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpWarehouseStreamJobActionDO.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpWarehouseStreamJobInstanceMapper.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpWarehouseStreamJobActionMapper.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseRealtimeJobControlService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseRealtimeJobController.java`
- Focused tests for schema, service, and controller.

## Acceptance Criteria

- P2-047 spec and plan are indexed.
- Migration is additive and does not edit old Flyway migrations.
- Heartbeat upserts job instance runtime state.
- Status API detects PASS, WARN, and FAIL states.
- Action request, pending action polling, acknowledgement, and completion are tenant scoped and auditable.
- Focused backend tests pass.
- Warehouse/BI/audience regression passes.

## Rollout

1. Deploy migration and API with no external job changes.
2. Configure staging realtime jobs to report heartbeats.
3. Use status API to validate heartbeat freshness and runtime state.
4. Let jobs poll pending actions before wiring real pause/resume/restart behavior.
5. Use action audit rows as production change evidence.

## Rollback

- Stop external heartbeat and action polling.
- Existing pipeline checkpoint, schema guard, and incident flow remain available.
- Job action audit rows remain for historical evidence.
