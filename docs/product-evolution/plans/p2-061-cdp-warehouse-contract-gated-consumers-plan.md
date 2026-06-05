# CDP Warehouse Contract Gated Consumers Implementation Plan

**Goal:** Connect P2-060 consumer availability contracts to BI query execution and manual audience materialization.

**Architecture:** Inject `CdpWarehouseConsumerAvailabilityService` into BI query execution and audience materialization operations. Add contract-gated methods that evaluate a `contractKey`, block when `allowed=false`, and delegate to the existing execution path when allowed.

**Tech Stack:** Java 21, Spring Boot, existing BI query execution, existing audience materialization operations, P2-060 consumer availability service, JUnit 5, Mockito, AssertJ.

## Scope

- Add P2-061 spec, plan, and indexes.
- Add BI contract-gated execution service method and controller endpoint.
- Add audience materialization contract-gated service method and controller endpoint.
- Add focused service and controller tests.
- Run focused tests, compile, and warehouse/BI/audience regression.

## Tasks

- [x] **Step 1: Add spec, plan, and indexes**

Create and index P2-061 docs.

- [x] **Step 2: Add BI contract-gated execution**

Evaluate consumer contract, block disallowed requests with BI history, and execute allowed requests through the existing query path.

- [x] **Step 3: Add audience contract-gated materialization**

Evaluate consumer contract, block disallowed materialization, and trigger allowed materialization through the existing path.

- [x] **Step 4: Add tests and verify**

Run focused tests, compile, and warehouse/BI/audience regression.

## Verification

- [x] Focused tests passed: `mvn -s "$tmp_settings" -pl canvas-engine -Dtest=CdpWarehouseConsumerAvailabilityServiceTest,CdpWarehouseConsumerAvailabilitySchemaTest,CdpWarehouseAvailabilityControllerTest,BiQueryExecutionServiceTest,BiQueryControllerTest,AudienceMaterializationOperationsServiceTest,CdpWarehouseAudienceMaterializationControllerTest,BiDashboardResourceServiceTest test` (53 tests, 0 failures, 0 errors, 0 skipped).
- [x] Main compile passed: `mvn -s "$tmp_settings" -pl canvas-engine -DskipTests compile`.
- [x] Warehouse/BI/audience regression passed: `mvn -s "$tmp_settings" -pl canvas-engine -Dtest='CdpWarehouse*Test,CdpAudience*Test,Bi*Test,MarketingBi*Test,Doris*Test,AudienceMaterialization*Test,CdpOlapAudienceSchemaTest,StableUserIndexServiceTest,VersionedAudienceBitmapStoreTest,BehaviorAudienceRuleCompilerTest,AudienceQualityServiceTest,MyBatisAudienceDefinitionRepositoryTest' test` (474 tests, 0 failures, 0 errors, 1 skipped).
- [x] Temporary Maven settings were removed after verification.
