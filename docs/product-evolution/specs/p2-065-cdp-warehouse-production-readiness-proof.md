# P2-065 - CDP Warehouse Production Readiness Proof Spec

Priority: P2
Sequence: 065
Source: `docs/product-evolution/specs/p2-036-cdp-warehouse-readiness-and-slo-summary.md`, `docs/product-evolution/specs/p2-055-cdp-warehouse-data-availability-gates.md`, `docs/product-evolution/specs/p2-060-cdp-warehouse-consumer-availability-contracts.md`, `docs/product-evolution/specs/p2-061-cdp-warehouse-contract-gated-consumers.md`, `docs/product-evolution/specs/p2-064-cdp-warehouse-scheduled-audience-contract-gates.md`
Implementation plan: `../plans/p2-065-cdp-warehouse-production-readiness-proof-plan.md`

Status: Historical plan evidence records implementation and verification; runtime verification plus commit and merge status was not verified in this docs-only audit.

## Goal

Add a tenant-scoped production readiness proof that combines warehouse readiness, offline/realtime window availability, and critical consumer availability contract evaluations into one operator-facing verdict before production OLAP/CDP use.

## Current Baseline

- P2-036 exposes broad warehouse readiness across offline, realtime, incidents, BI, and audience materialization.
- P2-055 evaluates window-level offline/realtime availability.
- P2-060 stores and evaluates consumer availability contracts.
- P2-061 and P2-064 wire those contracts into BI and audience consumers.
- Operators still need to query several APIs to prove that a specific production window is safe for CDP/OLAP consumption.

## In Scope

- Add a read-only production readiness proof service.
- Add a `/warehouse/production-readiness` API for current tenant.
- Accept `from`, `to`, `mode`, and repeated `contractKey` parameters.
- Include readiness summary evidence from P2-036.
- Include window availability evidence from P2-055.
- Evaluate each requested consumer contract through P2-060.
- Mark missing `contractKey` input as WARN, not PASS.
- Fail closed when contract evaluation is requested but consumer availability service is unavailable.
- Return a compact evidence list with `PASS`, `WARN`, or `FAIL` statuses.
- Add focused service and controller tests.

## Out Of Scope

- New Flyway schema.
- Real Doris/Flink/Kafka integration execution.
- Writing persistent proof history.
- Automatically discovering critical contract keys.
- Replacing existing readiness, availability, or contract APIs.
- UI.

## Runtime Semantics

1. Default `mode` is `HYBRID`.
2. Default `to` is now and default `from` is one hour before `to`.
3. The service first reads P2-036 readiness.
4. It then evaluates P2-055 window availability for the same window and mode.
5. If no contract keys are provided, the report includes a `consumer_contracts` WARN evidence row.
6. If contract keys are provided, each one is evaluated with P2-060 against the same window.
7. A contract evaluation with `allowed=false` produces FAIL evidence even if the raw contract status is WARN.
8. A contract evaluation with `allowed=true` keeps the raw PASS or WARN status.
9. Any exception while reading readiness, availability, or requested contract evidence produces FAIL evidence for that section.
10. Overall report status is the worst evidence status: FAIL beats WARN beats PASS.

## Functional Requirements

1. Operators can request one production readiness proof for a bounded warehouse window.
2. The report includes readiness, availability, and consumer contract proof evidence.
3. The report returns WARN when no consumer contracts are requested.
4. The report returns FAIL when readiness or availability fails.
5. The report returns FAIL when any requested consumer contract blocks.
6. The API uses the current tenant context and falls back to tenant `0` when no resolver is configured.
7. Existing readiness, availability, and contract APIs continue unchanged.

## Technical Scope

- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseProductionReadinessProofService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseProductionReadinessController.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseProductionReadinessProofServiceTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/web/CdpWarehouseProductionReadinessControllerTest.java`
- `docs/product-evolution/IMPLEMENTATION_ORDER.md`
- `docs/product-evolution/specs/INDEX.md`
- `docs/product-evolution/plans/INDEX.md`

## Acceptance Criteria

- P2-065 spec and plan are indexed.
- No Flyway migration is added.
- Service tests prove PASS when readiness, availability, and requested contracts pass.
- Service tests prove WARN when no contract keys are provided.
- Service tests prove FAIL when a requested contract is blocked.
- Service tests prove missing consumer availability service fails closed for requested contracts.
- Controller tests prove current tenant scoping and request parameter binding.
- Focused backend tests pass.
- Warehouse/BI/audience regression passes.

## Rollout

1. Deploy the read-only API.
2. Use staging to call `/warehouse/production-readiness` with critical BI and audience contract keys for representative windows.
3. Keep production promotion blocked unless the report is PASS or a WARN has an explicit operator exception.
4. Compare proof output with P2-036 readiness, P2-055 availability, and P2-063 incident output.

## Rollback

- Stop calling `/warehouse/production-readiness`.
- Existing readiness, availability, and consumer contract APIs remain available independently.
