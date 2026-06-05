# Shutdown Drain Runbook

## Owner

Runtime on-call engineer during deploys and emergency restarts. Escalate to platform ownership if Kubernetes or host termination grace periods are shorter than the configured drain timeout.

## Symptoms

- `CanvasShutdownDrainTimeout` alert is firing.
- Deployments terminate while canvas executions are still in flight.
- `canvas_runtime_lane_pressure` or `canvas_disruptor_backlog` remains high during a planned restart.

## Command

Pre-drain checks:

```bash
curl -sS "$CANVAS_BASE_URL/actuator/health"
curl -sS "$CANVAS_BASE_URL/actuator/prometheus" | \
  grep -E "canvas_runtime_lane_pressure|canvas_runtime_disruptor_pressure|canvas_execution_request_backlog"
```

During a planned deployment, stop routing new traffic first at the load balancer or orchestrator, then wait at least:

```text
max(canvas.execution.shutdown-drain-timeout-ms, canvas.background-tasks.shutdown-timeout-ms)
```

Post-drain validation:

```bash
curl -sS "$CANVAS_BASE_URL/actuator/prometheus" | \
  grep -E "canvas_runtime_shutdown_drain_timeout_total|canvas_runtime_execution_failures_total"
```

## Verify

No new `canvas_runtime_shutdown_drain_timeout_total` increment occurred during the deployment window. Execution request backlog is stable or draining after the new instance starts.

## Rollback

Abort the rollout if shutdown drain timeouts increase, if the execution request backlog grows for two consecutive checks, or if DLQ backlog appears after restart. Restore traffic to the previous healthy instance group and investigate in-flight execution IDs before retrying.

## Evidence

Save deployment window timestamps, pre/post Prometheus snippets, orchestrator termination events, and any timed-out execution IDs under:

```text
docs/architecture/evidence/incidents/<yyyy-mm-dd>-shutdown-drain.md
```
