# Observability And Ops Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add request/execution correlation, trace propagation, deployable Prometheus assets, and operational runbooks for canvas runtime operations.

**Architecture:** Put correlation ID handling at the WebFlux filter boundary, carry it through execution services and async workers with MDC-aware helpers, then commit dashboards, alert rules, and runbooks as deployable artifacts. Prefer Micrometer/actuator metrics already exposed by Spring Boot and add internal trace records only where cross-layer execution behavior is not otherwise observable.

**Tech Stack:** Java 21, Spring Boot WebFlux, Reactor, SLF4J MDC, Micrometer, Actuator Prometheus, Redis/RocketMQ operational runbooks, JUnit 5, AssertJ, Maven.

---

## Source Material

- Spec: `../specs/P1-04-observability-and-ops-spec.md`
- Source package: `../todo/p1/observability-and-ops/`
- Coverage matrix: `../todo/coverage-matrix.md`

## File Structure

- Filter: `backend/canvas-engine/src/main/java/org/chovy/canvas/config/CorrelationIdWebFilter.java`
- Response model: `backend/canvas-engine/src/main/java/org/chovy/canvas/common/R.java`
- Error handling: `backend/canvas-engine/src/main/java/org/chovy/canvas/config/GlobalExceptionHandler.java`
- Execution tracing: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trace/ExecutionTraceContext.java`
- Async MDC helper: `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/observability/MdcTaskDecorator.java`
- Metrics: `backend/canvas-engine/src/main/java/org/chovy/canvas/config/CanvasRuntimeMetrics.java`
- Tests: `backend/canvas-engine/src/test/java/org/chovy/canvas/config/CorrelationIdWebFilterTest.java`
- Tests: `backend/canvas-engine/src/test/java/org/chovy/canvas/config/GlobalExceptionHandlerTraceTest.java`
- Tests: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/trace/ExecutionTraceContextTest.java`
- Tests: `backend/canvas-engine/src/test/java/org/chovy/canvas/infrastructure/observability/MdcTaskDecoratorTest.java`
- Tests: `backend/canvas-engine/src/test/java/org/chovy/canvas/config/CanvasRuntimeMetricsTest.java`
- Ops asset: `deploy/observability/prometheus/canvas-alert-rules.yml`
- Ops asset: `deploy/observability/grafana/canvas-runtime-dashboard.json`
- Runbook: `docs/architecture/runbooks/dlq-handling.md`
- Runbook: `docs/architecture/runbooks/route-rebuild.md`
- Runbook: `docs/architecture/runbooks/cache-invalidation.md`
- Runbook: `docs/architecture/runbooks/shutdown-drain.md`
- Evidence: `docs/architecture/evidence/P1-04-observability-and-ops.md`

### Task 1: Add request and execution correlation ID propagation

**Files:**
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/config/CorrelationIdWebFilter.java`
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/common/R.java`
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/config/GlobalExceptionHandler.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/config/CorrelationIdWebFilterTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/config/GlobalExceptionHandlerTraceTest.java`
- Evidence: `docs/architecture/evidence/P1-04-observability-and-ops.md`

- [x] Create `CorrelationIdWebFilter` that reads `X-Correlation-Id`, generates one when absent, stores it in Reactor context and MDC, and writes it back to the response header.
- [x] Add a public `traceId` or `correlationId` field to the shared error response path without changing success payload semantics.
- [x] Add tests proving incoming IDs are preserved, missing IDs are generated, and generic error responses include the public correlation ID without leaking exception messages.

Run:

```bash
cd backend && mvn -pl canvas-engine -Dtest=CorrelationIdWebFilterTest,GlobalExceptionHandlerTraceTest test
# Do not stage or commit in this session unless the user explicitly asks.
```

Expected: filter and error tests pass; logs and client errors have a stable correlation ID.

### Task 2: Preserve MDC across async and execution paths

**Files:**
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/observability/MdcTaskDecorator.java`
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trace/ExecutionTraceContext.java`
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/CanvasExecutionService.java`
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/GroovyHandler.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/infrastructure/observability/MdcTaskDecoratorTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/trace/ExecutionTraceContextTest.java`

- [x] Create an MDC task decorator/helper that captures the current MDC map and restores it around bounded async tasks.
- [x] Add execution trace context fields for `executionId`, `canvasId`, `nodeId`, and `correlationId`.
- [x] Apply the helper to execution and handler async boundaries named in the spec evidence.

Run:

```bash
cd backend && mvn -pl canvas-engine -Dtest=MdcTaskDecoratorTest,ExecutionTraceContextTest test
# Do not stage or commit in this session unless the user explicitly asks.
```

Expected: async tests prove MDC values survive worker boundaries and are cleared after task completion.

### Task 3: Add runtime metrics, alert rules, and dashboard assets

**Files:**
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/config/CanvasRuntimeMetrics.java`
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/CanvasExecutionService.java`
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/redis/MqRouteRefreshService.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/config/CanvasRuntimeMetricsTest.java`
- Ops asset: `deploy/observability/prometheus/canvas-alert-rules.yml`
- Ops asset: `deploy/observability/grafana/canvas-runtime-dashboard.json`

