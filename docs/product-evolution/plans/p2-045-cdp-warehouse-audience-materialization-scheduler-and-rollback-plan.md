# CDP Warehouse Audience Materialization Scheduler And Rollback Implementation Plan

**Goal:** Add production-grade scheduled audience materialization and guarded rollback for versioned audience bitmaps.

**Architecture:** Keep P2-021 materialization as the single writer. Add due-selection and scheduling around it. Keep rollback in the bitmap store/persistence layer so membership lookup continues to rely on DB `READY` state. Reuse `CdpWarehouseJobLeaseService` for distributed scheduler protection.

**Tech Stack:** Java 21, Spring Boot, MyBatis-Plus, JUnit 5, Mockito, AssertJ.

## Scope

- Add P2-045 spec, plan, and indexes.
- Add rollback audit schema and DAL.
- Add bitmap rollback DB-state updates.
- Add due-refresh service and disabled-by-default lease-protected scheduler.
- Add rollback and manual due-refresh APIs.
- Add focused tests and regression verification.

## Tasks

- [x] **Step 1: Add spec, plan, and indexes**

Create and index P2-045 docs.

- [x] **Step 2: Add rollback schema and persistence**

Add rollback audit migration, data object, mapper, and bitmap version update methods.

- [x] **Step 3: Add rollback operations API**

Add guarded rollback behavior to the bitmap store and audience materialization operations/controller.

- [x] **Step 4: Add due-refresh scheduler**

Add due-audience selection, cron due logic, manual due-refresh API, and disabled-by-default scheduler with lease/overlap guards.

- [x] **Step 5: Add tests and verify**

Run focused tests, compile, and warehouse/BI/audience regression.

## Verification

- [x] Focused tests passed: `mvn -pl canvas-engine -Dtest=CdpAudienceMaterializationRollbackSchemaTest,AudienceMaterializationOperationsServiceTest,AudienceMaterializationScheduleServiceTest,CdpWarehouseAudienceMaterializationSchedulerTest,VersionedAudienceBitmapStoreTest,CdpWarehouseAudienceMaterializationControllerTest test` (`23 tests, 0 failures`).
- [x] Main compile passed: `mvn -pl canvas-engine -DskipTests compile`.
- [x] Warehouse/BI/audience regression passed: `mvn -pl canvas-engine -Dtest='CdpWarehouse*Test,CdpAudience*Test,Bi*Test,MarketingBi*Test,Doris*Test,AudienceMaterialization*Test,CdpOlapAudienceSchemaTest,StableUserIndexServiceTest,VersionedAudienceBitmapStoreTest,BehaviorAudienceRuleCompilerTest,AudienceQualityServiceTest,MyBatisAudienceDefinitionRepositoryTest' test` (`317 tests, 0 failures, 1 skipped`).
