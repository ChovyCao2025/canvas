# CDP Warehouse Metric Change Review Guard Implementation Plan

**Goal:** Add a tenant-scoped semantic metric change workflow that captures P2-041 impact evidence and applies edits only after approval.

**Architecture:** Persist review requests in a small warehouse governance table. `CdpWarehouseMetricChangeReviewService` resolves existing metrics through the BI dataset resolver, calls `CdpWarehouseMetricLineageService` for impact evidence, validates proposed allowed dimensions against dataset dimensions, and updates the existing `bi_metric` row after approval.

**Tech Stack:** Java 21, Spring Boot, MyBatis-Plus, JUnit 5, Mockito, AssertJ.

## Scope

- Add P2-042 spec, plan, and indexes.
- Add an additive Flyway migration for metric change reviews.
- Add DO, mapper, service, and controller.
- Add focused schema, service, and controller tests.
- Verify compile and warehouse/BI/audience regression.

## Tasks

- [x] **Step 1: Add spec, plan, and indexes**

Create and index P2-042 docs.

- [x] **Step 2: Add review schema and persistence objects**

Create `cdp_warehouse_metric_change_review`, DO, and mapper.

- [x] **Step 3: Add review service**

Implement request creation, validation, impact capture, risk classification, approval, rejection, and approved apply.

- [x] **Step 4: Add controller**

Expose tenant-scoped APIs under `/warehouse/metric-change-reviews`.

- [x] **Step 5: Add tests and verify**

Run focused tests, compile, and warehouse/BI/audience regression.

## Verification

- [x] Focused tests passed: `mvn -pl canvas-engine -Dtest=CdpWarehouseMetricChangeReviewSchemaTest,CdpWarehouseMetricChangeReviewServiceTest,CdpWarehouseMetricChangeReviewControllerTest,BiDatasetResourceServiceTest test` (`14 tests, 0 failures`).
- [x] Main compile passed: `mvn -pl canvas-engine -DskipTests compile`.
- [x] Warehouse/BI/audience regression passed: `mvn -pl canvas-engine -Dtest='CdpWarehouse*Test,Bi*Test,MarketingBi*Test,Doris*Test,AudienceMaterialization*Test,CdpOlapAudienceSchemaTest,StableUserIndexServiceTest,VersionedAudienceBitmapStoreTest,BehaviorAudienceRuleCompilerTest,AudienceQualityServiceTest,MyBatisAudienceDefinitionRepositoryTest' test` (`275 tests, 0 failures, 1 skipped`).
