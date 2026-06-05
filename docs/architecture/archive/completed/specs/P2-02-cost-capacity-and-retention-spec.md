# Spec: Cost, Capacity, And Retention

Source package: `docs/architecture/reviewed-packages/p2/cost-capacity-and-retention/`

Coverage matrix: `docs/architecture/reviewed-packages/coverage-matrix.md`


## Verification Status

Confirmed for repository-controlled artifacts. The SLO model, capacity cliff model, retention policy, retention migration, metric names, dashboards, alerts, and non-publishable baseline record are implemented and verified in-repo. True production costs and publishable capacity numbers still require an external load run with valid `perf-guide report` or `distributed-report` PASS evidence.

## Problems Covered

- No explicit SLA/SLO targets.
- No resource and cost model tied to QPS, DAU, canvas count, execution count, trace retention, and Redis memory.
- Data retention strategy is incomplete for execution, trace, audit, and quota data.
- Some capacity limits exist, but not all are enforced or tied to alerting.

## Source Coverage

- `archive/reviews/architecture-supplement-review-2026-05.md`: cost architecture, performance capacity model, retention recommendations.
- `archive/remediation/part7-resilience.md`: capacity limits, full-table scans, bottlenecks, retention gaps.
- `archive/evolution/multi-datasource-isolation.md`: connection pool isolation and monitoring.

## Acceptance Criteria

- [x] SLA/SLO targets are written and measurable.
- [x] Capacity model maps QPS/executions/users to app, DB, Redis, MQ, and storage requirements.
- [x] Retention rules exist for execution, trace, audit, DLQ, and Redis quota keys.
- [x] Metrics and alerts exist for pool saturation, queue depth, Redis memory, trace drops, and execution failures.

## Implementation Evidence

- `docs/architecture/capacity/slo-and-capacity-model.md`
- `docs/architecture/capacity/retention-policy.md`
- `docs/architecture/capacity/dashboard-and-alerts.md`
- `docs/architecture/capacity/baseline-load-result.md`
- `backend/canvas-engine/src/main/resources/db/migration/V239__execution_retention_policy.sql`
- `docs/architecture/evidence/P2-02-cost-capacity-and-retention.md`
