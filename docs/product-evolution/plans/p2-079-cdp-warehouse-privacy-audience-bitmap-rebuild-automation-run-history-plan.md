# CDP Warehouse Privacy Audience Bitmap Rebuild Automation Run History Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Persist privacy audience bitmap rebuild automation runs for manual and scheduled execution, and expose tenant-scoped run history APIs.

**Architecture:** Add a small run history service that wraps the existing P2-077 automation service and persists run rows before and after delegation. Wire P2-078 manual execution and the P2-077 scheduler through the wrapper when configured, while keeping the existing `AutomationResult` response shape and P2-077 eligibility rules unchanged.

**Tech Stack:** Java 21, Spring services/controllers, MyBatis-Plus, Flyway, Jackson, JUnit 5, Mockito, AssertJ.

---

## Scope

- Add P2-079 spec, plan, and index rows.
- Add a migration, DO, mapper, and schema test.
- Add failing run history service tests first.
- Add failing controller and scheduler tests.
- Implement run history persistence and APIs.
- Verify focused tests and warehouse/CDP regression.

## Files

- Create `backend/canvas-engine/src/main/resources/db/migration/V261__privacy_audience_rebuild_automation_run.sql`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunDO.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunMapper.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunService.java`.
- Add `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunSchemaTest.java`.
- Add `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunServiceTest.java`.
- Modify `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehousePrivacyAudienceBitmapRebuildScheduler.java`.
- Modify `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehousePrivacyAudienceBitmapRebuildSchedulerTest.java`.
- Modify `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehousePrivacyErasureController.java`.
- Modify `backend/canvas-engine/src/test/java/org/chovy/canvas/web/CdpWarehousePrivacyErasureControllerTest.java`.
- Modify product-evolution indexes.

## Tasks

- [x] **Step 1: Add spec, plan, and indexes**

Create P2-079 docs and insert rows after P2-078.

- [x] **Step 2: Write failing schema and run service tests**

Add tests for migration contents, success persistence, failure persistence, recent listing, and tenant-scoped get.

Expected red command:

```bash
cd backend
mvn -pl canvas-engine test -Dtest=CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunSchemaTest,CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunServiceTest
```

Expected failure before implementation: missing migration, DO, mapper, or run service.

- [x] **Step 3: Write failing controller and scheduler tests**

Add tests proving manual runs call the run history service, recent/get APIs are tenant scoped, and scheduler uses `triggerSource=SCHEDULED` when the run history service is configured.

- [x] **Step 4: Implement migration, DO, mapper, and run service**

Persist `RUNNING` before delegation, update the row on success/failure, serialize the P2-077 result JSON, and expose recent/get methods.

- [x] **Step 5: Wire controller and scheduler**

Route P2-078 manual execution through the run history service when configured, keep fallback to P2-077 automation service, add history query endpoints, and route scheduler execution through the wrapper when configured.

- [x] **Step 6: Verify focused tests**

Run:

```bash
cd backend
mvn -pl canvas-engine test -Dtest=CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunSchemaTest,CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunServiceTest,CdpWarehousePrivacyErasureControllerTest,CdpWarehousePrivacyAudienceBitmapRebuildSchedulerTest,CdpWarehousePrivacyAudienceBitmapRebuildAutomationServiceTest,CdpWarehousePrivacyAudienceBitmapRebuildServiceTest
```

Expected: all selected tests pass.

- [x] **Step 7: Verify warehouse/CDP regression**

Run:

```bash
cd backend
mvn -pl canvas-engine clean test -Dtest='CdpWarehouse*Test,Doris*Test,CdpEventIngestion*Test,CdpUserServiceTest'
```

Expected: all selected tests pass; `DorisConnectionTest` may be skipped unless `DORIS_ENABLED=true`.

## Verification

- P2-079 focused suite passed on 2026-06-05:
  `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine test -Dtest='CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunSchemaTest,CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunServiceTest,CdpWarehousePrivacyErasureControllerTest,CdpWarehousePrivacyAudienceBitmapRebuildSchedulerTest,CdpWarehousePrivacyAudienceBitmapRebuildAutomationServiceTest,CdpWarehousePrivacyAudienceBitmapRebuildServiceTest'`
  - Result: 26 tests, 0 failures.
- Warehouse/BI/audience/CDP regression passed on 2026-06-05:
  `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine test -Dtest='CdpWarehouse*Test,CdpAudience*Test,Bi*Test,MarketingBi*Test,Doris*Test,AudienceMaterialization*Test,CdpOlapAudienceSchemaTest,StableUserIndexServiceTest,VersionedAudienceBitmapStoreTest,BehaviorAudienceRuleCompilerTest,AudienceQualityServiceTest,MyBatisAudienceDefinitionRepositoryTest,CdpEventIngestion*Test,CdpUserServiceTest'`
  - Result: 774 tests, 0 failures, 1 skipped (`DorisConnectionTest`).
