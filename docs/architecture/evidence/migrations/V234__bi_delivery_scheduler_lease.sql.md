# V234 BI Delivery Scheduler Lease

## Backup

Capture scheduler lease state if present:

```bash
mysqldump --single-transaction canvas bi_delivery_scheduler_lease > bi-delivery-scheduler-lease-before-v234.sql || true
```

## Restore

If rollback is required, stop BI delivery schedulers, drop and restore:

```bash
mysql canvas -e "DROP TABLE IF EXISTS bi_delivery_scheduler_lease"
mysql canvas < bi-delivery-scheduler-lease-before-v234.sql || true
```

## Dry run

Run the full migration chain on MySQL 8:

```bash
cd backend && mvn -pl canvas-engine -am -P integration-tests -Dapi.version=1.54 -Dtest=CanvasExecutionRequestServiceIdempotencyIntegrationTest test
```

## Rollback owner

BI delivery owner. Coordinate with scheduler operators to prevent duplicate delivery runs.
