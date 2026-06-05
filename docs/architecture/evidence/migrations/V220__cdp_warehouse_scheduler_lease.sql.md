# V220 CDP Warehouse Scheduler Lease

## Backup

Capture scheduler lease state if present:

```bash
mysqldump --single-transaction canvas cdp_warehouse_job_lease > cdp-warehouse-job-lease-before-v220.sql || true
```

## Restore

If rollback is required, stop warehouse schedulers, drop the table, and restore any previous state:

```bash
mysql canvas -e "DROP TABLE IF EXISTS cdp_warehouse_job_lease"
mysql canvas < cdp-warehouse-job-lease-before-v220.sql || true
```

## Dry run

Run the migration chain on MySQL 8:

```bash
cd backend && mvn -pl canvas-engine -am -P integration-tests -Dapi.version=1.54 -Dtest=CanvasExecutionRequestServiceIdempotencyIntegrationTest test
```

## Rollback owner

Runtime platform owner. Coordinate rollback with scheduler operators to avoid duplicate warehouse jobs.
