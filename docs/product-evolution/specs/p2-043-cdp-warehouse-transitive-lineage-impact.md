# P2-043 - CDP Warehouse Transitive Lineage Impact Spec

Priority: P2
Sequence: 043
Source: `docs/product-evolution/specs/p2-027-cdp-warehouse-catalog-and-lineage.md`, `docs/product-evolution/specs/p2-041-cdp-warehouse-metric-lineage-and-impact.md`
Implementation plan: `../plans/p2-043-cdp-warehouse-transitive-lineage-impact-plan.md`

Status: Historical plan evidence records implementation and verification; runtime verification plus commit and merge status was not verified in this docs-only audit.

## Goal

Extend warehouse catalog lineage from direct edges to bounded transitive impact traversal so operators can answer which upstream sources feed a dataset and which downstream datasets may be affected by a change.

## Current Baseline

- P2-027 stores `cdp_warehouse_dataset_catalog` and direct `cdp_warehouse_lineage_edge`.
- `CdpWarehouseCatalogService#lineage` returns only direct upstream/downstream edges.
- P2-041 metric impact includes direct dataset lineage, charts, and dashboards, but explicitly excludes multi-hop transitive lineage.
- There is no API that summarizes multi-hop blast radius, path depth, cycles, or traversal truncation.

## In Scope

- Reuse existing catalog and lineage tables.
- Add bounded transitive traversal for upstream, downstream, and both directions.
- Include depth, relation, dataset nodes, lineage edges, and discovered paths.
- Guard cycles and excessive depth.
- Expose tenant-scoped API under the warehouse catalog surface.
- Add focused service and controller tests.

## Out Of Scope

- New Flyway migration.
- Replacing direct lineage API behavior.
- Graph database integration.
- Column-level lineage.
- UI.

## Runtime Semantics

1. Direct lineage remains unchanged.
2. Transitive lineage loads active lineage edges for tenant `0` and current tenant, with tenant rows overriding built-ins by `(upstream, downstream, transformRef)`.
3. Traversal starts at `datasetKey` with depth `0`.
4. `UPSTREAM` follows incoming edges, `DOWNSTREAM` follows outgoing edges, and `BOTH` follows both directions.
5. Traversal is breadth-first and bounded by `maxDepth`.
6. Default `maxDepth` is `3`; requests above the hard limit are capped.
7. Cycles are not expanded repeatedly and are reported as warnings.
8. Missing dataset catalog rows are returned as stub nodes, matching direct lineage behavior.

## Functional Requirements

1. Operators can request transitive lineage for current tenant, dataset key, direction, and optional max depth.
2. Response includes dataset nodes with relation (`SELF`, `UPSTREAM`, `DOWNSTREAM`, `BOTH`) and minimum depth.
3. Response includes lineage edges discovered during traversal.
4. Response includes paths from the target to impacted/source datasets.
5. Response reports `truncated=true` when traversal hits the depth cap while more edges exist.
6. Response warns on cycles and max-depth capping.
7. Tests prove upstream traversal, downstream traversal, both-direction cycle guard, tenant override behavior, controller tenant scoping, and no new schema requirement.

## Technical Scope

- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseCatalogService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseCatalogController.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseCatalogServiceTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/web/CdpWarehouseCatalogControllerTest.java`
- P2-043 spec, plan, and indexes.

## Acceptance Criteria

- No Flyway migration is added.
- Direct lineage tests continue to pass.
- Transitive lineage returns multi-hop upstream and downstream nodes/edges.
- Cycles do not create duplicate infinite paths.
- Max depth bounds traversal and reports truncation.
- Focused backend tests pass.
- Warehouse/BI/audience regression passes.

## Rollout

1. Deploy service/controller changes.
2. Keep existing direct lineage UI/API callers on the direct endpoint.
3. Use transitive endpoint for impact analysis, warehouse change review, and future UI graph views.

## Rollback

- Stop calling the transitive endpoint.
- Direct lineage, catalog, metric impact, and metric change review remain independent.
