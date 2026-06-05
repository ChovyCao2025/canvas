# P2-058 - CDP Warehouse Scheduled Audience Availability Gate Spec

Priority: P2
Sequence: 058
Source: `docs/product-evolution/specs/p2-045-cdp-warehouse-audience-materialization-scheduler-and-rollback.md`, `docs/product-evolution/specs/p2-055-cdp-warehouse-data-availability-gates.md`, `docs/product-evolution/specs/p2-056-cdp-warehouse-audience-materialization-availability-gate.md`
Implementation plan: `../plans/p2-058-cdp-warehouse-scheduled-audience-availability-gate-plan.md`

## Goal

Wire warehouse data availability gates into scheduled audience materialization so unattended OLAP audience refreshes do not consume stale or unavailable offline/realtime warehouse windows.

## Current Baseline

- P2-045 adds scheduled audience materialization and rollback.
- P2-055 exposes tenant-scoped offline, realtime, and hybrid availability decisions.
- P2-056 gates manual audience materialization but explicitly leaves scheduled refresh behavior unchanged.
- The scheduler can still refresh all due audiences without checking warehouse availability first.

## In Scope

- Add a gated scheduled refresh operation above the existing `refreshDue` path.
- Support `OFFLINE`, `REALTIME`, and `HYBRID` availability modes.
- Block the scheduled refresh batch on availability `FAIL`.
- Block availability `WARN` unless the caller or scheduler allows warning gates.
- Return availability evidence and the scheduled refresh result in one response.
- Add a tenant-scoped API endpoint for gated refresh-due execution.
- Let the scheduled job use the gated path through configuration without removing the original ungated method.

## Out Of Scope

- Changing the existing `/warehouse/audiences/materialization/refresh-due` endpoint behavior.
- Inferring audience-specific windows or modes from rule definitions.
- Per-audience partial gating.
- New Flyway tables.
- UI.

## Runtime Semantics

1. The gated scheduled refresh is tenant scoped.
2. Missing `from` and `to` use the availability service defaults.
3. Availability `PASS` invokes the existing `refreshDue` operation.
4. Availability `WARN` invokes `refreshDue` only when `allowWarn` is true.
5. Availability `FAIL` returns `BLOCKED` and does not scan or materialize due audiences.
6. If the availability service is not configured, the gated operation fails closed.
7. Existing manual refresh-due, manual materialization, rollback, and recent-run APIs continue to work.

## Functional Requirements

1. Operators can call a gated refresh-due endpoint with window, mode, allowWarn, limit, and operator.
2. Operators receive the availability decision that allowed or blocked the scheduled batch.
3. The response distinguishes `EXECUTED` from `BLOCKED`.
4. Blocked gated refreshes do not call the definition scanner or materialization service.
5. The scheduled job can be configured to call the gated path.
6. Focused tests prove PASS, WARN, FAIL, scheduler delegation, and controller tenant behavior.

## Technical Scope

- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/analytics/AudienceMaterializationScheduleService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/analytics/CdpWarehouseAudienceMaterializationScheduler.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseAudienceMaterializationController.java`
- Tests under `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/analytics/` and `backend/canvas-engine/src/test/java/org/chovy/canvas/web/`.

## Acceptance Criteria

- P2-058 spec and plan are indexed.
- No Flyway migration is added.
- Gated scheduled refresh invokes `refreshDue` on PASS.
- Gated scheduled refresh blocks FAIL.
- Gated scheduled refresh blocks WARN unless `allowWarn=true`.
- Configured scheduler delegates to the gated path.
- Controller endpoint is tenant scoped.
- Focused backend tests pass.
- Warehouse/BI/audience regression passes.

## Rollout

1. Deploy the gated service and endpoint alongside the existing refresh-due endpoint.
2. Enable scheduler gating in staging with `HYBRID` mode and `allowWarn=false`.
3. Compare blocked batches with P2-055 availability and manual materialization behavior.
4. Enable the gated scheduler path for production tenants after scheduler evidence is stable.

## Rollback

- Disable scheduler availability gating or stop calling the gated endpoint.
- Existing ungated refresh-due and materialization operations remain available.
