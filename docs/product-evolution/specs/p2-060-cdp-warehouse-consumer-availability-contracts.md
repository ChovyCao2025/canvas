# P2-060 - CDP Warehouse Consumer Availability Contracts Spec

Priority: P2
Sequence: 060
Source: `docs/product-evolution/specs/p2-055-cdp-warehouse-data-availability-gates.md`, `docs/product-evolution/specs/p2-040-cdp-warehouse-semantic-metric-contracts.md`, `docs/product-evolution/specs/p2-045-cdp-warehouse-audience-materialization-scheduler-and-rollback.md`
Implementation plan: `../plans/p2-060-cdp-warehouse-consumer-availability-contracts-plan.md`

Status: Historical plan evidence records implementation and verification; runtime verification plus commit and merge status was not verified in this docs-only audit.

## Goal

Add table, dataset, and metric availability contracts for downstream warehouse consumers so BI queries, semantic metrics, audience materialization, and future schedulers can declare the warehouse assets they depend on and receive a deterministic allow/block decision for a requested data window.

## Current Baseline

- P2-055 evaluates tenant-scoped warehouse availability at window level for `OFFLINE`, `REALTIME`, and `HYBRID`.
- P2-056 through P2-058 gate audience and BI operations with the P2-055 decision.
- P2-027, P2-040, and P2-041 expose catalog, metric contracts, and metric impact.
- P2-055 explicitly leaves table-level or metric-level availability calendars out of scope.
- Downstream consumers still cannot persist their required warehouse assets or evaluate asset-specific availability before running.

## In Scope

- Persist asset availability observations for `TABLE`, `DATASET`, and `METRIC` assets.
- Persist consumer availability contracts with consumer type/ref, required mode, required asset list, and gate policy.
- Evaluate a consumer contract by combining the P2-055 window-level decision with asset-specific availability calendar evidence.
- Return per-asset status, available-until timestamp, lag minutes, evidence source, and an overall allow/block decision.
- Add tenant-scoped APIs for asset observation upsert/list, contract upsert/list, and contract evaluation.
- Add focused schema, service, and controller tests.

## Out Of Scope

- Replacing existing BI or audience gated execution endpoints.
- Automatically discovering every BI/dashboard/audience dependency.
- UI.
- Cross-tenant fanout.
- Starting, stopping, or deploying external warehouse jobs.

## Runtime Semantics

1. Asset observations are tenant scoped. Tenant `0` may hold built-in shared evidence.
2. A consumer contract is tenant scoped by `contractKey`.
3. A contract can declare assets directly through `requiredAssets`.
4. `requiredMode` defaults to `HYBRID`.
5. `gatePolicy` can be `BLOCK_ON_FAIL` or `BLOCK_ON_WARN`.
6. Contract evaluation first calls the existing window-level availability service.
7. Each declared asset must have latest active evidence for the requested mode, or the asset gate fails.
8. An asset gate passes when evidence status is `PASS` and `availableUntil >= requestedTo`.
9. An asset gate warns when evidence status is `WARN`, or when evidence is past the requested window but within `warnToleranceMinutes`.
10. An asset gate fails when evidence is missing, status is `FAIL`, or lag exceeds `warnToleranceMinutes`.
11. Overall status is the worst status across the window-level decision and asset gates.
12. `allowed` is true only for `PASS`, or for `WARN` when `gatePolicy=BLOCK_ON_FAIL`.

## Functional Requirements

1. Operators can register and list table/dataset/metric availability observations.
2. Operators can register and list consumer availability contracts.
3. Consumers can evaluate a contract for a requested `[from, to]` window.
4. Missing asset evidence blocks the contract.
5. Window-level warehouse availability remains part of the final decision.
6. Existing P2-055 availability APIs and downstream gates continue to work.

## Technical Scope

- `backend/canvas-engine/src/main/resources/db/migration/V213__cdp_warehouse_consumer_availability_contracts.sql`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpWarehouseAssetAvailabilityDO.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpWarehouseConsumerAvailabilityContractDO.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpWarehouseAssetAvailabilityMapper.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpWarehouseConsumerAvailabilityContractMapper.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseConsumerAvailabilityService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseAvailabilityController.java`
- Tests under `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/` and `backend/canvas-engine/src/test/java/org/chovy/canvas/web/`.

## Acceptance Criteria

- P2-060 spec and plan are indexed.
- Migration creates asset availability and consumer contract tables with tenant-scoped unique keys and useful query indexes.
- Service tests prove asset upsert/list, contract upsert/list, missing asset evidence blocking, WARN allow/block policy, and window-level FAIL blocking.
- Controller tests prove tenant-scoped delegation for the new APIs.
- Focused backend tests pass.
- Warehouse/BI/audience regression passes.

## Rollout

1. Deploy the additive migration.
2. Register critical BI and audience consumers with explicit table/metric dependencies.
3. Let offline/realtime jobs write asset availability observations after successful loads.
4. Evaluate contracts in staging before wiring them into hard execution gates.
5. Later slices can connect contracts directly to BI query execution and audience refresh flows.

## Rollback

- Stop calling the contract evaluation endpoints.
- Existing P2-055 availability and gated BI/audience paths continue independently.
