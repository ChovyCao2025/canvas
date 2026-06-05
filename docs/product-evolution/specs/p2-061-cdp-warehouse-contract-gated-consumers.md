# P2-061 - CDP Warehouse Contract Gated Consumers Spec

Priority: P2
Sequence: 061
Source: `docs/product-evolution/specs/p2-060-cdp-warehouse-consumer-availability-contracts.md`, `docs/product-evolution/specs/p2-057-cdp-warehouse-bi-query-availability-gate.md`, `docs/product-evolution/specs/p2-056-cdp-warehouse-audience-materialization-availability-gate.md`
Implementation plan: `../plans/p2-061-cdp-warehouse-contract-gated-consumers-plan.md`

## Goal

Wire P2-060 consumer availability contracts into BI query execution and manual audience materialization so downstream consumers can block execution on table, dataset, or metric availability evidence instead of only the coarse warehouse window-level gate.

## Current Baseline

- P2-060 stores asset availability observations and evaluates consumer contracts.
- P2-057 gates BI query execution with the P2-055 window-level availability decision.
- P2-056 gates manual audience materialization with the P2-055 window-level availability decision.
- BI and audience paths do not yet consume P2-060 contract decisions.

## In Scope

- Add contract-gated BI query execution in `BiQueryExecutionService`.
- Add a BI API endpoint for contract-gated query execution.
- Add contract-gated manual audience materialization in `AudienceMaterializationOperationsService`.
- Add an audience API endpoint for contract-gated materialization.
- Block execution when the contract evaluation is not allowed.
- Preserve existing P2-056 and P2-057 window-level gated endpoints.
- Add focused service and controller tests.

## Out Of Scope

- Replacing existing `execute-gated` or `materialize-gated` endpoints.
- Scheduled audience refresh contract binding.
- BI dashboard/subscription scheduler contract gates.
- UI.
- Automatic contract discovery.

## Runtime Semantics

1. Callers provide a `contractKey` and requested `[from, to]` window.
2. The service evaluates the contract through `CdpWarehouseConsumerAvailabilityService`.
3. If the contract evaluation returns `allowed=false`, execution is blocked.
4. Blocked BI queries write a `BLOCKED` query history row and do not call the datasource executor.
5. Blocked audience materialization does not call the materialization service.
6. If the contract evaluation is allowed, the existing BI or audience execution path is used.
7. The contract policy, not the request, decides whether WARN is allowed.

## Functional Requirements

1. BI consumers can call a contract-gated query endpoint.
2. Audience operators can call a contract-gated materialization endpoint.
3. Contract FAIL always blocks.
4. Contract WARN blocks or allows according to the stored `gatePolicy`.
5. Existing window-level gated endpoints remain compatible.

## Technical Scope

- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/BiQueryExecutionService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiQueryController.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/analytics/AudienceMaterializationOperationsService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseAudienceMaterializationController.java`
- Tests under `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/query/`, `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/analytics/`, and `backend/canvas-engine/src/test/java/org/chovy/canvas/web/`.

## Acceptance Criteria

- P2-061 spec and plan are indexed.
- No Flyway migration is added.
- BI contract-gated execution blocks when the contract evaluation is not allowed and records a `BLOCKED` history row.
- BI contract-gated execution runs the existing query path when allowed.
- Audience contract-gated materialization blocks without invoking materialization when not allowed.
- Audience contract-gated materialization invokes the existing materialization path when allowed.
- Focused backend tests pass.
- Warehouse/BI/audience regression passes.

## Rollout

1. Deploy the additive code path.
2. Register consumer contracts for critical BI metrics and audience definitions.
3. Route staging BI and manual audience operations through contract-gated endpoints.
4. Compare block decisions with existing window-level gated endpoints.
5. Later slices can wire scheduled refreshes, subscriptions, and dashboards to contract keys.

## Rollback

- Stop calling the contract-gated endpoints.
- Existing ungated and window-level gated endpoints continue independently.
