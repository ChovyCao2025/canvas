# CDP Warehouse Consumer Availability Contracts Implementation Plan

Spec: `../specs/p2-060-cdp-warehouse-consumer-availability-contracts.md`

Status: Historical plan evidence records implementation and verification; runtime verification plus commit and merge status was not verified in this docs-only audit.

**Goal:** Add table/dataset/metric availability observations and downstream consumer contracts that combine asset-level evidence with the existing warehouse availability gate.

**Architecture:** Introduce two additive MySQL tables, a `CdpWarehouseConsumerAvailabilityService`, and APIs under `/warehouse/availability`. The service evaluates a contract by calling `CdpWarehouseAvailabilityService.evaluate` and then applying asset-specific gates from the latest availability observations.

**Tech Stack:** Java 21, Spring Boot, MyBatis Plus, Jackson, existing warehouse availability service, JUnit 5, Mockito, AssertJ.

## Scope

- Add P2-060 spec, plan, and indexes.
- Add asset availability and consumer contract schema.
- Add DOs, mappers, service, and availability controller endpoints.
- Add focused schema, service, and controller tests.
- Run focused tests, compile, and warehouse/BI/audience regression.

## Tasks

- [x] **Step 1: Add spec, plan, and indexes**

Create and index P2-060 docs.

- [x] **Step 2: Add schema and persistence objects**

Create asset availability and consumer contract tables with tenant-scoped unique keys, DOs, and mappers.

- [x] **Step 3: Add contract evaluation service and APIs**

Evaluate consumer contracts through existing window-level availability plus per-asset evidence, returning allow/block decisions.

- [x] **Step 4: Add tests and verify**

Run focused tests, compile, and warehouse/BI/audience regression.

## Verification

- [x] Focused tests passed: `mvn -s "$tmp_settings" -pl canvas-engine -Dtest=CdpWarehouseConsumerAvailabilityServiceTest,CdpWarehouseConsumerAvailabilitySchemaTest,CdpWarehouseAvailabilityControllerTest,BiQueryExecutionServiceTest,BiQueryControllerTest,AudienceMaterializationOperationsServiceTest,CdpWarehouseAudienceMaterializationControllerTest,BiDashboardResourceServiceTest test` (53 tests, 0 failures, 0 errors, 0 skipped).
- [x] Main compile passed: `mvn -s "$tmp_settings" -pl canvas-engine -DskipTests compile`.
- [x] Warehouse/BI/audience regression passed: `mvn -s "$tmp_settings" -pl canvas-engine -Dtest='CdpWarehouse*Test,CdpAudience*Test,Bi*Test,MarketingBi*Test,Doris*Test,AudienceMaterialization*Test,CdpOlapAudienceSchemaTest,StableUserIndexServiceTest,VersionedAudienceBitmapStoreTest,BehaviorAudienceRuleCompilerTest,AudienceQualityServiceTest,MyBatisAudienceDefinitionRepositoryTest' test` (474 tests, 0 failures, 0 errors, 1 skipped).
- [x] Temporary Maven settings were removed after verification.
