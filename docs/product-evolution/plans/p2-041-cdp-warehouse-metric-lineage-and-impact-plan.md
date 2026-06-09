# CDP Warehouse Metric Lineage And Impact Implementation Plan

Spec: `../specs/p2-041-cdp-warehouse-metric-lineage-and-impact.md`

**Goal:** Add read-only metric impact analysis using existing semantic metric contracts, dataset lineage, BI charts, and BI dashboards.

**Architecture:** `CdpWarehouseMetricLineageService` resolves the metric through `BiDatasetSpecResolver`, derives field dependencies from the metric expression and allowed dimensions, then optionally reads catalog lineage, chart resources, and dashboard resources.

**Tech Stack:** Java 21, Spring Boot, JUnit 5, Mockito, AssertJ.

Status: Historical plan evidence records implementation and verification; runtime verification plus commit and merge status was not verified in this docs-only audit.

## Scope

- Add P2-041 spec, plan, and indexes.
- Add metric lineage/impact service.
- Add tenant-scoped controller.
- Add focused tests.
- No Flyway migration.

## Tasks

- [x] **Step 1: Add spec, plan, and indexes**

Create and index P2-041 docs.

- [x] **Step 2: Add impact service**

Resolve semantic metric contracts, extract field dependencies, include dataset lineage, charts, dashboards, and warnings.

- [x] **Step 3: Add controller**

Expose metric impact under `/warehouse/metric-lineage/impact`.

- [x] **Step 4: Add tests and verify**

Run focused tests, compile, and warehouse/BI/audience regression.

## Verification

- [x] Focused tests passed: `mvn -pl canvas-engine -Dtest=CdpWarehouseMetricLineageServiceTest,CdpWarehouseMetricLineageControllerTest test` (`5 tests, 0 failures`).
- [x] Main compile passed: `mvn -pl canvas-engine -DskipTests compile`.
- [x] Warehouse/BI/audience regression passed: `mvn -pl canvas-engine -Dtest='CdpWarehouse*Test,Bi*Test,MarketingBi*Test,Doris*Test,AudienceMaterialization*Test,CdpOlapAudienceSchemaTest,StableUserIndexServiceTest,VersionedAudienceBitmapStoreTest,BehaviorAudienceRuleCompilerTest,AudienceQualityServiceTest,MyBatisAudienceDefinitionRepositoryTest' test` (`260 tests, 0 failures, 1 skipped`).
