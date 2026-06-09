# P2-068 - CDP Warehouse E2E Certification Scheduler And Gate Spec

Priority: P2
Sequence: 068
Source: `docs/product-evolution/specs/p2-066-cdp-warehouse-physical-e2e-certification.md`, `docs/product-evolution/specs/p2-067-cdp-warehouse-e2e-certification-history.md`
Implementation plan: `../plans/p2-068-cdp-warehouse-e2e-certification-scheduler-and-gate-plan.md`

Status: Historical plan evidence records implementation and verification; runtime verification plus commit and merge status was not verified in this docs-only audit.

## Goal

Turn persisted warehouse physical E2E certification runs into a production gate by adding scheduled certification execution and a tenant-scoped "recent PASS evidence" decision.

## Current Baseline

- P2-066 can produce an immediate physical E2E certification from production readiness proof, Doris JDBC connectivity, and live table inspection.
- P2-067 persists manually triggered certification runs in `cdp_warehouse_e2e_certification_run`.
- Operators can inspect history, but there is no disabled-by-default scheduler to refresh evidence and no API that answers whether the last acceptable PASS run is still fresh enough for production promotion.

## In Scope

- Add an E2E certification gate service that evaluates recent persisted runs.
- Add a disabled-by-default scheduler that calls the P2-067 run service with a configured rolling window.
- Protect the scheduler with the existing warehouse lease service when available.
- Add a tenant-scoped `/warehouse/e2e-certification/gate` API.
- Add canonical `/warehouse/e2e-certification/runs` route compatibility for the P2-067 run controller while keeping the current `/warehouse/e2e-certification-runs` route working.
- Add focused service, scheduler, and controller tests.

## Out Of Scope

- New certification storage tables.
- Reimplementing P2-066 physical certification.
- Real Doris/Flink/Kafka execution in unit tests.
- Automatically blocking deploys outside this service.
- Retention cleanup for certification history rows.

## Runtime Semantics

1. The scheduler is disabled unless `canvas.warehouse.e2e-certification-scheduler.enabled=true`.
2. Each scheduler cycle computes `to=now`, `from=to-windowMinutes`, and delegates to `CdpWarehouseE2eCertificationRunService.run`.
3. Scheduler runs use configured `tenantId`, `mode`, comma-separated `contractKeys`, `requirePhysical`, `requestedBy`, and `leaseTtlSeconds`.
4. If a warehouse lease service is configured, only the lease holder runs the cycle.
5. The gate service reads recent persisted runs for the tenant and returns `PASS` only when the latest matching run is `PASS`, fresh within `maxAgeMinutes`, has the requested mode, satisfies `requirePhysical`, and contains all requested contract keys.
6. Missing, stale, failed, mode-mismatched, physical-mismatched, or contract-missing evidence returns `FAIL`.
7. The gate does not execute a certification; it only evaluates persisted evidence.

## Functional Requirements

1. Operators can ask whether current tenant warehouse E2E certification evidence is valid for a production gate.
2. Operators can configure unattended certification refresh without changing P2-066 or P2-067 manual APIs.
3. Scheduled cycles must be overlap guarded and lease guarded.
4. Gate output must identify the matched run ID, status, reason, generated time, expiry time, requested mode, max age, and required contract keys.
5. Existing certification run APIs remain compatible.

## Technical Scope

- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseE2eCertificationGateService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseE2eCertificationScheduler.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseE2eCertificationGateController.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseE2eCertificationRunController.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseE2eCertificationGateServiceTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseE2eCertificationSchedulerTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/web/CdpWarehouseE2eCertificationGateControllerTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/web/CdpWarehouseE2eCertificationRunControllerTest.java`

## Acceptance Criteria

- P2-068 spec and plan are indexed.
- Gate service tests prove fresh PASS evidence passes.
- Gate service tests prove stale, failed, and missing contract evidence fails.
- Scheduler tests prove disabled, enabled, lease-denied, and overlap behavior.
- Controller tests prove tenant scoping and documented route mappings.
- Existing P2-067 run route remains available and canonical nested route is added.
- Focused tests pass.
- Main compile passes.
- Warehouse/BI/audience regression passes.

## Rollout

1. Deploy with scheduler disabled.
2. In staging, manually create a certification run and call `/warehouse/e2e-certification/gate`.
3. Enable scheduler for one tenant with conservative window and `requirePhysical=false` only for dry-run diagnostics.
4. Enable `requirePhysical=true` only where Doris connectivity and physical table contracts are configured.
5. Treat gate `PASS` as required evidence for production OLAP/CDP promotion notes.

## Rollback

- Disable `canvas.warehouse.e2e-certification-scheduler.enabled`.
- Stop calling `/warehouse/e2e-certification/gate`.
- P2-066 immediate certification and P2-067 certification history remain available independently.
