# CDP Warehouse Scheduled Audience Availability Gate Implementation Plan

**Goal:** Add an availability-gated scheduled audience refresh path without changing the existing refresh-due endpoint.

**Architecture:** Reuse `CdpWarehouseAvailabilityService` inside `AudienceMaterializationScheduleService`. Return a combined response that carries the availability decision and, only when allowed, the existing scheduled refresh result. Add scheduler configuration to delegate unattended cycles to the gated path.

**Tech Stack:** Java 21, Spring Boot, existing audience materialization scheduler, existing warehouse availability service, JUnit 5, Mockito, AssertJ.

## Scope

- Add P2-058 spec, plan, and indexes.
- Add gated scheduled refresh result model and operation.
- Add tenant-scoped gated refresh-due API endpoint.
- Add scheduler configuration for availability-gated cycles.
- Add focused service, scheduler, and controller tests.
- Run focused tests and regression verification.

## Tasks

- [x] **Step 1: Add spec, plan, and indexes**

Create and index P2-058 docs.

- [x] **Step 2: Add gated scheduled refresh**

Evaluate warehouse availability and invoke existing refresh-due only when PASS or allowed WARN.

- [x] **Step 3: Add API and scheduler integration**

Expose a tenant-scoped gated refresh-due endpoint and configure the scheduler to call the gated path when enabled.

- [x] **Step 4: Add tests and verify**

Run focused tests, compile, and warehouse/BI/audience regression.

## Verification

- [x] Focused tests passed: `mvn -s "$tmp_settings" -pl canvas-engine -Dtest=AudienceMaterializationScheduleServiceTest,CdpWarehouseAudienceMaterializationSchedulerTest,CdpWarehouseAudienceMaterializationControllerTest,AudienceMaterializationOperationsServiceTest test` (25 tests, 0 failures, 0 errors, 0 skipped).
- [x] Main compile passed: `mvn -s "$tmp_settings" -pl canvas-engine -DskipTests compile`.
- [x] Warehouse/BI/audience regression passed: `mvn -s "$tmp_settings" -pl canvas-engine -Dtest='CdpWarehouse*Test,CdpAudience*Test,Bi*Test,MarketingBi*Test,Doris*Test,AudienceMaterialization*Test,CdpOlapAudienceSchemaTest,StableUserIndexServiceTest,VersionedAudienceBitmapStoreTest,BehaviorAudienceRuleCompilerTest,AudienceQualityServiceTest,MyBatisAudienceDefinitionRepositoryTest' test` (443 tests, 0 failures, 0 errors, 1 skipped).
- [x] Temporary Maven settings were removed after verification.
