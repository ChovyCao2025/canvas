# Capacity Dashboards And Alerts

Date: 2026-06-05

## Dashboard Panels

| Panel | Metric | Unit | Warning | Critical | Owner | Runbook |
| --- | --- | --- | --- | --- | --- | --- |
| pool saturation | `canvas.capacity.pool.saturation.percent` | percent | 80 | 90 | Backend/DBA | `docs/architecture/evidence/capacity/slo-and-capacity-model.md` |
| request backlog | `canvas.execution.request.backlog` | requests | 5,000 pending/retry | 10,000 pending/retry | Runtime platform | `docs/architecture/evidence/capacity/slo-and-capacity-model.md` |
| lane saturation | `canvas.capacity.lane.saturation.percent` | percent | 80 | 95 | Runtime platform | `docs/architecture/evidence/capacity/slo-and-capacity-model.md` |
| queue depth | `canvas.capacity.queue.depth` | messages | 50,000 | 100,000 | Runtime platform | `docs/stressTest/local-capacity-runbook.md` |
| Redis memory | `canvas.capacity.redis.memory.bytes` | bytes | 70% maxmemory | 85% maxmemory | Runtime platform | `docs/architecture/evidence/capacity/retention-policy.md` |
| trace drops | `canvas.trace.dropped.total` | count | any increase in 15 minutes | sustained increase | Runtime platform | `docs/architecture/evidence/capacity/retention-policy.md` |
| DLQ growth | `canvas.capacity.dlq.backlog` and `canvas.dlq.size` | rows/messages | 100 | 500 | Runtime platform | `docs/architecture/evidence/capacity/retention-policy.md` |
| execution failures | `canvas.execution.total{status="FAILED"}` | executions | 1% over 15 minutes | 5% over 15 minutes | Runtime platform | `docs/architecture/evidence/capacity/slo-and-capacity-model.md` |

## Alert Rules

| Alert | Expression intent | Severity | Owner |
| --- | --- | --- | --- |
| HikariPoolSaturationHigh | pool saturation >= 90% for 10 minutes | P1 | Backend/DBA |
| ExecutionRequestBacklogHigh | pending + retry backlog >= 10,000 for 10 minutes | P1 | Runtime platform |
| LaneSaturationCritical | any lane saturation >= 95% for 10 minutes | P1 | Runtime platform |
| RocketMqDepthHigh | MQ queue depth >= 100,000 for 10 minutes | P1 | Runtime platform |
| RedisMemoryHigh | Redis memory >= 85% maxmemory for 15 minutes | P1 | Runtime platform |
| TraceDropped | trace drops increase for 5 minutes | P0 | Runtime platform |
| DlqBacklogHigh | DLQ backlog >= 500 for 30 minutes | P1 | Runtime platform |
| ExecutionFailureRateHigh | failed executions >= 5% for 15 minutes | P1 | Runtime platform |

## Metric Gaps

- `canvas.trigger.admission.duration` and notification delivery timers are named SLO metrics but still require instrumentation at the request admission and notification dispatch boundaries.
- RocketMQ depth and Redis memory values require collectors to call `CanvasMetrics.setQueueDepth` and `CanvasMetrics.setRedisMemoryBytes`.
- Hikari pool saturation can be sourced from Micrometer Hikari metrics or normalized into `CanvasMetrics.setPoolSaturationPercent`.
