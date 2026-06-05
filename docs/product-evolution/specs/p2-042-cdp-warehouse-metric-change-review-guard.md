# P2-042 - CDP Warehouse Metric Change Review Guard Spec

Priority: P2
Sequence: 042
Source: `docs/product-evolution/specs/p2-040-cdp-warehouse-semantic-metric-contracts.md`, `docs/product-evolution/specs/p2-041-cdp-warehouse-metric-lineage-and-impact.md`
Implementation plan: `../plans/p2-042-cdp-warehouse-metric-change-review-guard-plan.md`

## Goal

Add a production guard for semantic metric changes so operators can propose expression and allowed-dimension edits, capture impact evidence, require reviewer approval, and only then apply the change to the persisted BI metric.

## Current Baseline

- P2-040 stores semantic metric contracts in `bi_metric` and enforces `allowed_dimensions_json` at BI query compile time.
- P2-041 exposes read-only metric impact analysis for field dependencies, direct lineage, charts, and dashboards.
- Existing BI audit logs record access decisions but do not hold a pending metric-change workflow.
- Existing manual approval tables are canvas-runtime approval records, not tenant-scoped BI metric governance.

## In Scope

- Add a tenant-scoped metric change review table.
- Store current metric snapshot, proposed metric snapshot, and P2-041 impact summary at request time.
- Block direct metric mutation through the new workflow until a reviewer approves the request.
- Apply approved changes to the existing `bi_metric` row without duplicating the BI metric schema.
- Provide APIs to request, list, approve, reject, and apply metric changes.
- Add schema, service, and controller tests.

## Out Of Scope

- Replacing the existing BI dataset resource API.
- Full UI.
- Multi-step external approval engine integration.
- Editing built-in in-code metrics that do not have a persisted tenant `bi_metric` row.
- Full SQL parsing beyond the existing metric-expression validation and impact extraction.

## Runtime Semantics

1. A metric change request is created with `PENDING_REVIEW` status.
2. Request creation validates that the dataset and metric exist and that proposed allowed dimensions are dataset dimensions.
3. Request creation captures current metric details, proposed details, and current impact evidence.
4. Approval requires reviewer identity and review note, then changes status to `APPROVED`.
5. Rejection requires reviewer identity and review note, then changes status to `REJECTED`.
6. Applying a request is allowed only after approval and updates the matching tenant `bi_metric` row.
7. Applying a request marks it `APPLIED` and records `applied_at`.
8. Requests for built-in metrics without a persisted tenant metric row may be reviewed but cannot be applied until the dataset is persisted.

## Functional Requirements

1. Operators can create a metric change request for `datasetKey`, `metricKey`, proposed expression, proposed allowed dimensions, and reason.
2. The service must reject missing reason, missing proposed expression, unknown metrics, and non-dimension allowed fields.
3. Request views include current snapshot, proposed snapshot, impact counts, risk level, status, requester, reviewer, and timestamps.
4. Risk level is `HIGH` when impacted dashboards exist, `MEDIUM` when impacted charts or field dependencies exist, and `LOW` otherwise.
5. Approval and rejection are tenant-scoped and cannot operate on other tenant rows.
6. Applying a request updates only expression and allowed dimensions for the target `bi_metric` row.
7. Applying without approval, applying rejected requests, and applying non-persisted metrics must fail closed.

## Technical Scope

- `backend/canvas-engine/src/main/resources/db/migration/V202__cdp_warehouse_metric_change_review.sql`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpWarehouseMetricChangeReviewDO.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpWarehouseMetricChangeReviewMapper.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseMetricChangeReviewService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseMetricChangeReviewController.java`
- Focused tests under `backend/canvas-engine/src/test/java/org/chovy/canvas`.

## Acceptance Criteria

- Schema tests prove the review table and tenant/status indexes exist.
- Service tests prove request validation, impact capture, risk calculation, approval/rejection, and approved apply behavior.
- Controller tests prove tenant-scoped request, list, approve, reject, and apply APIs.
- Focused backend tests pass.
- Existing warehouse/BI/audience regression passes.

## Rollout

1. Deploy the additive migration.
2. Route semantic metric edits through `/warehouse/metric-change-reviews`.
3. Keep existing BI query behavior unchanged until an approved request is applied.
4. Use P2-041 impact output in operator UI when the frontend is added.

## Rollback

- Stop calling the review APIs and continue using the existing BI dataset APIs.
- Leave review rows for audit; no runtime query path depends on them.
