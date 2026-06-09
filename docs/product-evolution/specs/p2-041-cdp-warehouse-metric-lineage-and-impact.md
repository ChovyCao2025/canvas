# P2-041 - CDP Warehouse Metric Lineage And Impact Spec

Priority: P2
Sequence: 041
Source: `docs/product-evolution/specs/p2-027-cdp-warehouse-catalog-and-lineage.md`, `docs/product-evolution/specs/p2-040-cdp-warehouse-semantic-metric-contracts.md`
Implementation plan: `../plans/p2-041-cdp-warehouse-metric-lineage-and-impact-plan.md`

Status: Historical plan evidence records implementation and verification; runtime verification plus commit and merge status was not verified in this docs-only audit.

## Goal

Add read-only metric lineage and impact analysis so operators can see which warehouse fields, dataset lineage edges, BI charts, and BI dashboards depend on a semantic metric before changing it.

## Current Baseline

- P2-027 stores direct dataset lineage.
- P2-040 exposes semantic metric contracts and enforces metric allowed dimensions.
- BI chart and dashboard resources already persist query metrics and dimensions.
- Operators still cannot answer "what breaks if this metric changes" from one API.

## In Scope

- Reuse existing dataset lineage, semantic metric contracts, chart queries, and dashboard widgets.
- Add a read-only service that calculates metric field dependencies and downstream BI usage.
- Add a tenant-scoped API for metric impact.
- Return warnings for unavailable optional sources instead of failing the whole impact scan.
- Add focused service and controller tests.

## Out Of Scope

- New schema or Flyway migration.
- Full SQL expression parser.
- Multi-hop transitive lineage.
- Stored impact snapshots.
- UI.

## Runtime Semantics

1. Metric impact requires a valid dataset key and metric key.
2. Field dependencies are inferred from metric expression references to dataset fields.
3. Allowed dimensions are reported as dimension dependencies.
4. Dataset lineage is read from `CdpWarehouseCatalogService`.
5. BI charts are impacted when their query uses the target metric on the target dataset.
6. BI dashboards are impacted when any widget uses the target metric on the dashboard dataset.
7. Optional source failures are collected in `warnings`.

## Functional Requirements

1. Operators can request metric impact for current tenant, dataset key, and metric key.
2. Response includes metric expression, value type, allowed dimensions, and field dependencies.
3. Response includes direct dataset lineage nodes and edges when available.
4. Response includes impacted BI charts.
5. Response includes impacted BI dashboards and widget keys.
6. Tests prove field dependency extraction, BI usage detection, controller tenant scoping, and no migration requirement.

## Technical Scope

- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseMetricLineageService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseMetricLineageController.java`
- Tests under `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse` and `backend/canvas-engine/src/test/java/org/chovy/canvas/web`.

## Acceptance Criteria

- No Flyway migration is added.
- Unknown metric is rejected before impact scan.
- Impact includes referenced metric expression fields and allowed dimensions.
- Impact includes charts and dashboards using the metric.
- Focused backend tests pass.
- Warehouse/BI/audience regression passes.

## Rollout

1. Deploy the read-only endpoint.
2. Use it before editing semantic metric expressions or allowed dimensions.
3. Add UI later once API behavior stabilizes.

## Rollback

- Stop calling the impact API.
- Existing metric, BI, and catalog APIs remain independent.
