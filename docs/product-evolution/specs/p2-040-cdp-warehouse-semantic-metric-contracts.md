# P2-040 - CDP Warehouse Semantic Metric Contracts Spec

Priority: P2
Sequence: 040
Source: `docs/product-evolution/specs/p2-023-bi-dataset-query-compiler-foundation.md`, `docs/product-evolution/specs/p2-027-cdp-warehouse-catalog-and-lineage.md`, `backend/canvas-engine/src/main/resources/db/migration/V216__bi_platform_foundation.sql`
Implementation plan: `../plans/p2-040-cdp-warehouse-semantic-metric-contracts-plan.md`

Status: Historical plan evidence records implementation and verification; runtime verification plus commit and merge status was not verified in this docs-only audit.

## Goal

Promote BI metrics from query-time expressions into enforceable semantic contracts for warehouse analytics, without duplicating the existing BI metric schema.

## Current Baseline

- V216 already creates `bi_metric` with expression, aggregation, data type, owner, description, status, and `allowed_dimensions_json`.
- `BiDatasetResourceService` already persists and resolves BI dataset fields and metrics.
- `BiQueryCompiler` currently validates that requested metric keys exist, but does not enforce metric-level allowed dimensions.
- Warehouse operators do not have a warehouse-oriented API for inspecting semantic metric contracts and their dimension constraints.

## In Scope

- Reuse existing `bi_metric` and `BiDatasetSpec`.
- Extend runtime `BiMetricSpec` to carry allowed dimensions.
- Enforce metric allowed dimensions during BI query compilation.
- Add a warehouse semantic metric service and API that exposes metrics per dataset.
- Ensure built-in marketing BI metrics also carry allowed dimensions.
- Add focused service, controller, compiler, and regression tests.

## Out Of Scope

- New metric table or Flyway migration.
- Replacing the existing BI dataset resource API.
- Metric approval workflow.
- Metric lineage beyond direct field/dimension references.
- UI.

## Runtime Semantics

1. Empty `allowedDimensions` means the metric is compatible with all dataset dimensions.
2. Non-empty `allowedDimensions` restricts the metric to those dimensions only.
3. A query using an incompatible dimension/metric pair fails at compile time before SQL execution.
4. Semantic metric listing is tenant-scoped and resolves tenant overrides through the existing `BiDatasetSpecResolver`.
5. Archived metrics are excluded because `BiDatasetResourceService` already excludes archived metrics from runtime specs.

## Functional Requirements

1. Operators can list semantic metric contracts for a tenant and optional dataset.
2. Metric contract output includes dataset key, metric key, expression, value type, allowed dimensions, owner/source metadata where available.
3. BI query compilation rejects incompatible dimension/metric pairs.
4. Built-in `canvas_daily_stats` metrics expose allowed dimensions.
5. Tests prove compiler enforcement, API tenant scoping, and no new schema is required.

## Technical Scope

- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/BiMetricSpec.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/BiQueryCompiler.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/MarketingBiDatasetRegistry.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dataset/BiDatasetResourceService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseSemanticMetricService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseSemanticMetricController.java`
- Focused tests under `backend/canvas-engine/src/test/java/org/chovy/canvas`.

## Acceptance Criteria

- No Flyway migration is added.
- `BiQueryCompiler` rejects a metric when the requested dimensions are outside `allowedDimensions`.
- Runtime dataset specs preserve allowed dimensions from persisted BI metric resources.
- Warehouse semantic metric API lists contracts for current tenant.
- Focused backend tests pass.
- Warehouse/BI/audience regression passes.

## Rollout

1. Deploy runtime enforcement with built-in dimensions aligned to existing seeded BI metrics.
2. Verify existing dashboards and query presets use compatible dimensions.
3. Use semantic metric API as the source for operator-facing metric catalogs.

## Rollback

- Clear `allowed_dimensions_json` for problematic persisted metrics.
- Built-in metric restrictions can be relaxed by setting empty allowed dimension lists in the registry.
