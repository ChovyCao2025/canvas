# P2-027 - CDP Warehouse Catalog And Lineage Spec

Priority: P2
Sequence: 027
Source: `docs/product-evolution/specs/p2-023-bi-dataset-query-compiler-foundation.md`, `docs/product-evolution/specs/p2-026-cdp-warehouse-quality-and-reconciliation.md`, `docs/product-evolution/archive/2026-06-03/product-evolution-directions-2026-05-31.md`
Implementation plan: `../plans/p2-027-cdp-warehouse-catalog-and-lineage-plan.md`

## Goal

Add a warehouse catalog and lineage foundation so CDP, audience, and BI datasets are discoverable, governable, and traceable across ODS, DWD, DWS, ADS, and BI layers.

## Current Baseline

- P2-021 through P2-026 establish CDP OLAP materialization, warehouse ingestion, job operations, retry, and quality checks.
- BI query compilation has a registered dataset foundation, but it does not expose warehouse table ownership, freshness SLA, PII classification, or upstream/downstream lineage.
- Operators cannot currently answer which warehouse assets feed an audience or BI dataset without reading code/specs.

## In Scope

- A MySQL warehouse dataset catalog table.
- A MySQL lineage edge table.
- Built-in metadata for CDP ODS/DWD/DWS datasets used by P2-021 through P2-026.
- Service APIs for upserting datasets, creating lineage edges, listing datasets, and reading direct lineage graphs.
- Operator controller endpoints for catalog and lineage.
- Tests for schema, built-in seed coverage, upsert/list behavior, lineage graph assembly, and tenant-scoped controller delegation.

## Out Of Scope

- Column-level lineage parsing.
- Automatic SQL parser or DAG parser.
- External data catalog integrations.
- Permission enforcement beyond tenant scope.
- Flink CDC deployment.

## Architecture

P2-027 adds metadata-only governance tables. These tables do not affect ingestion, retries, backfill, aggregation, or query execution. They describe datasets and relationships already introduced by earlier warehouse specs.

```text
Warehouse runtime tables
    |
    v
cdp_warehouse_dataset_catalog
    |
    +--> cdp_warehouse_lineage_edge
            |
            +--> API lineage graph for BI, audience, and operations surfaces
```

## Runtime Semantics

1. Catalog rows are tenant scoped, with tenant `0` reserved for built-in shared warehouse assets.
2. Dataset keys are unique per tenant.
3. Lineage edges are tenant scoped and can point from built-in datasets to tenant datasets.
4. Listing datasets can filter by layer and status.
5. Lineage graph calls return direct upstream, downstream, or both directions.
6. The catalog is metadata-only and must not block data warehouse execution.

## Functional Requirements

1. Built-in catalog rows must cover CDP ODS event logs, DWD user event facts, DWS daily user metrics, and the BI canvas daily stats dataset.
2. Built-in lineage must connect CDP ODS to DWD and DWD to DWS.
3. Dataset catalog rows must preserve layer, table name, display name, subject area, owner, description, SLA, PII level, status, schema JSON, and source system.
4. Lineage edges must preserve upstream/downstream dataset keys, transform type, transform ref, dependency type, description, and active state.
5. Controller APIs must resolve current tenant and fall back to tenant `0` when no tenant context exists.
6. Service methods must be unit-testable without a live database.

## Technical Scope

- `backend/canvas-engine/src/main/resources/db/migration/V194__cdp_warehouse_catalog_lineage.sql`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpWarehouseDatasetCatalogDO.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpWarehouseLineageEdgeDO.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpWarehouseDatasetCatalogMapper.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpWarehouseLineageEdgeMapper.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseCatalogService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseCatalogController.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseCatalogSchemaTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseCatalogServiceTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/web/CdpWarehouseCatalogControllerTest.java`

## Acceptance Criteria

- Schema test proves catalog and lineage tables, uniqueness, indexes, and built-in seed rows exist.
- Service tests prove dataset upsert inserts or updates by tenant/key.
- Service tests prove dataset listing applies tenant and layer filters.
- Service tests prove lineage graph returns direct upstream and downstream nodes/edges.
- Controller tests prove tenant-scoped catalog list, dataset upsert, edge creation, and lineage graph endpoints.
- Focused backend tests pass for P2-027 plus P2-023/P2-026 warehouse regressions.

## Rollout

1. Deploy metadata migration.
2. Verify built-in catalog rows exist for ODS/DWD/DWS/BI assets.
3. Expose catalog APIs to operators and BI surfaces.
4. Add tenant-specific datasets only after owner, SLA, and PII values are reviewed.

## Rollback

- Hide catalog endpoints or stop calling them.
- Leave metadata tables in place; they do not affect runtime warehouse execution.
