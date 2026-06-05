# CDP Warehouse BI Query Availability Gate Implementation Plan

**Goal:** Add an availability-gated BI query execution path without changing the existing BI execute endpoint.

**Architecture:** Reuse `CdpWarehouseAvailabilityService` inside `BiQueryExecutionService`. Return a combined response that carries the availability decision and, only when allowed, the existing BI query result. Blocked gated queries write BI history with status `BLOCKED` and never call the datasource.

**Tech Stack:** Java 21, Spring Boot, existing BI query execution service, existing warehouse availability service, JUnit 5, Mockito, AssertJ.

## Scope

- Add P2-057 spec, plan, and indexes.
- Add gated BI query result model and execution operation.
- Add tenant-scoped gated API endpoint.
- Add focused service and controller tests.
- Run focused tests and regression verification.

## Tasks

- [x] **Step 1: Add spec, plan, and indexes**

Create and index P2-057 docs.

- [x] **Step 2: Add gated BI execution**

Evaluate warehouse availability and invoke existing BI execution only when PASS or allowed WARN.

- [x] **Step 3: Add API integration**

Expose a tenant-scoped gated BI execution endpoint with query, window, mode, and allowWarn fields.

- [x] **Step 4: Add tests and verify**

Run focused tests, compile, and warehouse/BI/audience regression.

## Verification

- [x] Focused tests passed: `mvn -s "$tmp_settings" -pl canvas-engine -Dtest=BiQueryExecutionServiceTest,BiQueryControllerTest test` (19 tests, 0 failures, 0 errors, 0 skipped).
- [x] Main compile passed: `mvn -s "$tmp_settings" -pl canvas-engine -DskipTests compile`.
- [x] Warehouse/BI/audience regression passed: `mvn -s "$tmp_settings" -pl canvas-engine -Dtest='CdpWarehouse*Test,CdpAudience*Test,Bi*Test,MarketingBi*Test,Doris*Test,AudienceMaterialization*Test,CdpOlapAudienceSchemaTest,StableUserIndexServiceTest,VersionedAudienceBitmapStoreTest,BehaviorAudienceRuleCompilerTest,AudienceQualityServiceTest,MyBatisAudienceDefinitionRepositoryTest' test` (438 tests, 0 failures, 0 errors, 1 skipped).
- [x] Temporary Maven settings were removed after verification.
