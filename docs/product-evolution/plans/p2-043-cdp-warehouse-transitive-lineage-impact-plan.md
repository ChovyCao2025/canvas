# CDP Warehouse Transitive Lineage Impact Implementation Plan

**Goal:** Add bounded multi-hop lineage traversal on top of the existing warehouse catalog and lineage tables.

**Architecture:** Keep direct lineage unchanged. Add `transitiveLineage` to `CdpWarehouseCatalogService`, loading active lineage edges once, applying tenant override semantics, then running a bounded breadth-first traversal with cycle detection and path output.

**Tech Stack:** Java 21, Spring Boot, MyBatis-Plus, JUnit 5, Mockito, AssertJ.

## Scope

- Add P2-043 spec, plan, and indexes.
- Add no Flyway migration.
- Extend catalog service with transitive traversal records.
- Extend catalog controller with a transitive lineage endpoint.
- Add focused service and controller tests.
- Verify compile and warehouse/BI/audience regression.

## Tasks

- [x] **Step 1: Add spec, plan, and indexes**

Create and index P2-043 docs.

- [x] **Step 2: Add transitive service traversal**

Implement tenant-aware edge loading, bounded BFS, depth/relation node metadata, path output, cycle warnings, and truncation.

- [x] **Step 3: Add controller endpoint**

Expose `/warehouse/catalog/datasets/{datasetKey}/lineage/transitive`.

- [x] **Step 4: Add tests and verify**

Run focused tests, compile, and warehouse/BI/audience regression.

## Verification

- [x] Focused tests passed: `mvn -pl canvas-engine -Dtest=CdpWarehouseCatalogServiceTest,CdpWarehouseCatalogControllerTest test` (`14 tests, 0 failures`).
- [x] Main compile passed: `mvn -pl canvas-engine -DskipTests compile`.
- [x] Warehouse/BI/audience regression passed: `mvn -pl canvas-engine -Dtest='CdpWarehouse*Test,Bi*Test,MarketingBi*Test,Doris*Test,AudienceMaterialization*Test,CdpOlapAudienceSchemaTest,StableUserIndexServiceTest,VersionedAudienceBitmapStoreTest,BehaviorAudienceRuleCompilerTest,AudienceQualityServiceTest,MyBatisAudienceDefinitionRepositoryTest' test` (`280 tests, 0 failures, 1 skipped`).
