# DAG Execution Flow Runbook

## symptom

Use this runbook when execution is accepted but the graph does not progress, a node route is unexpected, wait/resume does not continue, trace rows are missing, or completion status is wrong.

## severity

- P0: executions are accepted but broadly fail or hang.
- P1: one trigger family, lane, or handler type fails.
- P2: a single canvas or node configuration fails.

## dashboard link placeholder

`<grafana>/d/canvas-runtime/dag-execution`

## Execution Path

1. Trigger admission enters through `ExecutionController`, event/MQ/scheduled services, or replay APIs.
2. `CanvasExecutionService` prepares the execution context and trigger payload.
3. `DagParser` reads `graphJson`, builds node maps, edge maps, reverse edges, and validates cycles and multi-input convergence.
4. The scheduler/DAG engine resolves the next node and dispatches the registered `NodeHandler`.
5. The handler returns `NodeResult`; routing stays inside the engine.
6. WAIT nodes may persist state and stop progress until wait/resume injects completed or timeout status.
7. Trace writers persist node evidence and the lifecycle gate updates execution/request completion.

## diagnostic commands

```bash
mysql "$CANVAS_MYSQL_DSN" -e "select id, canvas_id, status, trigger_type, created_at, updated_at from canvas_execution order by id desc limit 20"
mysql "$CANVAS_MYSQL_DSN" -e "select execution_id, node_id, node_type, status, reason_code, created_at from canvas_execution_trace where execution_id='$EXECUTION_ID' order by id"
curl -sS "$CANVAS_BASE_URL/actuator/prometheus" | grep -E "canvas.execution|canvas.trace|canvas.capacity.lane"
```

## remediation commands

```bash
curl -sS -X POST -H "Authorization: Bearer $ADMIN_JWT" \
  "$CANVAS_BASE_URL/ops/cache/invalidate/$CANVAS_ID"
curl -sS -X POST -H "Authorization: Bearer $ADMIN_JWT" \
  "$CANVAS_BASE_URL/ops/recovery/runtime-state/rebuild"
```

## rollback commands

```bash
curl -sS -X POST -H "Authorization: Bearer $ADMIN_JWT" \
  -H "Content-Type: application/json" \
  -d '{"reason":"dag execution incident","mode":"GRACEFUL"}' \
  "$CANVAS_BASE_URL/ops/canvas/$CANVAS_ID/rollback"
```

## evidence

Capture graph version ID, `DagParser` error if present, trace rows, request row, handler logs, and route/cache remediation responses under `docs/architecture/evidence/incidents/<yyyy-mm-dd>-dag-execution.md`.
