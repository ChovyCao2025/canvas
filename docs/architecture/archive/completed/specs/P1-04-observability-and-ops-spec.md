# Spec: Observability And Ops

Source package: `docs/architecture/todo/p1/observability-and-ops/`

Coverage matrix: `docs/architecture/todo/coverage-matrix.md`


## Verification Status

Implemented and verified on 2026-06-04.

## Problems

- Logback is configured to include MDC fields such as `traceId`, but current code does not populate them.
- There is no visible Micrometer tracing/Zipkin/OTel integration in code or POM.
- Prometheus actuator endpoints exist, but alerting rules and full infrastructure scrape config are documentation-only.
- HikariCP, Redis, MQ, lane, disruptor, and execution failure metrics need production dashboards and alerts.
- Production deployment checklist exists, but it is not the same as executable deployment assets.

## Evidence

- `logback-spring.xml:15-16`
- `GlobalExceptionHandler.java` Javadoc mentions `traceId`, but `R` responses do not carry it.
- `application.yml:159-166` exposes health/info/prometheus/metrics.
- No `MDC.put`, `Tracer`, `Span`, or tracing dependencies were found in current main code.
- `docs/architecture/archive/reviews/production-deployment-checklist-2026-06-02.md` is checklist material, not deployed config.

## Acceptance Criteria

- Each request/execution has a correlation ID visible in logs and error responses.
- Cross-layer tracing exists for trigger -> execution -> handler -> external call.
- Prometheus rules and dashboards exist as committed deployable assets.
- Operational runbooks cover cache invalidation, route rebuild, DLQ handling, and shutdown/drain.

## Implementation Evidence

- Correlation/filter code: `backend/canvas-engine/src/main/java/org/chovy/canvas/config/CorrelationIdWebFilter.java`
- Execution MDC scope: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trace/ExecutionTraceContext.java`
- Async MDC helper: `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/observability/MdcTaskDecorator.java`
- Runtime metrics: `backend/canvas-engine/src/main/java/org/chovy/canvas/config/CanvasRuntimeMetrics.java`
- Alert rules: `deploy/observability/prometheus/canvas-alert-rules.yml`
- Dashboard: `deploy/observability/grafana/canvas-runtime-dashboard.json`
- Runbooks: `docs/architecture/runbooks/`
- Verification record: `docs/architecture/evidence/P1-04-observability-and-ops.md`
