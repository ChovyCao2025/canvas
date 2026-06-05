# P2-056 - CDP Warehouse Audience Materialization Availability Gate Spec

Priority: P2
Sequence: 056
Source: `docs/product-evolution/specs/p2-021-cdp-olap-audience-materialization.md`, `docs/product-evolution/specs/p2-035-cdp-warehouse-audience-materialization-operations.md`, `docs/product-evolution/specs/p2-055-cdp-warehouse-data-availability-gates.md`
Implementation plan: `../plans/p2-056-cdp-warehouse-audience-materialization-availability-gate-plan.md`

## Goal

Wire warehouse data availability gates into audience materialization operations so operators can block OLAP audience refreshes when the requested warehouse window is not safe to consume.

## Current Baseline

- P2-021 and P2-035 provide audience materialization and operations APIs.
- P2-045 adds scheduled refresh and rollback.
- P2-055 exposes window-level offline/realtime availability decisions.
- Audience materialization can still be triggered without checking whether the required warehouse window is available.

## In Scope

- Add a gated materialization operation that evaluates warehouse availability before invoking the materialization engine.
- Support `OFFLINE`, `REALTIME`, and `HYBRID` availability modes.
- Block materialization on availability `FAIL`.
- Block availability `WARN` unless the operator explicitly allows warning gates.
- Return availability evidence and materialization result in one response.
- Add a tenant-scoped API endpoint and focused tests.

## Out Of Scope

- Changing the existing manual `/materialize` endpoint behavior.
- Changing scheduled refresh behavior.
- Inferring audience-specific event windows from rule definitions.
- New Flyway tables.
- UI.

## Runtime Semantics

1. The gated operation is tenant scoped.
2. Missing `from` and `to` use the availability service defaults.
3. Availability `PASS` triggers materialization.
4. Availability `WARN` triggers materialization only when `allowWarn` is true.
5. Availability `FAIL` returns `BLOCKED` and does not invoke materialization.
6. If the availability service is not configured, the gated operation fails closed.
7. Existing materialization, rollback, recent-run, and refresh-due APIs continue to work.

## Functional Requirements

1. Operators can call a gated materialization endpoint with window and mode.
2. Operators receive the availability decision that allowed or blocked the run.
3. The response distinguishes `TRIGGERED` from `BLOCKED`.
4. Existing non-gated materialization remains available for emergency manual use.
5. Focused tests prove PASS, WARN, FAIL, and controller tenant behavior.

## Technical Scope

- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/analytics/AudienceMaterializationOperationsService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseAudienceMaterializationController.java`
- Tests under `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/analytics/` and `backend/canvas-engine/src/test/java/org/chovy/canvas/web/`.

## Acceptance Criteria

- P2-056 spec and plan are indexed.
- No Flyway migration is added.
- Gated materialization invokes the materialization engine on PASS.
- Gated materialization blocks FAIL.
- Gated materialization blocks WARN unless `allowWarn=true`.
- Controller endpoint is tenant scoped.
- Focused backend tests pass.
- Warehouse/BI/audience regression passes.

## Rollout

1. Deploy the gated endpoint alongside the existing manual endpoint.
2. Use the gated endpoint in staging for audience refreshes that depend on warehouse freshness.
3. Compare blocked decisions with P2-055 availability responses.
4. Later slices can move scheduled refresh to the gated path after operational evidence is trusted.

## Rollback

- Stop calling the gated endpoint.
- Existing materialization operations continue through the original endpoint.
