# V215 CDP Warehouse Runs And Watermarks

## Backup

Capture warehouse run and watermark metadata if present:

```bash
mysqldump --single-transaction canvas cdp_warehouse_sync_run cdp_warehouse_watermark > cdp-warehouse-runs-before-v215.sql || true
```

## Restore

If rollback is required before scheduled syncs depend on these tables, drop and restore:

```bash
mysql canvas -e "DROP TABLE IF EXISTS cdp_warehouse_watermark, cdp_warehouse_sync_run"
mysql canvas < cdp-warehouse-runs-before-v215.sql || true
```

## Dry run

Run the full Flyway chain on MySQL 8:

```bash
cd backend && mvn -pl canvas-engine -am -P integration-tests -Dapi.version=1.54 -Dtest=CanvasExecutionRequestServiceIdempotencyIntegrationTest test
```

## Rollback owner

CDP warehouse owner. Coordinate with runtime scheduling owners because watermarks control replay and sync position.
