# P2-067 - CDP Warehouse E2E Certification History Spec

Priority: P2
Sequence: 067
Source: `docs/product-evolution/specs/p2-065-cdp-warehouse-production-readiness-proof.md`, `docs/product-evolution/specs/p2-066-cdp-warehouse-physical-e2e-certification.md`
Implementation plan: `../plans/p2-067-cdp-warehouse-e2e-certification-history-plan.md`

## Goal

Persist operator-triggered CDP warehouse E2E certification runs so production readiness decisions have auditable evidence, not only a transient API response.

## Current Baseline

- P2-065 returns a logical production readiness proof for a bounded offline/realtime warehouse window.
- P2-066 returns a physical E2E certification by combining P2-065, Doris JDBC connectivity, and live table contract inspection.
- P2-066 is read-only and transient: if an operator uses it to approve a production rollout, the result is not stored as an auditable run record.

## In Scope

- Add a `cdp_warehouse_e2e_certification_run` table.
- Add a run service that delegates to P2-066 and persists the returned certification.
- Store tenant, window, mode, contract keys, physical requirement, final status, requestedBy, timing, evidence JSON, production readiness JSON, live table inspection JSON, and error message.
- Add tenant-scoped APIs to run, list, and get certification history.
- Fail the run record with persisted error details if P2-066 throws.
- Add schema, service, and controller tests.

## Out Of Scope

- Scheduling automatic certification runs.
- Retention cleanup for historical run rows.
- UI.
- Real Doris/Flink/Kafka execution.
- Replacing P2-066 immediate certification.

## Runtime Semantics

1. `POST /warehouse/e2e-certification/runs` triggers P2-066 and persists one run row.
2. The request accepts `from`, `to`, `mode`, repeated `contractKey`, `requirePhysical`, and optional `requestedBy`.
3. Default `mode` remains `HYBRID`.
4. Default `requirePhysical` remains `true`.
5. If P2-066 returns a certification, the run status equals the certification status.
6. If P2-066 throws, the run status is `FAIL`, the error is persisted, and the API returns the failed run view.
7. `GET /warehouse/e2e-certification/runs` lists recent tenant-scoped runs.
8. `GET /warehouse/e2e-certification/runs/{id}` returns one tenant-scoped run or fails when the row does not belong to the current tenant.

## Functional Requirements

1. Operators can persist a certification run before production promotion.
2. Operators can list recent certification runs for a tenant.
3. Operators can retrieve the stored evidence for a specific run.
4. Stored evidence includes contract keys and certification evidence.
5. Failed certification execution is auditable instead of disappearing as an exception.
6. P2-066 immediate certification remains unchanged.

## Technical Scope

- `backend/canvas-engine/src/main/resources/db/migration/V242__cdp_warehouse_e2e_certification_history.sql`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpWarehouseE2eCertificationRunDO.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpWarehouseE2eCertificationRunMapper.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseE2eCertificationRunService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseE2eCertificationRunController.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseE2eCertificationRunSchemaTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseE2eCertificationRunServiceTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/web/CdpWarehouseE2eCertificationRunControllerTest.java`

## Acceptance Criteria

- P2-067 spec and plan are indexed.
- A new Flyway migration creates `cdp_warehouse_e2e_certification_run`.
- Schema tests verify table, key indexes, and JSON columns.
- Service tests prove successful P2-066 certification is persisted.
- Service tests prove thrown P2-066 certification creates a `FAIL` run with error details.
- Service tests prove list/get are tenant-scoped.
- Controller tests prove tenant binding and request parameter binding.
- Focused backend tests pass.
- Warehouse/BI/audience regression passes.

## Rollout

1. Deploy the table and read/write APIs.
2. In staging, call `POST /warehouse/e2e-certification/runs` before production-like BI and audience exercises.
3. Attach run IDs to production promotion notes.
4. Keep `GET /warehouse/e2e-certification` available for immediate no-history checks.

## Rollback

- Stop calling `/warehouse/e2e-certification/runs`.
- P2-066 immediate certification remains available independently.
