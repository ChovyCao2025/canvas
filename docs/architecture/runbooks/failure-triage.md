# Failure Triage Runbook

## symptom

Use when alerts show execution failures, trigger rejection, trace drops, DLQ growth, route rebuild failures, cache invalidation failures, or API p95 breaches.

## severity

- P0: user-facing execution is unavailable, trace drops are sustained, or data isolation is at risk.
- P1: one lane, trigger type, or operational API is degraded.
- P2: isolated canvas, handler, or integration failure.

## dashboard link placeholder

`<grafana>/d/canvas-runtime/failure-triage`

## diagnostic commands

```bash
curl -sS "$CANVAS_BASE_URL/actuator/health"
curl -sS "$CANVAS_BASE_URL/actuator/prometheus" | grep -E "canvas.execution|canvas.capacity|canvas.trace|dlq|route|cache"
mysql "$CANVAS_MYSQL_DSN" -e "select status, count(*) from canvas_execution_request group by status"
mysql "$CANVAS_MYSQL_DSN" -e "select failed_node_type, count(*) from canvas_execution_dlq group by failed_node_type order by count(*) desc limit 10"
```

## remediation commands

```bash
curl -sS -X POST -H "Authorization: Bearer $ADMIN_JWT" "$CANVAS_BASE_URL/ops/recovery/runtime-state/rebuild"
curl -sS -X POST -H "Authorization: Bearer $ADMIN_JWT" "$CANVAS_BASE_URL/ops/cache/invalidate/$CANVAS_ID"
```

Escalate to `dlq-replay.md` only after sampling failed rows and confirming side-effect safety.

## rollback commands

```bash
curl -sS -X POST -H "Authorization: Bearer $ADMIN_JWT" \
  -H "Content-Type: application/json" \
  -d '{"reason":"runtime failure triage","mode":"GRACEFUL"}' \
  "$CANVAS_BASE_URL/ops/canvas/$CANVAS_ID/pause"
```

Use `deploy-rollback.md` for release-wide rollback.

## evidence

Capture alert ID, severity, time window, health output, Prometheus snippets, sampled request IDs, DLQ IDs, remediation responses, and owner decisions under `docs/architecture/evidence/incidents/<yyyy-mm-dd>-failure-triage.md`.
