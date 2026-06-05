# V224 CDP Warehouse Realtime Pipeline Runtime

## Backup

Capture stream pipeline and checkpoint metadata if present:

```bash
mysqldump --single-transaction canvas cdp_warehouse_stream_pipeline cdp_warehouse_stream_checkpoint > cdp-warehouse-stream-runtime-before-v224.sql || true
```

## Restore

If rollback is required, stop stream jobs, drop runtime tables, and restore previous state:

```bash
mysql canvas -e "DROP TABLE IF EXISTS cdp_warehouse_stream_checkpoint, cdp_warehouse_stream_pipeline"
mysql canvas < cdp-warehouse-stream-runtime-before-v224.sql || true
```

## Dry run

Run the full migration chain on MySQL 8:

```bash
cd backend && mvn -pl canvas-engine -am -P integration-tests -Dapi.version=1.54 -Dtest=CanvasExecutionRequestServiceIdempotencyIntegrationTest test
```

## Rollback owner

CDP realtime owner. Coordinate rollback with stream job operators to avoid duplicate processing.
