# P2-059 - CDP Warehouse Availability Incident Automation Spec

Priority: P2
Sequence: 059
Source: `docs/product-evolution/specs/p2-030-cdp-warehouse-quality-incident-loop.md`, `docs/product-evolution/specs/p2-055-cdp-warehouse-data-availability-gates.md`, `docs/product-evolution/specs/p2-057-cdp-warehouse-bi-query-availability-gate.md`, `docs/product-evolution/specs/p2-058-cdp-warehouse-scheduled-audience-availability-gate.md`
Implementation plan: `../plans/p2-059-cdp-warehouse-availability-incident-automation-plan.md`

## Goal

Route warehouse availability gate WARN/FAIL evidence into the existing warehouse incident loop, and automatically resolve matching availability incidents when later scans pass.

## Current Baseline

- P2-055 exposes tenant-scoped offline/realtime/hybrid availability decisions.
- P2-056, P2-057, and P2-058 gate audience materialization, BI queries, and scheduled audience refreshes.
- Existing warehouse incidents cover quality checks, readiness, realtime pipelines/jobs, and table drift.
- Availability gate failures can block downstream consumers but are not yet visible as durable operator incidents.

## In Scope

- Add availability-specific incident recording in `CdpWarehouseIncidentService`.
- Add availability-specific incident auto-resolution by stable availability gate key.
- Add an availability incident scan service that evaluates a requested window and opens/resolves incidents for each gate.
- Add a tenant-scoped scan API.
- Add a disabled-by-default scheduled scanner with warehouse lease protection.
- Add focused service, scheduler, controller, and incident tests.

## Out Of Scope

- Changing P2-055 availability evaluation semantics.
- Changing BI/audience gate behavior.
- New Flyway tables.
- External paging/alert channels.
- UI.

## Runtime Semantics

1. The scan is tenant scoped and evaluates one requested availability mode/window.
2. Each availability gate maps to a stable incident key: `AVAILABILITY:{MODE}:{GATE_KEY}`.
3. Gate `WARN` opens or refreshes a warning incident.
4. Gate `FAIL` opens or refreshes a critical incident.
5. Gate `PASS` resolves the matching open or acknowledged availability incident, if one exists.
6. Scan failures are counted in the scan response and do not abort remaining gates.
7. The scheduler is disabled by default and protected by `CdpWarehouseJobLeaseService` when available.

## Functional Requirements

1. Operators can scan availability incidents for `OFFLINE`, `REALTIME`, or `HYBRID` windows.
2. WARN/FAIL gates become warehouse incidents with source type `WAREHOUSE_AVAILABILITY`.
3. PASS gates resolve matching availability incidents.
4. The scan response reports total gates, opened, resolved, skipped, and failed counts.
5. The scheduler can periodically scan a configured mode and rolling window.
6. Existing availability, readiness, and downstream gate APIs continue to work.

## Technical Scope

- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpWarehouseIncidentMapper.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseIncidentService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseAvailabilityIncidentService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseAvailabilityIncidentScheduler.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseAvailabilityIncidentController.java`
- Tests under `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/` and `backend/canvas-engine/src/test/java/org/chovy/canvas/web/`.

## Acceptance Criteria

- P2-059 spec and plan are indexed.
- No Flyway migration is added.
- Availability WARN opens a warning incident.
- Availability FAIL opens a critical incident.
- Availability PASS resolves a matching availability incident.
- Manual scan API is tenant scoped.
- Scheduler is disabled by default and lease protected when configured.
- Focused backend tests pass.
- Warehouse/BI/audience regression passes.

## Rollout

1. Deploy manual scan API and run it in staging for `HYBRID` mode.
2. Compare availability incidents with P2-055 availability responses and gated consumer blocks.
3. Enable the scheduled scanner in staging with a conservative rolling window.
4. Enable production scanning per tenant after incident volume is understood.

## Rollback

- Disable the scheduled scanner and stop calling the scan API.
- Existing availability decisions and downstream gated operations continue to work.
