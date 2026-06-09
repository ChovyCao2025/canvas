# CDP Warehouse Asset Availability Automation Implementation Plan

Spec: `../specs/p2-062-cdp-warehouse-asset-availability-automation.md`

Status: Historical plan evidence records implementation and verification; runtime verification plus commit and merge status was not verified in this docs-only audit.

**Goal:** Publish P2-060 asset availability observations from offline aggregation and realtime pipeline checkpoint evidence.

**Architecture:** Inject `CdpWarehouseConsumerAvailabilityService` as an optional dependency into offline aggregation and realtime pipeline services. On successful aggregation or checkpoint reporting, call `recordAssetAvailability` with deterministic table/dataset assets. Treat this as a best-effort side effect so core warehouse job recording remains authoritative.

**Tech Stack:** Java 21, Spring Boot, ObjectProvider optional dependencies, existing P2-060 availability service, JUnit 5, Mockito, AssertJ.

## Scope

- Add P2-062 spec, plan, and indexes.
- Add offline aggregation asset observation side effect.
- Add realtime checkpoint sink availability side effect.
- Add focused service tests.
- Run focused tests, compile, and warehouse/BI/audience regression.

## Tasks

- [x] **Step 1: Add spec, plan, and indexes**

Create and index P2-062 docs.

- [x] **Step 2: Add offline aggregation observations**

Publish DWD/DWS table and dataset `OFFLINE` PASS observations after successful aggregation.

- [x] **Step 3: Add realtime checkpoint observations**

Publish sink table `REALTIME` observations after checkpoint reports, including FAIL evidence for missing watermarks.

- [x] **Step 4: Add tests and verify**

Run focused tests, compile, and warehouse/BI/audience regression.

## Verification

- [x] Focused tests passed: `mvn -s "$tmp_settings" -pl canvas-engine -Dtest=CdpWarehouseAggregationServiceTest,CdpWarehouseRealtimePipelineServiceTest test` (20 tests, 0 failures, 0 errors, 0 skipped).
- [x] Main compile passed: `mvn -s "$tmp_settings" -pl canvas-engine -DskipTests compile`.
- [x] Warehouse/BI/audience regression passed: `mvn -s "$tmp_settings" -pl canvas-engine -Dtest='CdpWarehouse*Test,CdpAudience*Test,Bi*Test,MarketingBi*Test,Doris*Test,AudienceMaterialization*Test,CdpOlapAudienceSchemaTest,StableUserIndexServiceTest,VersionedAudienceBitmapStoreTest,BehaviorAudienceRuleCompilerTest,AudienceQualityServiceTest,MyBatisAudienceDefinitionRepositoryTest' test` (483 tests, 0 failures, 0 errors, 1 skipped).
- [x] Temporary Maven settings were removed after verification.
