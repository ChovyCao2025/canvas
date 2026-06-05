# Cost, Capacity, And Retention Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Turn the current capacity settings, execution data growth, and retention gaps into measurable SLOs, retention rules, dashboards, alerts, and a recorded baseline load result.

**Architecture:** Use the existing single backend deployable and local dependency stack. Capacity inputs come from `application.yml`, queue/request metrics, Hikari/Redis/RocketMQ settings, and the stress-test runbooks. Retention is enforced through new Flyway migrations and service tests for execution, trace, audit, quota, DLQ, and Redis key families.

**Tech Stack:** Java 21, Spring Boot Actuator, Micrometer Prometheus, HikariCP, MySQL 8/Flyway, Redis 7, RocketMQ 5.3.1, Maven, local Docker Compose, Markdown runbooks.

---

## Source Material

- Spec: `../specs/P2-02-cost-capacity-and-retention-spec.md`
- Source package: `../todo/p2/cost-capacity-and-retention/`
- Coverage matrix: `../todo/coverage-matrix.md`

## File Structure

- Read: `docs/architecture/specs/P2-02-cost-capacity-and-retention-spec.md`
- Read: `docs/architecture/todo/p2/cost-capacity-and-retention/plan.md`
- Read: `backend/canvas-engine/src/main/resources/application.yml`
- Read: `backend/canvas-engine/src/main/resources/application-prod.yml`
- Read: `docker-compose.local.yml`
- Read: `docs/stressTest/local-capacity-runbook.md`
- Read: `docs/stressTest/distributed-capacity-runbook.md`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/request/CanvasExecutionRequestBacklogMetrics.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/CanvasMetrics.java`
- Create: `docs/architecture/capacity/slo-and-capacity-model.md`
- Create: `docs/architecture/capacity/retention-policy.md`
- Create: `docs/architecture/capacity/baseline-load-result.md`
- Create: `backend/canvas-engine/src/main/resources/db/migration/V239__execution_retention_policy.sql`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/request/CanvasExecutionRequestBacklogMetricsTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/execution/PerfRunTrackingSchemaTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/execution/CanvasExecutionDlqSchemaTest.java`

### Task 1: Define product-facing SLA/SLO targets

**Files:**
- Create: `docs/architecture/capacity/slo-and-capacity-model.md`
- Read: `backend/canvas-engine/src/main/resources/application.yml`
- Read: `backend/canvas-engine/src/main/resources/application-prod.yml`

- [x] Define availability, trigger admission latency, execution completion latency, API p95 latency, trace durability, and notification delivery SLOs.
- [x] Tie each SLO to one current metric or name the metric to add.
- [x] Set an error-budget calculation window and alert severity for each SLO.

**Run:**
```bash
test -f docs/architecture/capacity/slo-and-capacity-model.md
rg "availability|trigger admission|execution completion|API p95|trace durability|error budget" docs/architecture/capacity/slo-and-capacity-model.md
```

**Expected:** The SLO document contains measurable targets, a metric source, and alert severity for each product-facing behavior.

### Task 2: Build a baseline capacity model from current config

