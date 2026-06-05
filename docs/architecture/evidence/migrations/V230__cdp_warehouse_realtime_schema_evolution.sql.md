# V230 CDP Warehouse Realtime Schema Evolution

## Backup

Capture stream schema versions and checkpoint schema state before applying:

```bash
mysqldump --single-transaction canvas cdp_warehouse_stream_schema cdp_warehouse_stream_checkpoint > cdp-warehouse-stream-schema-before-v230.sql || true
```

## Restore

If rollback is required, stop stream jobs, restore the checkpoint table/schema dump, and drop stream schema metadata if created by this migration:

```bash
mysql canvas -e "DROP TABLE IF EXISTS cdp_warehouse_stream_schema"
mysql canvas < cdp-warehouse-stream-schema-before-v230.sql || true
```

## Dry run

Run the full migration chain on MySQL 8:

```bash
cd backend && mvn -pl canvas-engine -am -P integration-tests -Dapi.version=1.54 -Dtest=CanvasExecutionRequestServiceIdempotencyIntegrationTest test
```

## Rollback owner

CDP realtime owner. Coordinate with schema governance owners because rollback affects schema version compatibility.
