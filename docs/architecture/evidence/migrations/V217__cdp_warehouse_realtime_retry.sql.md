# V217 CDP Warehouse Realtime Retry

## Backup

Capture the retry buffer table if it already exists:

```bash
mysqldump --single-transaction canvas cdp_warehouse_realtime_retry > cdp-warehouse-realtime-retry-before-v217.sql || true
```

## Restore

If rollback is required before retry workers use the table, drop and restore:

```bash
mysql canvas -e "DROP TABLE IF EXISTS cdp_warehouse_realtime_retry"
mysql canvas < cdp-warehouse-realtime-retry-before-v217.sql || true
```

## Dry run

Run the full migration chain on MySQL 8:

```bash
cd backend && mvn -pl canvas-engine -am -P integration-tests -Dapi.version=1.54 -Dtest=CanvasExecutionRequestServiceIdempotencyIntegrationTest test
```

## Rollback owner

CDP realtime owner. Coordinate with ingestion owners because rollback can discard retry-buffer control state.
