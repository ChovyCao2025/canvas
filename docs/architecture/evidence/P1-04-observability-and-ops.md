# P1-04 Observability And Ops Evidence

## Status

Completed on 2026-06-04.

## Implemented

- Request correlation is handled by `CorrelationIdWebFilter`, using `X-Correlation-Id` when present and generating a UUID when absent.
- Error responses expose `traceId` through the shared `R` failure path, including security 401/403 responses and generic exception handling.
- Execution MDC fields are carried with `ExecutionTraceContext`: `traceId`, `executionId`, `canvasId`, and `nodeId`.
- Async work uses `MdcTaskDecorator` across bounded worker paths and virtual-thread tasks.
- Runtime metrics are exposed through `CanvasRuntimeMetrics` for execution failures, DLQ backlog, route rebuild failures, cache invalidation failures, Redis/MQ availability, lane pressure, disruptor pressure, and shutdown drain timeout.
- Deployable Prometheus and Grafana assets were added.
- Operational runbooks were added for DLQ handling, route rebuild, cache invalidation, and shutdown/drain.

## Verification

Focused observability tests:

```bash
cd backend && JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH mvn -pl canvas-engine -Dtest=CorrelationIdWebFilterTest,GlobalExceptionHandlerTraceTest,MdcTaskDecoratorTest,ExecutionTraceContextTest,CanvasRuntimeMetricsTest test
```

Result:

```text
Tests run: 11, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Task-specific checks also passed:

```bash
cd backend && JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH mvn -pl canvas-engine -Dtest=CorrelationIdWebFilterTest,GlobalExceptionHandlerTraceTest test
cd backend && JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH mvn -pl canvas-engine -Dtest=MdcTaskDecoratorTest,ExecutionTraceContextTest test
cd backend && JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH mvn -pl canvas-engine -Dtest=CanvasRuntimeMetricsTest test
```

Regression checks for touched existing behavior also passed:

```bash
cd backend && JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH mvn -pl canvas-engine -Dtest=GlobalExceptionHandlerTest,SecurityConfigRouteTest test
```

Result:

```text
Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Prometheus exposure remains configured:

```text
backend/canvas-engine/src/main/resources/application.yml:196 include: health,info,prometheus,metrics
```

Deployable assets:

```text
deploy/observability/prometheus/canvas-alert-rules.yml
deploy/observability/grafana/canvas-runtime-dashboard.json
```

Asset checks:

```bash
test -f deploy/observability/prometheus/canvas-alert-rules.yml
test -f deploy/observability/grafana/canvas-runtime-dashboard.json
node -e "JSON.parse(require('fs').readFileSync('deploy/observability/grafana/canvas-runtime-dashboard.json','utf8'))"
rg -n "CanvasExecutionFailureRate|CanvasDlqBacklog|route_rebuild|cache_invalidation|shutdown_drain" deploy/observability
```

Runbooks:

```text
docs/architecture/evidence/runbooks/dlq-handling.md
docs/architecture/evidence/runbooks/route-rebuild.md
docs/architecture/evidence/runbooks/cache-invalidation.md
docs/architecture/evidence/runbooks/shutdown-drain.md
```

Runbook structure check:

```bash
rg -n "Owner|Command|Verify|Rollback|Evidence" docs/architecture/evidence/runbooks/dlq-handling.md docs/architecture/evidence/runbooks/route-rebuild.md docs/architecture/evidence/runbooks/cache-invalidation.md docs/architecture/evidence/runbooks/shutdown-drain.md
```

## Remaining Decisions

- No OpenTelemetry, Zipkin, or distributed tracing dependency was introduced in this task. The current implementation uses request correlation ID, MDC propagation, and Micrometer metrics. A future tracing rollout can add spans around trigger, DAG, handler, and external-call boundaries without changing the public error response contract.
- Node-level DLQ rows currently have diagnosis guidance but no dedicated replay endpoint. The runbook restricts direct replay to reviewed `canvas_execution_request` failures and requires manual review for `canvas_execution_dlq` rows.
