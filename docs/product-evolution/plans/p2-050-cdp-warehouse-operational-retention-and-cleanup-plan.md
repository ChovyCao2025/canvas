# CDP Warehouse Operational Retention And Cleanup Implementation Plan

**Goal:** Add bounded cleanup for warehouse operational ledgers so production tenants can retain enough audit history without letting run, retry, and resolved incident rows grow without limit.

**Architecture:** Reuse existing warehouse ledger tables and MyBatis mappers. `CdpWarehouseRetentionService` owns plan and cleanup rules, `CdpWarehouseController` exposes tenant-scoped operator APIs, and `CdpWarehouseRetentionScheduler` runs the same cleanup entrypoint behind the existing warehouse lease pattern.

**Tech Stack:** Java 21, Spring Boot, MyBatis-Plus, JUnit 5, Mockito, AssertJ.

## Scope

- Add P2-050 spec, plan, and indexes.
- Add retention plan and cleanup service.
- Expose tenant-scoped plan and run APIs.
- Add disabled-by-default scheduler with lease and overlap guard.
- Add focused tests and regression verification.

## Tasks

- [x] **Step 1: Add spec, plan, and indexes**

Create and index P2-050 docs.

- [x] **Step 2: Add retention planning**

Count eligible sync runs, terminal retry rows, and resolved incidents from bounded retention cutoffs.

- [x] **Step 3: Add cleanup execution**

Delete only eligible terminal ledger rows and return per-ledger deleted counts.

- [x] **Step 4: Add API and scheduler integration**

Expose plan/run endpoints and add a disabled-by-default retention scheduler with warehouse lease protection.

- [x] **Step 5: Add tests and verify**

Run focused tests, compile, and warehouse/BI/audience regression.

## Verification

- [x] Focused tests passed: `mvn -s "$tmp_settings" -pl canvas-engine -Dtest=CdpWarehouseRetentionServiceTest,CdpWarehouseRetentionSchedulerTest,CdpWarehouseControllerTest test` (14 tests, 0 failures, 0 errors, 0 skipped).
- [x] Main compile passed: `mvn -s "$tmp_settings" -pl canvas-engine -DskipTests compile`.
- [x] Warehouse/BI/audience regression passed: `mvn -s "$tmp_settings" -pl canvas-engine -Dtest='CdpWarehouse*Test,CdpAudience*Test,Bi*Test,MarketingBi*Test,Doris*Test,AudienceMaterialization*Test,CdpOlapAudienceSchemaTest,StableUserIndexServiceTest,VersionedAudienceBitmapStoreTest,BehaviorAudienceRuleCompilerTest,AudienceQualityServiceTest,MyBatisAudienceDefinitionRepositoryTest' test` (385 tests, 0 failures, 0 errors, 1 skipped).

Temporary Maven settings were used to avoid the local Nexus mirror timeout and were removed after verification.
