# P2-038 - CDP Warehouse Readiness Incident Scheduler Spec

Priority: P2
Sequence: 038
Source: `docs/product-evolution/specs/p2-037-cdp-warehouse-readiness-incident-automation.md`, `docs/product-evolution/specs/p2-028-cdp-warehouse-scheduler-lease.md`
Implementation plan: `../plans/p2-038-cdp-warehouse-readiness-incident-scheduler-plan.md`

## Goal

Add a guarded scheduler that can automatically run readiness incident scans in production without duplicate work across service instances.

## Current Baseline

- P2-037 exposes a manual readiness incident scan API.
- Warehouse schedulers already use disabled-by-default switches, tenant scoping, distributed leases, and in-process overlap guards.
- Operators still need to remember to call the scan API or wire it manually.

## In Scope

- Add a disabled-by-default readiness incident scheduler.
- Use existing `CdpWarehouseJobLeaseService`.
- Use existing `CdpWarehouseReadinessIncidentService`.
- Add an in-process overlap guard.
- Add focused scheduler tests.

## Out Of Scope

- New schema.
- Alert routing.
- Multi-tenant fan-out.
- Runtime config UI.
- Incident auto-resolution.

## Runtime Semantics

1. Scheduler is disabled unless `canvas.warehouse.readiness-incident-scheduler.enabled=true`.
2. Scheduler runs for configured `canvas.warehouse.readiness-incident-scheduler.tenant-id`, default `0`.
3. Multi-instance execution is guarded by lease key `CDP_WAREHOUSE_READINESS_INCIDENT`.
4. In-process overlap is rejected even when a nested cycle is triggered.
5. The scheduled cycle delegates to `CdpWarehouseReadinessIncidentService.scan(tenantId)`.
6. A denied lease or disabled scheduler returns without side effects.

## Functional Requirements

1. Operators can enable scheduled readiness incident scanning with configuration.
2. The scheduler is tenant-scoped.
3. The scheduler uses the existing warehouse lease mechanism.
4. The scheduler prevents overlapping local executions.
5. Tests prove disabled, enabled, denied lease, and overlap behaviors.

## Technical Scope

- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseReadinessIncidentScheduler.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseReadinessIncidentSchedulerTest.java`

## Acceptance Criteria

- No Flyway migration is added.
- Scheduler defaults to disabled.
- Enabled scheduler delegates to readiness incident scan.
- Denied lease skips scan.
- Overlap guard prevents nested execution.
- Focused backend tests pass.
- Warehouse/BI/audience regression passes.

## Rollout

1. Deploy with scheduler disabled.
2. Enable in staging with conservative fixed delay and tenant id.
3. Enable in production after validating incident volume.

## Rollback

- Set `canvas.warehouse.readiness-incident-scheduler.enabled=false`.
- Manual scan API from P2-037 remains available.
