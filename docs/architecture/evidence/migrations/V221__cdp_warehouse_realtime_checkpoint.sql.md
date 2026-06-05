# V221 CDP Warehouse Realtime Checkpoint

## Backup

Capture checkpoint state if present:

```bash
mysqldump --single-transaction canvas cdp_warehouse_realtime_checkpoint > cdp-warehouse-realtime-checkpoint-before-v221.sql || true
```

## Restore

If rollback is required, stop realtime consumers, drop the table, and restore any previous checkpoint dump:

```bash
mysql canvas -e "DROP TABLE IF EXISTS cdp_warehouse_realtime_checkpoint"
mysql canvas < cdp-warehouse-realtime-checkpoint-before-v221.sql || true
```

## Dry run

Run the full migration chain on MySQL 8:

```bash
cd backend && mvn -pl canvas-engine -am -P integration-tests -Dapi.version=1.54 -Dtest=CanvasExecutionRequestServiceIdempotencyIntegrationTest test
```

## Rollback owner

CDP realtime owner. Coordinate rollback with ingestion and replay owners because checkpoints define replay position.
