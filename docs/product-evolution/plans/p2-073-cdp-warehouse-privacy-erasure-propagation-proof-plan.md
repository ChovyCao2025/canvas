# CDP Warehouse Privacy Erasure Propagation Proof Implementation Plan

Spec: `../specs/p2-073-cdp-warehouse-privacy-erasure-propagation-proof.md`

Status: Historical plan evidence records implementation and verification; runtime verification plus commit and merge status was not verified in this docs-only audit.

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a tenant-scoped, auditable privacy erasure propagation ledger and readiness evidence for CDP warehouse assets.

**Architecture:** Store erasure requests and per-asset proof rows in MySQL, keep raw subjects out of persisted/returned views through hashing and masking, roll up request/backlog status in a domain service, expose operator APIs, and add optional production readiness evidence.

**Tech Stack:** Java 21, Spring Boot WebFlux controllers, MyBatis-Plus, Flyway, Jackson, JUnit 5, Mockito, AssertJ.

---

## Scope

- Add P2-073 spec, plan, and index rows.
- Add erasure request and asset proof schema.
- Implement request creation, default planning, asset proof recording, request rollup, history, and backlog summary.
- Add operator controller endpoints.
- Add optional production readiness evidence.
- Add focused tests and regression verification.

## Files

- Create `backend/canvas-engine/src/main/resources/db/migration/V254__cdp_warehouse_privacy_erasure_proof.sql`
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpWarehousePrivacyErasureRequestDO.java`
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpWarehousePrivacyErasureAssetProofDO.java`
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpWarehousePrivacyErasureRequestMapper.java`
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpWarehousePrivacyErasureAssetProofMapper.java`
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehousePrivacyErasureService.java`
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehousePrivacyErasureController.java`
- Modify `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseProductionReadinessProofService.java`
- Add focused tests under `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/` and `backend/canvas-engine/src/test/java/org/chovy/canvas/web/`.

## Tasks

- [x] **Step 1: Add spec, plan, and index**

Create P2-073 docs and index rows.

- [x] **Step 2: Add schema and service tests**

Cover migration fields, subject hashing/masking, default asset plans, proof rollup, and backlog summary.

- [x] **Step 3: Add readiness and controller tests**

Cover `privacy_erasure_backlog` evidence and tenant-scoped controller delegation.

- [x] **Step 4: Implement schema, DOs, and mappers**

Add additive migration and MyBatis-Plus objects.

- [x] **Step 5: Implement erasure proof service**

Create request, insert plans, record proof, recompute status, list history, and summarize backlog.

- [x] **Step 6: Implement controller and readiness wiring**

Expose operator endpoints and add optional readiness proof evidence.

- [x] **Step 7: Verify**

Run focused tests and warehouse regression. Update this plan with observed evidence.

## Verification

- `mvn -s "$tmp_settings" -pl canvas-engine test -Dtest=CdpWarehousePrivacyErasureSchemaTest,CdpWarehousePrivacyErasureServiceTest,CdpWarehouseProductionReadinessProofServiceTest,CdpWarehousePrivacyErasureControllerTest` - PASS, 12 tests.
- `mvn -s "$tmp_settings" -pl canvas-engine test -Dtest='CdpWarehouse*Test,Doris*Test'` - PASS, 375 tests passed, 1 skipped (`DorisConnectionTest`, real Doris disabled).
- P2-073 privacy-ledger redaction audit updated on 2026-06-05:
  `CdpWarehousePrivacyErasureServiceTest.recordAssetProofRedactsRawSubjectCompatibleWithMaskedReference`
  proves operator-submitted proof/error text is redacted before proof rows, returned views, and request `evidenceJson` can persist masked-reference-compatible raw subjects.
- Focused OLAP privacy slice passed on 2026-06-05:
  `CdpWarehousePrivacyErasureExecutionServiceTest,CdpWarehousePrivacyErasureServiceTest,CdpWarehousePrivacyErasureControllerTest,CdpWarehousePrivacyTombstoneServiceTest,CdpWarehousePrivacyAudienceBitmapRebuildServiceTest,CdpWarehousePrivacyAudienceBitmapRebuildAutomationServiceTest,CdpWarehousePrivacyAudienceBitmapRebuildSchedulerTest,CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunServiceTest,CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunSchemaTest,CdpWarehouseProductionReadinessProofServiceTest`
  - Result: 50 tests, 0 failures.
- Warehouse/BI/CDP regression passed on 2026-06-05:
  `CdpWarehouse*Test,CdpAudience*Test,Bi*Test,MarketingBi*Test,Doris*Test,AudienceMaterialization*Test,CdpOlapAudienceSchemaTest,StableUserIndexServiceTest,VersionedAudienceBitmapStoreTest,BehaviorAudienceRuleCompilerTest,AudienceQualityServiceTest,MyBatisAudienceDefinitionRepositoryTest,CdpEventIngestion*Test,CdpUserServiceTest,UserWorkspacePreferenceServiceTest,CanvasControllerCollaborationTest`
  - Result: 793 tests, 0 failures, 1 skipped (`DorisConnectionTest`).
