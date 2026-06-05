# Spec: Data Platform Architecture

Source package: `docs/architecture/reviewed-packages/p3/platform-evolution/`

Coverage matrix: `docs/architecture/reviewed-packages/coverage-matrix.md`

## Verification Status

Implemented as a data-platform proof-of-concept definition and governance package. Full data platform rollout remains deferred; the selected thin slice is audience compute history. Evidence and contracts are in `docs/architecture/evidence/p3-03-data-platform.md`, `docs/architecture/work-products/p3-03-data-platform/data-platform-source-inventory.md`, `docs/architecture/work-products/p3-03-data-platform/data-platform-poc-plan.md`, and `docs/architecture/work-products/p3-03-data-platform/data-platform-contract-governance.md`.

## Source Documents

- `docs/architecture/archive/evolution/data-platform-architecture.md`
- `docs/architecture/archive/evolution/target-architecture-overview.md`
- `docs/architecture/archive/evolution/architect-critical-review.md`

## Scope

Define a data platform for analytics, audience computation, event history, and governance without coupling OLTP canvas execution to OLAP workloads. This must follow the boundary review in `P3-00-architecture-boundary-review-spec.md`: data platform work starts from a thin business slice and must not drive online-domain service boundaries.

Candidate capabilities:

- operational data capture from canvas, execution, event, CDP, and reach domains;
- CDC or event-stream ingestion;
- warehouse layering for raw, cleaned, modeled, and serving data;
- OLAP serving for reports and dashboards;
- data governance, lineage, retention, and PII controls;
- APIs for product analytics and audience segmentation.

## Acceptance Criteria

- Business use cases are prioritized before infrastructure is selected.
- Source systems, data ownership, ingestion method, freshness, retention, and SLA are documented.
- CDC/event contracts include schema versioning and replay behavior.
- PII classification, masking, retention, and deletion workflows are included.
- A small proof of concept is defined before committing to Flink, ClickHouse, or another platform component.
