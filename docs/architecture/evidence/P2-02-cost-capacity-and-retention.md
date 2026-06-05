# P2-02 Cost, Capacity, And Retention Evidence

Date: 2026-06-05

## Scope

Implemented and verified repository-controlled SLO, capacity, retention, dashboard, alert, migration, and baseline evidence artifacts for P2-02. No publishable production capacity number was created because a valid runbook capacity claim requires `perf-guide report` or `distributed-report` PASS artifacts.

## Implemented Artifacts

- `docs/architecture/capacity/slo-and-capacity-model.md` defines measurable SLOs, current capacity inputs, workload mapping, and capacity cliffs.
- `docs/architecture/capacity/retention-policy.md` defines MySQL retention/archive/delete policy and Redis TTL rules.
- `docs/architecture/capacity/dashboard-and-alerts.md` maps capacity metrics to dashboard panels, thresholds, owners, and runbooks.
- `docs/architecture/capacity/baseline-load-result.md` records the non-publishable baseline status, required result fields, dependency versions, and acceptance rule.
- `backend/canvas-engine/src/main/resources/db/migration/V239__execution_retention_policy.sql` adds retention policy schema through the next available migration version.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/CanvasMetrics.java` exposes capacity gauges/counters for pool saturation, lane saturation, queue depth, Redis memory, DLQ backlog, and trace drops.
- Schema and metrics tests assert the new migration and metric names.

## Versioning Decision

The source plan originally referenced `V91__execution_retention_policy.sql`, but the current repository already contains migrations through `V238`. The retention migration was implemented as `V239__execution_retention_policy.sql` to avoid Flyway version collisions and to preserve existing migration history.

## Verification

| Command | Result | Notes |
| --- | --- | --- |
| `cd backend && mvn -pl canvas-engine -am -P integration-tests -Dapi.version=1.54 -Dtest=CanvasExecutionRequestServiceIdempotencyIntegrationTest clean test` | Passed | 1 test, 0 failures; Testcontainers MySQL applied 149 migrations to `v239`. |
| `cd backend && mvn test -pl canvas-engine -Dtest=CanvasExecutionRequestBacklogMetricsTest,TriggerPriorityConfigTest,CanvasMetricsTest,PerfRunTrackingSchemaTest,CanvasExecutionDlqSchemaTest,CanvasExecutionRequestServiceTest,CanvasExecutionRequestExecutorTest` | Passed | 17 tests, 0 failures. |
| `bash scripts/release/check-flyway-migration.sh` | Passed | Baseline `V185`, highest `V239`, 149 migrations, 30 new migrations, high-risk notes present. |
| `node --test tools/perf/*.test.mjs` | Passed | 153 tests, 0 failures; perf tooling, report gates, and verifier behavior covered. |
| `node tools/perf/perf-guide.mjs doctor` | Passed | Returned `{ "status": "PASS" }`. |
| Capacity doc grep checks | Passed | SLO, capacity, retention, dashboard, alert, and baseline keywords present. |

## Residual Manual Items

- Production cost numbers require real traffic, infrastructure pricing, and storage growth data.
- Publishable QPS, p95, bottleneck, and capacity claims require a full local or distributed runbook execution with retained PASS artifacts.
- No commit was created; the working tree contains unrelated active changes and staging/commit should remain user-controlled.
