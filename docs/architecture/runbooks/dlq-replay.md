# DLQ Replay Runbook

## symptom

Use when `canvas_execution_dlq` grows, `CanvasDlqBacklog` fires, or sampled execution requests are stuck in failed/retry states and require reviewed replay.

## severity

- P0: DLQ is growing from a global runtime defect or side-effect duplication risk exists.
- P1: one handler type or integration creates repeated DLQ rows.
- P2: a bounded canvas/configuration issue after operator review.

## dashboard link placeholder

`<grafana>/d/canvas-runtime/dlq-replay`

## diagnostic commands

```bash
mysql "$CANVAS_MYSQL_DSN" -e "select id, canvas_id, user_id, trigger_type, trigger_node_type, retry_count, left(error_msg, 200), failed_at from canvas_execution_dlq order by failed_at desc limit 50"
curl -sS -H "Authorization: Bearer $ADMIN_JWT" "$CANVAS_BASE_URL/canvas/dlq?page=1&size=20"
```

`DlqController` exposes `GET /canvas/dlq`, `POST /canvas/dlq/{id}/replay`, and `DELETE /canvas/dlq/{id}`. Replay creates a new trigger call with `dlq-replay-*` idempotency key; it does not delete the original DLQ row.

## remediation commands

```bash
curl -sS -X POST -H "Authorization: Bearer $ADMIN_JWT" \
  "$CANVAS_BASE_URL/canvas/dlq/$DLQ_ID/replay?skipSuccessNodes=true"
```

Retry limits: replay at most 100 sampled rows per incident batch, stop when more than 10 percent of sampled rows fail again, and never replay side-effect nodes without duplicate-delivery review.

## rollback commands

```bash
curl -sS -X POST -H "Authorization: Bearer $ADMIN_JWT" \
  -H "Content-Type: application/json" \
  -d '{"reason":"dlq replay stopped","mode":"GRACEFUL"}' \
  "$CANVAS_BASE_URL/ops/canvas/$CANVAS_ID/pause"
```

## replay verification

Verify the new execution/request row, confirm trace completion, compare side-effect ledgers, and ensure DLQ backlog decreases or remains bounded.

## evidence

Capture DLQ IDs, replay command output, before/after metrics, sampled side-effect ledger rows, replay verification SQL, retry count, and stop/continue decision under `docs/architecture/evidence/incidents/<yyyy-mm-dd>-dlq-replay.md`.
