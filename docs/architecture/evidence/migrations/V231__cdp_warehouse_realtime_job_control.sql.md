# V231 CDP Warehouse Realtime Job Control

## Backup

Capture stream job control tables if present:

```bash
mysqldump --single-transaction canvas cdp_warehouse_stream_job_instance cdp_warehouse_stream_job_action > cdp-warehouse-stream-job-control-before-v231.sql || true
```

## Restore

If rollback is required, stop realtime job controllers, drop and restore:

```bash
mysql canvas -e "DROP TABLE IF EXISTS cdp_warehouse_stream_job_action, cdp_warehouse_stream_job_instance"
mysql canvas < cdp-warehouse-stream-job-control-before-v231.sql || true
```

## Dry run

Run the migration chain on MySQL 8:

```bash
cd backend && mvn -pl canvas-engine -am -P integration-tests -Dapi.version=1.54 -Dtest=CanvasExecutionRequestServiceIdempotencyIntegrationTest test
```

## Rollback owner

Runtime platform owner. Coordinate with CDP realtime owners because job actions and instances are operational control state.
