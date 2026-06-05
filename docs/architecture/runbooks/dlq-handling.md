# DLQ Handling Runbook

## Owner

Runtime on-call engineer. Escalate to the canvas domain owner when the failed node performs side effects such as message delivery, points, coupon, webhook, or external API mutation.

## Symptoms

- `CanvasDlqBacklog` alert is firing.
- `canvas_runtime_dlq_backlog` is greater than zero.
- `canvas.execution.request.backlog{status="FAILED"}` or `canvas.execution.request.backlog{status="RETRY"}` keeps increasing.

## Command

Diagnose execution request backlog:

```bash
curl -sS -H "Authorization: Bearer $ADMIN_JWT" \
  "$CANVAS_BASE_URL/canvas/execution-requests?status=FAILED&page=1&size=20"
```

Replay one reviewed execution request:

```bash
curl -sS -X POST -H "Authorization: Bearer $ADMIN_JWT" \
  "$CANVAS_BASE_URL/canvas/execution-requests/$REQUEST_ID/replay?reason=dlq-reviewed"
```

Replay a bounded batch after sampling failures:

```bash
curl -sS -X POST -H "Authorization: Bearer $ADMIN_JWT" \
  "$CANVAS_BASE_URL/canvas/execution-requests/replay?status=FAILED&limit=100&reason=dlq-reviewed"
```

Inspect node-level DLQ rows before any manual repair:

```bash
mysql "$CANVAS_MYSQL_DSN" -e \
  "select id, execution_id, canvas_id, failed_node_id, failed_node_type, retry_count, left(error_msg, 200) error_msg, failed_at from canvas_execution_dlq order by failed_at desc limit 50"
```

## Verify

```bash
curl -sS "$CANVAS_BASE_URL/actuator/prometheus" | \
  grep -E "canvas_runtime_dlq_backlog|canvas_execution_request_backlog|canvas_runtime_execution_failures_total"
```

Confirm sampled replayed requests moved out of `FAILED` or `RETRY` and did not create duplicate side effects.

## Rollback

Stop replay immediately if the same error repeats for more than 10 percent of replayed rows, if side-effect nodes show duplicate delivery, or if lane pressure exceeds 85 percent for 10 minutes. Leave remaining rows untouched and attach sampled request IDs to the incident.

## Evidence

Save command output, sampled `requestId` values, before/after Prometheus snapshots, and any manually reviewed `canvas_execution_dlq.id` rows under:

```text
docs/architecture/evidence/incidents/<yyyy-mm-dd>-dlq-handling.md
```
