# Route Rebuild Runbook

## symptom

Use when MQ, event, or tagger triggers have no matching canvas even though the published graph contains a trigger node, Redis route keys are missing or stale, or `CanvasRouteRebuildFailure` fires.

## severity

- P0: all trigger routing is unavailable.
- P1: one trigger family is unavailable.
- P2: one canvas route is stale after publish, rollback, or migration.

## dashboard link placeholder

`<grafana>/d/canvas-runtime/route-rebuild`

## diagnostic commands

```bash
redis-cli -u "$CANVAS_REDIS_URL" --scan --pattern "canvas:trigger:*" | head -50
mysql "$CANVAS_MYSQL_DSN" -e "select id, status, published_version_id from canvas where id='$CANVAS_ID'"
curl -sS "$CANVAS_BASE_URL/actuator/prometheus" | grep -E "route|mq_route"
```

`CanvasRouteInitializer` rebuilds routes on cold start when the table is empty. `TriggerRouteService` owns route sets, route ready flag, and route mutation lock.

## remediation commands

Rebuild Redis trigger routes and local scheduled runtime state from published canvas versions:

```bash
curl -sS -X POST -H "Authorization: Bearer $ADMIN_JWT" \
  "$CANVAS_BASE_URL/ops/recovery/runtime-state/rebuild"
```

## rollback commands

Route rebuild is source-of-truth based and has no direct rollback. If rebuilt routes are wrong, roll back the published canvas version or disable the affected trigger, then rerun rebuild:

```bash
curl -sS -X POST -H "Authorization: Bearer $ADMIN_JWT" \
  -H "Content-Type: application/json" \
  -d '{"reason":"bad route rebuild","mode":"GRACEFUL"}' \
  "$CANVAS_BASE_URL/ops/canvas/$CANVAS_ID/rollback"
```

## evidence

Capture the rebuild response, Redis key sample, trigger sample, and Prometheus output under:

```text
docs/architecture/evidence/incidents/<yyyy-mm-dd>-route-rebuild.md
```
