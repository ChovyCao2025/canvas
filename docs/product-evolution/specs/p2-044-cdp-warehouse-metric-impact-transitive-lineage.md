# P2-044 - CDP Warehouse Metric Impact Transitive Lineage Spec

Priority: P2
Sequence: 044
Source: `docs/product-evolution/specs/p2-041-cdp-warehouse-metric-lineage-and-impact.md`, `docs/product-evolution/specs/p2-042-cdp-warehouse-metric-change-review-guard.md`, `docs/product-evolution/specs/p2-043-cdp-warehouse-transitive-lineage-impact.md`
Implementation plan: `../plans/p2-044-cdp-warehouse-metric-impact-transitive-lineage-plan.md`

## Goal

Connect semantic metric impact analysis and metric-change review to bounded transitive warehouse lineage, so metric owners can see multi-hop upstream sources and downstream warehouse blast radius before approving a metric contract change.

## Current Baseline

- P2-041 metric impact returns field dependencies, direct catalog lineage, BI charts, and BI dashboards.
- P2-042 metric change review stores an impact summary at request time.
- P2-043 catalog lineage now supports bounded transitive traversal with paths, depth, truncation, and cycle warnings.
- Metric impact and review still only summarize direct lineage counts.

## In Scope

- Add transitive lineage evidence to `CdpWarehouseMetricLineageService.MetricImpactView`.
- Reuse `CdpWarehouseCatalogService#transitiveLineage`; do not add another traversal implementation.
- Preserve direct lineage fields for existing callers.
- Add transitive counts and truncation flag to metric-change review impact summary.
- Include downstream transitive impact in metric review risk classification.
- Add focused service/controller tests and regression verification.

## Out Of Scope

- New Flyway migration.
- Column-level lineage.
- UI.
- Replacing P2-043 catalog transitive API.
- Changing BI query compile semantics.

## Runtime Semantics

1. Metric impact still validates dataset key and metric key first.
2. Direct lineage remains populated from `catalogService.lineage(..., BOTH)`.
3. Transitive lineage is populated from `catalogService.transitiveLineage(..., BOTH, null)`.
4. Optional catalog failures become warnings and do not block field, chart, or dashboard impact output.
5. Metric-change review snapshots include transitive node count, edge count, path count, downstream node count, and truncation.
6. Risk is `HIGH` when dashboards are impacted, transitive lineage is truncated, or downstream transitive nodes exist.
7. Risk is `MEDIUM` when charts, fields, direct lineage, or transitive lineage exist without downstream impact.

## Functional Requirements

1. Metric impact API response includes transitive lineage graph when catalog service is available.
2. Metric impact warnings include transitive lineage warnings, including max-depth capping or cycle detection.
3. Existing direct lineage fields remain present and compatible.
4. Metric-change review impact summary includes transitive counts and `transitiveTruncated`.
5. Metric-change review risk classification escalates to `HIGH` for downstream multi-hop blast radius or truncation.
6. Tests prove transitive evidence is included, optional failures warn instead of fail, review summary captures transitive counts, and no migration is added.

## Technical Scope

- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseMetricLineageService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseMetricChangeReviewService.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseMetricLineageServiceTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseMetricChangeReviewServiceTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/web/CdpWarehouseMetricLineageControllerTest.java`
- P2-044 spec, plan, and indexes.

## Acceptance Criteria

- No Flyway migration is added.
- Metric impact includes both direct and transitive lineage evidence.
- Metric impact collects transitive traversal warnings.
- Metric-change review stores transitive impact summary fields.
- Focused backend tests pass.
- Warehouse/BI/audience regression passes.

## Rollout

1. Deploy service changes after P2-043.
2. Use metric impact API as the source for pre-review blast-radius display.
3. Metric-change review automatically stores the expanded impact summary for new requests.

## Rollback

- Stop reading `transitiveLineage` from metric impact responses.
- Existing direct lineage, BI usage impact, and review rows remain valid.
