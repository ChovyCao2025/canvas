# V235 BI Delivery Retry Backoff

## Backup

Capture BI delivery logs before altering retry/backoff columns:

```bash
mysqldump --single-transaction canvas bi_delivery_log > bi-delivery-log-before-v235.sql
mysqldump --single-transaction --no-data canvas bi_delivery_log > bi-delivery-log-schema-before-v235.sql
```

## Restore

If rollback is required and new delivery log writes can be replayed or discarded, restore the captured schema/data:

```bash
mysql canvas < bi-delivery-log-schema-before-v235.sql
mysql canvas < bi-delivery-log-before-v235.sql
```

## Dry run

Run the migration chain on MySQL 8:

```bash
cd backend && mvn -pl canvas-engine -am -P integration-tests -Dapi.version=1.54 -Dtest=CanvasExecutionRequestServiceIdempotencyIntegrationTest test
```

## Rollback owner

BI delivery owner. Coordinate with notification/export owners because retry state affects duplicate-send prevention.
