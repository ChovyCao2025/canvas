# CDP Warehouse Metric Impact Transitive Lineage Implementation Plan

Spec: `../specs/p2-044-cdp-warehouse-metric-impact-transitive-lineage.md`

**Goal:** Extend metric impact and metric-change review to use P2-043 bounded transitive lineage.

**Architecture:** Keep P2-041 direct lineage fields intact. `CdpWarehouseMetricLineageService` calls the existing catalog transitive traversal and places the graph on the impact response. `CdpWarehouseMetricChangeReviewService` summarizes the transitive graph into review impact counts and risk classification.

**Tech Stack:** Java 21, Spring Boot, JUnit 5, Mockito, AssertJ.

Status: Historical plan evidence records implementation and verification; runtime verification plus commit and merge status was not verified in this docs-only audit.

## Scope

- Add P2-044 spec, plan, and indexes.
- Add no Flyway migration.
- Extend metric impact response with transitive lineage evidence.
- Extend metric-change review impact summary with transitive counts and truncation.
- Add focused tests.
- Verify compile and warehouse/BI/audience regression.

## Tasks

- [x] **Step 1: Add spec, plan, and indexes**

Create and index P2-044 docs.

- [x] **Step 2: Extend metric impact**

Call `CdpWarehouseCatalogService#transitiveLineage` from metric impact and attach the graph while preserving direct lineage fields.

- [x] **Step 3: Extend review impact summary**

Store transitive node/edge/path/downstream/truncation counts and include them in risk classification.

- [x] **Step 4: Add tests and verify**

Run focused tests, compile, and warehouse/BI/audience regression.

## Verification

- [x] Focused tests passed: `mvn -pl canvas-engine -Dtest=CdpWarehouseMetricLineageServiceTest,CdpWarehouseMetricLineageControllerTest,CdpWarehouseMetricChangeReviewServiceTest,CdpWarehouseMetricChangeReviewControllerTest test` (`15 tests, 0 failures`).
- [x] Main compile passed: `mvn -pl canvas-engine -DskipTests compile`.
- [x] Warehouse/BI/audience regression passed: `mvn -pl canvas-engine -Dtest='CdpWarehouse*Test,Bi*Test,MarketingBi*Test,Doris*Test,AudienceMaterialization*Test,CdpOlapAudienceSchemaTest,StableUserIndexServiceTest,VersionedAudienceBitmapStoreTest,BehaviorAudienceRuleCompilerTest,AudienceQualityServiceTest,MyBatisAudienceDefinitionRepositoryTest' test` (`288 tests, 0 failures, 1 skipped`).
