# Cache Invalidation Runbook

## symptom

Use when `CanvasCacheInvalidationFailure` fires, published graph changes are not reflected by direct or trigger execution, or a migration/manual data repair changed graph/version data.

## severity

- P0: stale cache can execute the wrong published graph for many canvases.
- P1: one active canvas executes stale runtime config.
- P2: cache refresh is needed after a controlled repair.

## dashboard link placeholder

`<grafana>/d/canvas-runtime/cache-invalidation`

## diagnostic commands

```bash
curl -sS "$CANVAS_BASE_URL/actuator/prometheus" | grep -E "cache|invalidation"
mysql "$CANVAS_MYSQL_DSN" -e "select id, published_version_id, canary_version_id, updated_at from canvas where id='$CANVAS_ID'"
```

`CanvasConfigCache` and `CanvasEntityCache` own runtime cache reads. `RocketMqCacheInvalidationPublisher` broadcasts cross-node invalidation.

## remediation commands

Invalidate one canvas runtime config cache:

```bash
curl -sS -X POST -H "Authorization: Bearer $ADMIN_JWT" \
  "$CANVAS_BASE_URL/ops/cache/invalidate/$CANVAS_ID"
```

If routing may also be stale, rebuild runtime state after cache invalidation:

```bash
curl -sS -X POST -H "Authorization: Bearer $ADMIN_JWT" \
  "$CANVAS_BASE_URL/ops/recovery/runtime-state/rebuild"
```

## rollback commands

Cache invalidation removes stale state but does not change source data. If behavior remains wrong after invalidation, roll back the published canvas version or revert the database repair, then invalidate again.

```bash
curl -sS -X POST -H "Authorization: Bearer $ADMIN_JWT" \
  -H "Content-Type: application/json" \
  -d '{"reason":"stale cache incident","mode":"GRACEFUL"}' \
  "$CANVAS_BASE_URL/ops/canvas/$CANVAS_ID/rollback"
```

## evidence

Record the canvas ID, invalidate response, optional rebuild response, expected version ID, actual execution version ID, and Prometheus snapshot under:

```text
docs/architecture/evidence/incidents/<yyyy-mm-dd>-cache-invalidation.md
```
