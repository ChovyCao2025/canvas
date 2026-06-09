# P2-066 - CDP Warehouse Physical E2E Certification Spec

Priority: P2
Sequence: 066
Source: `docs/product-evolution/specs/p2-051-cdp-warehouse-live-doris-ddl-drift.md`, `docs/product-evolution/specs/p2-055-cdp-warehouse-data-availability-gates.md`, `docs/product-evolution/specs/p2-060-cdp-warehouse-consumer-availability-contracts.md`, `docs/product-evolution/specs/p2-065-cdp-warehouse-production-readiness-proof.md`
Implementation plan: `../plans/p2-066-cdp-warehouse-physical-e2e-certification-plan.md`

Status: Historical plan evidence records implementation and verification; runtime verification plus commit and merge status was not verified in this docs-only audit.

## Goal

Add a tenant-scoped physical E2E certification API that proves the CDP/OLAP warehouse is not only logically ready, but also backed by reachable Doris infrastructure and live physical table contracts.

## Current Baseline

- P2-051 can inspect live Doris DDL for physical table drift.
- P2-055 can evaluate bounded offline/realtime availability.
- P2-060 can evaluate downstream consumer availability contracts.
- P2-065 combines readiness, availability, and consumer contract evidence into one production readiness proof.
- The remaining production gap is that P2-065 can pass without independently proving a live Doris JDBC connection and live physical table inspection in the same operator-facing report.

## In Scope

- Add a read-only physical E2E certification service.
- Add `/warehouse/e2e-certification` for the current tenant.
- Accept `from`, `to`, `mode`, repeated `contractKey`, and `requirePhysical` query parameters.
- Reuse P2-065 production readiness proof.
- Probe Doris JDBC with a bounded `SELECT 1` through the optional `dorisJdbcTemplate`.
- Reuse P2-051 live table inspection through `CdpWarehouseTableGovernanceService.inspectLiveAll`.
- Return compact evidence with `PASS`, `WARN`, or `FAIL`.
- Fail closed when `requirePhysical=true` and Doris is not configured.
- Fail closed when live table contracts are missing or live inspection fails.
- Preserve a non-strict operator dry-run mode with `requirePhysical=false`, where missing physical proof is `WARN`.
- Add focused service and controller tests.

## Out Of Scope

- Writing synthetic events into MySQL, Kafka, Flink, or Doris.
- Creating or mutating Doris tables.
- Persisting certification history.
- Replacing P2-051, P2-055, P2-060, or P2-065 APIs.
- UI.

## Runtime Semantics

1. Default `mode` is `HYBRID`.
2. Default `to` is now and default `from` is one hour before `to`, delegated to P2-065.
3. Default `requirePhysical` is `true`.
4. The service first requests the P2-065 production readiness proof for the same tenant, window, mode, and contract keys.
5. It then probes Doris JDBC with `SELECT 1`.
6. It then runs live table inspection for all active table contracts with inspectedBy `warehouse-e2e-certification`.
7. Any P2-065 status is included as `production_readiness` evidence.
8. Doris JDBC absence, failed query, non-`1` response, missing live table contracts, or failed live table inspection produce `FAIL` when `requirePhysical=true`.
9. The same missing physical proof produces `WARN` when `requirePhysical=false`.
10. Overall certification status is the worst evidence status: `FAIL` beats `WARN` beats `PASS`.

## Functional Requirements

1. Operators can request one physical E2E certification for a bounded warehouse window.
2. The certification includes logical production readiness proof evidence.
3. The certification includes live Doris connectivity evidence.
4. The certification includes live physical table contract evidence.
5. The certification fails when Doris is disabled and physical proof is required.
6. The certification fails when no active live table contract can be inspected and physical proof is required.
7. The certification fails when any live table inspection fails.
8. The API uses the current tenant context and falls back to tenant `0` when no resolver is configured.
9. Existing readiness, availability, contract, and table governance APIs continue unchanged.

## Technical Scope

- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehousePhysicalE2eCertificationService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehousePhysicalE2eCertificationController.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehousePhysicalE2eCertificationServiceTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/web/CdpWarehousePhysicalE2eCertificationControllerTest.java`
- `docs/product-evolution/IMPLEMENTATION_ORDER.md`
- `docs/product-evolution/specs/INDEX.md`
- `docs/product-evolution/plans/INDEX.md`

## Acceptance Criteria

- P2-066 spec and plan are indexed.
- No Flyway migration is added.
- Service tests prove PASS when P2-065, Doris JDBC, and live table inspections pass.
- Service tests prove FAIL when Doris is missing and `requirePhysical=true`.
- Service tests prove WARN when Doris is missing and `requirePhysical=false`.
- Service tests prove FAIL when live table inspection has failures.
- Controller tests prove current tenant scoping and request parameter binding.
- Focused backend tests pass.
- Warehouse/BI/audience regression passes.

## Rollout

1. Deploy the read-only API.
2. In staging, enable `canvas.doris.enabled=true` and configure real Doris JDBC.
3. Ensure active table contracts exist for ODS, DWD, DWS, BI, and audience physical tables.
4. Call `/warehouse/e2e-certification` with critical BI and audience contract keys.
5. Keep production promotion blocked unless the report is `PASS` or a `WARN` has an explicit operator exception.

## Rollback

- Stop calling `/warehouse/e2e-certification`.
- Existing P2-051, P2-055, P2-060, and P2-065 APIs remain available independently.
