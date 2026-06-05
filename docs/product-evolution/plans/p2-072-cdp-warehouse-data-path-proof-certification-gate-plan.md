# CDP Warehouse Data Path Proof Certification Gate Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Require P2-071 synthetic ODS data-path proof in warehouse physical E2E certification and production promotion gates.

**Architecture:** Reuse `CdpWarehouseSyntheticDataPathProbeService` inside physical E2E certification, persist the proof summary with certification runs, and extend scheduler/gate/controller flags without changing lower-level sink, Doris Stream Load, or realtime runtime behavior.

**Tech Stack:** Java 21, Spring Boot WebFlux controllers, Spring scheduling, MyBatis-Plus, Flyway, Jackson, JUnit 5, Mockito, AssertJ.

---

## Scope

- Add P2-072 spec, plan, and index rows.
- Add certification run history fields for data-path proof.
- Extend physical certification with P2-071 evidence.
- Extend persisted runs, scheduled runs, and gate matching.
- Extend immediate/run/gate controllers with request binding.
- Add focused tests and regression verification.

## Files

- Create `backend/canvas-engine/src/main/resources/db/migration/V253__cdp_warehouse_e2e_data_path_proof.sql`
- Modify `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpWarehouseE2eCertificationRunDO.java`
- Modify `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehousePhysicalE2eCertificationService.java`
- Modify `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseE2eCertificationRunService.java`
- Modify `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseE2eCertificationGateService.java`
- Modify `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseE2eCertificationScheduler.java`
- Modify `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehousePhysicalE2eCertificationController.java`
- Modify `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseE2eCertificationRunController.java`
- Modify `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseE2eCertificationGateController.java`
- Add or update focused tests under `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/` and `backend/canvas-engine/src/test/java/org/chovy/canvas/web/`.

## Tasks

- [x] **Step 1: Add spec, plan, and index**

Create P2-072 docs and index rows.

- [x] **Step 2: Add failing schema and certification tests**

Cover migration fields, data-path PASS evidence, missing/failing proof failure, and non-required skip WARN.

- [x] **Step 3: Add failing run, scheduler, gate, and controller tests**

Cover persisted requirement/proof JSON, scheduler delegation, gate matching, and request parameter binding.

- [x] **Step 4: Implement schema and data object fields**

Add migration and DO fields for `require_data_path_proof` and `data_path_proof_json`.

- [x] **Step 5: Implement certification data-path evidence**

Inject optional P2-071 service, run strict proof when required, add evidence, and expose proof summary.

- [x] **Step 6: Implement run, scheduler, gate, and controller wiring**

Pass `requireDataPathProof` through immediate API, run API, scheduler, persisted view, and gate matching.

- [x] **Step 7: Verify**

Run focused tests and warehouse regression. Update this plan with observed evidence.

## Verification

- `mvn -s "$tmp_settings" -pl canvas-engine test -Dtest=CdpWarehouseE2eDataPathProofSchemaTest,CdpWarehousePhysicalE2eCertificationServiceTest,CdpWarehouseE2eCertificationRunServiceTest,CdpWarehouseE2eCertificationGateServiceTest,CdpWarehouseE2eCertificationSchedulerTest,CdpWarehousePhysicalE2eCertificationControllerTest,CdpWarehouseE2eCertificationRunControllerTest,CdpWarehouseE2eCertificationGateControllerTest` - PASS, 36 tests.
- `mvn -s "$tmp_settings" -pl canvas-engine test -Dtest='CdpWarehouse*Test,Doris*Test'` - PASS, 367 tests passed, 1 skipped (`DorisConnectionTest`, real Doris disabled).
