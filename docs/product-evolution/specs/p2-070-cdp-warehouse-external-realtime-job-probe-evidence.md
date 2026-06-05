# P2-070 - CDP Warehouse External Realtime Job Probe Evidence Spec

Priority: P2
Sequence: 070
Source: `docs/product-evolution/specs/p2-047-cdp-warehouse-realtime-job-control-plane.md`, `docs/product-evolution/specs/p2-048-cdp-warehouse-realtime-job-incident-automation.md`, `docs/product-evolution/specs/p2-069-cdp-warehouse-realtime-physical-e2e-certification.md`
Implementation plan: `../plans/p2-070-cdp-warehouse-external-realtime-job-probe-evidence-plan.md`

## Goal

Add external realtime job probes so Flink REST, Kafka Connect REST, and Doris routine-load style health evidence can automatically feed the existing P2-047 realtime job heartbeat control plane. This lets P2-069 physical E2E certification consume real external runtime state instead of relying only on manually reported job heartbeats.

## Current Baseline

- P2-047 stores realtime job instance heartbeat state and action audit.
- P2-048 turns stale or failed realtime job heartbeat state into incidents.
- P2-069 requires realtime pipeline and realtime job status in physical E2E certification.
- External Flink/Kafka Connect/Doris routine-load health polling is still explicitly out of scope for P2-069.
- Therefore a certification can be realtime-required while the job state still depends on another actor actively pushing heartbeats into canvas-engine.

## In Scope

- Add tenant-scoped external realtime job probe targets.
- Support provider-neutral engine types, with built-in mappers for:
  - `FLINK_REST`
  - `KAFKA_CONNECT`
  - `DORIS_ROUTINE_LOAD`
  - `GENERIC_HTTP`
- Probe target registration, list, enable/disable, and manual scan API.
- Scheduled scans protected by the existing warehouse lease service when available.
- Map external probe results into `CdpWarehouseRealtimeJobControlService.HeartbeatCommand`.
- Store probe response metadata in heartbeat payload JSON.
- Treat probe failures as heartbeat evidence with `runtimeStatus=FAILED`, so existing incident, readiness, and certification layers can fail closed.
- Add focused tests without requiring real Flink, Kafka Connect, or Doris.

## Out Of Scope

- Deploying or managing Flink, Kafka, Kafka Connect, Doris routine-load, RocketMQ, or Kubernetes jobs.
- Executing pause, resume, or restart against external engines.
- Reading secrets directly; `authRef` is a placeholder for a later secret-resolution slice.
- Replacing P2-032 checkpoint reporting or P2-047 heartbeat/action APIs.
- UI.

## Runtime Semantics

1. A probe target is tenant scoped and uniquely identified by `(tenant_id, pipeline_key, job_key)`.
2. A target defines engine type, endpoint URL, external job identifier, optional connector name, owner, and max staleness.
3. Disabled targets are never scanned.
4. A manual scan can run one target or all enabled targets for the current tenant.
5. The scheduler scans enabled targets for the configured tenant when enabled.
6. If `CdpWarehouseJobLeaseService` is present, scheduled scans acquire a tenant-scoped lease before scanning.
7. A successful external probe maps to `RUNNING`, `PAUSED`, `STOPPED`, or `FAILED`.
8. Unknown or degraded external states map to `FAILED` when the provider reports failure, otherwise `RUNNING` with payload metadata.
9. Network, parse, timeout, and provider errors map to a failed heartbeat instead of being silently ignored.
10. Heartbeats written by probes keep P2-047 health evaluation, P2-048 incident automation, and P2-069 certification semantics unchanged.

## Functional Requirements

1. Operators can register or update an external realtime job probe target.
2. Operators can list probe targets for the current tenant.
3. Operators can enable or disable a probe target.
4. Operators can manually scan all enabled targets or one target.
5. Scheduled scans can run with bounded target limits.
6. Probe scans write heartbeat evidence through P2-047.
7. Failed probes write failed heartbeat evidence with bounded error messages.
8. Flink REST job states are mapped to runtime status.
9. Kafka Connect connector/task states are mapped to runtime status.
10. Doris routine-load states are mapped to runtime status.

## Technical Scope

- Add migration `backend/canvas-engine/src/main/resources/db/migration/V249__cdp_warehouse_external_realtime_job_probe.sql`
- Add `CdpWarehouseExternalRealtimeJobProbeTargetDO`
- Add `CdpWarehouseExternalRealtimeJobProbeTargetMapper`
- Add `CdpWarehouseExternalRealtimeJobProbeClient`
- Add `HttpCdpWarehouseExternalRealtimeJobProbeClient`
- Add `CdpWarehouseExternalRealtimeJobProbeService`
- Add `CdpWarehouseExternalRealtimeJobProbeScheduler`
- Add `CdpWarehouseExternalRealtimeJobProbeController`
- Add focused schema, service, scheduler, and controller tests.

## Acceptance Criteria

- P2-070 spec and plan are indexed.
- Migration test proves the probe target table and unique key exist.
- Service tests prove a passing external probe writes a running heartbeat.
- Service tests prove a failed external probe writes a failed heartbeat.
- Service tests prove disabled targets are not scanned.
- Scheduler tests prove disabled scheduler no-ops and enabled scheduler delegates with lease protection when present.
- Controller tests prove target registration, listing, enable/disable, and manual scan request binding.
- Focused backend tests pass.
- Main compile passes.

## Rollout

1. Deploy migration and code with the scheduler disabled.
2. Register staging probe targets for one Flink/Kafka Connect/Doris routine-load job.
3. Run manual scans and inspect `/warehouse/realtime/jobs/status`.
4. Confirm P2-048 incidents and P2-069 certification react to the externally sourced heartbeat evidence.
5. Enable scheduled scans with conservative target limits and heartbeat staleness thresholds.

## Rollback

- Disable the external realtime job probe scheduler.
- Disable individual probe targets.
- Existing P2-047 manual heartbeat and action APIs remain available.