**Files:**
- Modify: `docs/architecture/capacity/slo-and-capacity-model.md`
- Read: `backend/canvas-engine/src/main/resources/application.yml`
- Read: `docker-compose.local.yml`
- Read: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/request/CanvasExecutionRequestBacklogMetrics.java`

- [x] Extract current values for Hikari max pool, Redis pool, Disruptor ring buffer, execution concurrency, lane concurrency, queue limits, MQ consumer threads, scheduler trigger concurrency, and HTTP client limits.
- [x] Map QPS, DAU, canvas count, execution count, trace rows, Redis memory, MQ depth, and storage growth to the current limits.
- [x] Add a "capacity cliff" table that names the metric, limit, symptom, first mitigation, and owner.

**Run:**
```bash
rg "maximum-pool-size|max-active|ring-buffer-size|max-concurrency|queue-limit|consume-thread-number|trigger-concurrency" backend/canvas-engine/src/main/resources/application.yml
rg "QPS|DAU|Redis memory|MQ depth|capacity cliff" docs/architecture/capacity/slo-and-capacity-model.md
```

**Expected:** The capacity model traces every listed config limit to workload input, saturation symptom, and mitigation.

### Task 3: Add data retention policy for MySQL tables and Redis key families

**Files:**
- Create: `docs/architecture/capacity/retention-policy.md`
- Read: `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/redis/RedisKeyUtil.java`
- Read: `backend/canvas-engine/src/main/resources/db/migration/V4__dlq_table.sql`
- Read: `backend/canvas-engine/src/main/resources/db/migration/V46__canvas_execution_request.sql`
- Read: `backend/canvas-engine/src/main/resources/db/migration/V72__perf_run_tracking.sql`
- Read: `backend/canvas-engine/src/main/resources/db/migration/V73__audience_compute_run_tracking.sql`
- Read: `backend/canvas-engine/src/main/resources/db/migration/V78__saas_foundation.sql`

- [x] Define retention windows for `canvas_execution`, `canvas_execution_trace`, `canvas_execution_dlq`, `canvas_execution_request`, `canvas_execution_stats`, audit logs, quota keys, route keys, and context keys.
- [x] Identify which rows are deleted, archived, compacted, or kept for compliance evidence.
- [x] Define Redis TTL rules for context, quota, route, cache invalidation, and kill-switch key families.

**Run:**
```bash
test -f docs/architecture/capacity/retention-policy.md
rg "canvas_execution|canvas_execution_trace|canvas_execution_dlq|canvas_execution_request|Redis TTL|quota|audit" docs/architecture/capacity/retention-policy.md
cd backend && mvn test -pl canvas-engine -Dtest=PerfRunTrackingSchemaTest,CanvasExecutionDlqSchemaTest
```

**Expected:** Retention policy names every high-volume table and Redis key family, and current schema-focused tests still pass.

### Task 4: Add partition, archive, and delete strategy for high-volume tables

**Files:**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V239__execution_retention_policy.sql`
- Modify: `docs/architecture/capacity/retention-policy.md`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/execution/PerfRunTrackingSchemaTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/execution/CanvasExecutionDlqSchemaTest.java`

- [x] Add migration objects required by the retention policy, such as indexes, archive tables, retention markers, or cleanup metadata.
- [x] Do not edit existing migrations `V1` through `V238`; add `V239` because `V91` already exists in the current chain.
- [x] Add or update schema tests that assert the new retention objects exist.

**Run:**
```bash
test -f backend/canvas-engine/src/main/resources/db/migration/V239__execution_retention_policy.sql
cd backend && mvn test -pl canvas-engine -Dtest=PerfRunTrackingSchemaTest,CanvasExecutionDlqSchemaTest
```

**Expected:** New retention schema is introduced only through `V239__execution_retention_policy.sql`, and schema tests pass.

### Task 5: Add capacity dashboards and alerts

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/request/CanvasExecutionRequestBacklogMetrics.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/CanvasMetrics.java`
- Create: `docs/architecture/capacity/dashboard-and-alerts.md`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/request/CanvasExecutionRequestBacklogMetricsTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/trigger/TriggerPriorityConfigTest.java`

- [x] Ensure metrics exist for pool saturation, request backlog, lane saturation, queue depth, Redis memory, trace drops, DLQ growth, and execution failures.
- [x] Document dashboard panels with metric name, unit, threshold, alert owner, and runbook link.
- [x] Add metric tests for any new counter, gauge, or timer name.

**Run:**
```bash
cd backend && mvn test -pl canvas-engine -Dtest=CanvasExecutionRequestBacklogMetricsTest,TriggerPriorityConfigTest
rg "pool saturation|queue depth|Redis memory|trace drops|DLQ|execution failures" docs/architecture/capacity/dashboard-and-alerts.md
```

**Expected:** Dashboard documentation covers every metric in the spec, and backend metric tests pass.

### Task 6: Record baseline load readiness and non-publishable result status

**Files:**
- Create: `docs/architecture/capacity/baseline-load-result.md`
- Read: `docs/stressTest/local-capacity-runbook.md`
- Read: `docs/stressTest/distributed-capacity-runbook.md`
- Read: `docker-compose.local.yml`

- [x] Record local dependency versions from `docker-compose.local.yml`.
- [x] Verify the perf tooling preflight and backend pre-load unit guard.
- [x] Record QPS, p95 latency, execution failures, queue depth, Redis memory, DB pool saturation, CPU, memory, and bottleneck as `not measured`, because no runbook-valid `report`/`distributed-report` PASS artifact was produced.

**Run:**
```bash
cd backend && mvn test -pl canvas-engine -Dtest=CanvasExecutionRequestServiceTest,CanvasExecutionRequestExecutorTest
node --test tools/perf/*.test.mjs
node tools/perf/perf-guide.mjs doctor
test -f docs/architecture/capacity/baseline-load-result.md
rg "QPS|p95|execution failures|queue depth|Redis memory|DB pool|bottleneck" docs/architecture/capacity/baseline-load-result.md
```

**Expected:** Baseline evidence exists with command, timestamp, dependency versions, required fields, and an explicit rule that `not measured` values must not be used as capacity claims.

### Task 7: Handoff scoped cost, capacity, and retention changes

**Files:**
- Modify: `docs/architecture/plans/P2-02-cost-capacity-and-retention-plan.md`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/request/CanvasExecutionRequestBacklogMetrics.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/CanvasMetrics.java`
- Create: `backend/canvas-engine/src/main/resources/db/migration/V239__execution_retention_policy.sql`
- Create: `docs/architecture/capacity/slo-and-capacity-model.md`
- Create: `docs/architecture/capacity/retention-policy.md`
- Create: `docs/architecture/capacity/dashboard-and-alerts.md`
- Create: `docs/architecture/capacity/baseline-load-result.md`

- [x] Review only files named in this plan.
- [x] Record verification evidence for the plan, metrics, migration, tests, and capacity docs.
- [x] Leave staging and commit to the user-controlled handoff because the current worktree contains unrelated active changes.

**Run:**
```bash
git diff -- docs/architecture/plans/P2-02-cost-capacity-and-retention-plan.md docs/architecture/specs/P2-02-cost-capacity-and-retention-spec.md docs/architecture/evidence/P2-02-cost-capacity-and-retention.md backend/canvas-engine/src/main/java/org/chovy/canvas/engine/request/CanvasExecutionRequestBacklogMetrics.java backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/CanvasMetrics.java backend/canvas-engine/src/main/resources/db/migration/V239__execution_retention_policy.sql docs/architecture/capacity
```

**Expected:** The handoff contains only the scoped capacity, retention, metric, migration, test, and evidence files for this package; no commit is created automatically.
