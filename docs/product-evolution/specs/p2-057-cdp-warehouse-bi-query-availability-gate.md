# P2-057 - CDP Warehouse BI Query Availability Gate Spec

Priority: P2
Sequence: 057
Source: `docs/product-evolution/specs/p2-023-bi-dataset-query-compiler-foundation.md`, `docs/product-evolution/specs/p2-034-cdp-warehouse-field-governance-and-bi-policy.md`, `docs/product-evolution/specs/p2-055-cdp-warehouse-data-availability-gates.md`
Implementation plan: `../plans/p2-057-cdp-warehouse-bi-query-availability-gate-plan.md`

## Goal

Wire warehouse data availability gates into BI query execution so operators can block report queries when the requested offline, realtime, or hybrid warehouse window is not safe to consume.

## Current Baseline

- P2-023 provides safe BI dataset compilation and execution.
- P2-034 enforces warehouse field governance before BI compile and execute.
- P2-055 exposes tenant-scoped window-level warehouse availability.
- P2-056 gates audience materialization, but BI query execution can still read stale or unavailable warehouse data without checking availability.

## In Scope

- Add a gated BI query execution operation that evaluates warehouse availability before hitting the datasource.
- Support `OFFLINE`, `REALTIME`, and `HYBRID` availability modes.
- Block query execution on availability `FAIL`.
- Block availability `WARN` unless the operator explicitly allows warning gates.
- Return availability evidence and query result in one response.
- Record blocked gated queries in BI query history for operator audit.
- Add a tenant-scoped API endpoint and focused tests.

## Out Of Scope

- Changing the existing `/canvas/bi/query/execute` endpoint behavior.
- Inferring query time windows from arbitrary BI filters.
- Changing field governance, row permission, cache, or SQL compilation semantics.
- New Flyway tables.
- UI.

## Runtime Semantics

1. The gated operation is tenant scoped.
2. The caller supplies the BI query plus optional `from`, `to`, `mode`, and `allowWarn`.
3. Missing `from` and `to` use the availability service defaults.
4. Availability `PASS` executes the query through the existing BI execution path.
5. Availability `WARN` executes only when `allowWarn` is true.
6. Availability `FAIL` returns `BLOCKED`, records a BI history row, and does not compile or execute datasource SQL.
7. If the availability service is not configured, the gated operation fails closed.
8. Existing BI compile, execute, history, datasource health, and embed endpoints continue to work.

## Functional Requirements

1. Operators can call a gated BI query endpoint with query, window, mode, and warning override.
2. Operators receive the availability decision that allowed or blocked the query.
3. The response distinguishes `EXECUTED` from `BLOCKED`.
4. Blocked gated queries do not call the BI datasource.
5. Blocked gated queries create query-history evidence.
6. Existing non-gated BI execution remains available for emergency/manual use.
7. Focused tests prove PASS, WARN, FAIL, history, and controller tenant behavior.

## Technical Scope

- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/BiQueryExecutionService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiQueryController.java`
- Tests under `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/query/` and `backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi/`.

## Acceptance Criteria

- P2-057 spec and plan are indexed.
- No Flyway migration is added.
- Gated BI execution invokes the existing query execution path on PASS.
- Gated BI execution blocks FAIL.
- Gated BI execution blocks WARN unless `allowWarn=true`.
- Blocked gated BI queries record `BLOCKED` history.
- Controller endpoint is tenant scoped.
- Focused backend tests pass.
- Warehouse/BI/audience regression passes.

## Rollout

1. Deploy the gated endpoint alongside the existing BI execute endpoint.
2. Use gated execution in staging dashboards that depend on warehouse freshness.
3. Compare blocked decisions with P2-055 availability responses and actual report freshness.
4. Move default dashboard/report callers to the gated endpoint after operational evidence is trusted.

## Rollback

- Stop calling the gated endpoint.
- Existing BI query execution continues through the original endpoint.
