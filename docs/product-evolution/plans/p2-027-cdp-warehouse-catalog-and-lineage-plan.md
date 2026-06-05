# CDP Warehouse Catalog And Lineage Implementation Plan

**Goal:** Add tenant-scoped warehouse catalog and direct lineage metadata for CDP, audience, and BI datasets.

**Architecture:** Add metadata-only MySQL tables and a service/controller layer. Runtime data movement remains in P2-022 through P2-026; this slice makes those assets discoverable and traceable.

**Tech Stack:** Java 21, Spring Boot WebFlux, MyBatis-Plus, Flyway, MySQL, JUnit 5, Mockito, AssertJ.

## Current State

- Warehouse jobs and quality checks exist.
- BI compiler has a registry, but warehouse assets are not cataloged in persistent metadata.
- Operators lack a lineage graph from ODS to DWD/DWS/BI assets.

## Desired State

- Built-in warehouse datasets are listed in a catalog.
- Direct lineage edges are persisted.
- Operators can list datasets and request direct lineage graphs through APIs.

## Implementation Tasks

### Task 1: Register P2-027 Spec And Plan

- [x] **Step 1: Create spec and plan**

Create:
- `docs/product-evolution/specs/p2-027-cdp-warehouse-catalog-and-lineage.md`
- `docs/product-evolution/plans/p2-027-cdp-warehouse-catalog-and-lineage-plan.md`

- [x] **Step 2: Update indexes**

Update:
- `docs/product-evolution/specs/INDEX.md`
- `docs/product-evolution/plans/INDEX.md`
- `docs/product-evolution/IMPLEMENTATION_ORDER.md`

### Task 2: Catalog Schema

- [x] **Step 1: Write failing schema test**

Create `CdpWarehouseCatalogSchemaTest` proving:
- `cdp_warehouse_dataset_catalog` exists;
- `cdp_warehouse_lineage_edge` exists;
- uniqueness and indexes exist;
- built-in ODS/DWD/DWS/BI seed rows and lineage edges exist.

- [x] **Step 2: Add migration and DAL objects**

Create:
- `V194__cdp_warehouse_catalog_lineage.sql`;
- `CdpWarehouseDatasetCatalogDO`;
- `CdpWarehouseLineageEdgeDO`;
- mappers for both tables.

### Task 3: Catalog Service

- [x] **Step 1: Write failing service tests**

Create `CdpWarehouseCatalogServiceTest` covering:
- dataset upsert inserts when missing;
- dataset upsert updates when existing;
- listing applies tenant and layer filters;
- lineage graph includes direct upstream/downstream edges and nodes.

- [x] **Step 2: Implement service**

Create `CdpWarehouseCatalogService` with:
- `upsertDataset(Long tenantId, DatasetCommand command)`;
- `createLineageEdge(Long tenantId, LineageCommand command)`;
- `listDatasets(Long tenantId, String layer, String status)`;
- `lineage(Long tenantId, String datasetKey, Direction direction)`.

### Task 4: Controller

- [x] **Step 1: Write failing controller tests**

Create `CdpWarehouseCatalogControllerTest` for:
- `GET /warehouse/catalog/datasets`;
- `POST /warehouse/catalog/datasets`;
- `POST /warehouse/catalog/lineage`;
- `GET /warehouse/catalog/datasets/{datasetKey}/lineage`.

- [x] **Step 2: Implement controller**

Create `CdpWarehouseCatalogController` with tenant context resolution and bounded elastic wrapping.

### Task 5: Verification

- [x] **Step 1: Run focused tests**

```bash
cd backend && JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH mvn -pl canvas-engine test -Dtest=CdpWarehouseCatalogSchemaTest,CdpWarehouseCatalogServiceTest,CdpWarehouseCatalogControllerTest,CdpWarehouseQualitySchemaTest,CdpWarehouseQualityServiceTest,CdpWarehouseQualitySchedulerTest,CdpWarehouseQualityControllerTest,BiQueryControllerTest
```

- [x] **Step 2: Inspect changed files**

Check `git status --short` and leave unrelated dirty files untouched.

## Acceptance Checklist

- [x] P2-027 spec and plan are indexed.
- [x] Catalog and lineage tables are additive and tenant scoped.
- [x] Built-in ODS/DWD/DWS/BI catalog rows exist.
- [x] Built-in lineage edges exist.
- [x] Service supports upsert, listing, and direct lineage graph.
- [x] Controller exposes tenant-scoped catalog APIs.
- [x] Focused tests pass.