- [x] Register Micrometer gauges/counters for execution failures, DLQ backlog, route rebuild failures, cache invalidation failures, Hikari, Redis, MQ, lane pressure, and disruptor pressure.
- [x] Create Prometheus alert rules for sustained execution failure rate, DLQ backlog, route rebuild failure, Redis/MQ unavailable, and graceful shutdown drain timeout.
- [x] Create a Grafana dashboard JSON that references the alert metrics and actuator Prometheus names.

Run:

```bash
cd backend && mvn -pl canvas-engine -Dtest=CanvasRuntimeMetricsTest test
test -f deploy/observability/prometheus/canvas-alert-rules.yml
test -f deploy/observability/grafana/canvas-runtime-dashboard.json
rg -n "CanvasExecutionFailureRate|CanvasDlqBacklog|route_rebuild|cache_invalidation|shutdown_drain" deploy/observability
# Do not stage or commit in this session unless the user explicitly asks.
```

Expected: metrics tests pass, alert/dashboard files exist, and the named operational signals are present in deployable assets.

### Task 4: Add operational runbooks

**Files:**
- Runbook: `docs/architecture/runbooks/dlq-handling.md`
- Runbook: `docs/architecture/runbooks/route-rebuild.md`
- Runbook: `docs/architecture/runbooks/cache-invalidation.md`
- Runbook: `docs/architecture/runbooks/shutdown-drain.md`
- Evidence: `docs/architecture/evidence/P1-04-observability-and-ops.md`

- [x] Write DLQ handling steps with diagnosis command, replay command, rollback/stop condition, owner, and evidence capture path.
- [x] Write route rebuild and cache invalidation runbooks with exact ops endpoint or CLI command, auth requirement, verification command, and rollback note.
- [x] Write shutdown/drain runbook with pre-drain checks, active execution checks, timeout policy, and post-drain validation.

Run:

```bash
rg -n "Owner|Command|Verify|Rollback|Evidence" docs/architecture/runbooks/dlq-handling.md docs/architecture/runbooks/route-rebuild.md docs/architecture/runbooks/cache-invalidation.md docs/architecture/runbooks/shutdown-drain.md
# Do not stage or commit in this session unless the user explicitly asks.
```

Expected: each runbook includes owner, command, verification, rollback, and evidence sections.

### Task 5: Validate observability package end to end

**Files:**
- Evidence: `docs/architecture/evidence/P1-04-observability-and-ops.md`
- Plan: `docs/architecture/plans/P1-04-observability-and-ops-plan.md`
- Spec: `docs/architecture/specs/P1-04-observability-and-ops-spec.md`

- [x] Run focused observability tests.
- [x] Confirm actuator Prometheus remains enabled and alert/dashboard assets are discoverable.
- [x] Record test output, dashboard paths, runbook paths, and remaining tracing decisions in the evidence file.

Run:

```bash
cd backend && mvn -pl canvas-engine -Dtest=CorrelationIdWebFilterTest,GlobalExceptionHandlerTraceTest,MdcTaskDecoratorTest,ExecutionTraceContextTest,CanvasRuntimeMetricsTest test
rg -n "prometheus|metrics|health" backend/canvas-engine/src/main/resources/application.yml
rg -n "P1-04|correlation|dashboard|runbook|remaining" docs/architecture/evidence/P1-04-observability-and-ops.md
# Do not stage or commit in this session unless the user explicitly asks.
```

Expected: focused backend tests pass, Prometheus/metrics exposure is still configured, and evidence records deployed assets plus any deferred tracing decision.
