# V222 CDP Warehouse Quality Incident

## Backup

Capture quality incident state if present:

```bash
mysqldump --single-transaction canvas cdp_warehouse_incident > cdp-warehouse-incident-before-v222.sql || true
```

## Restore

If rollback is required before incident automation depends on this table, drop and restore:

```bash
mysql canvas -e "DROP TABLE IF EXISTS cdp_warehouse_incident"
mysql canvas < cdp-warehouse-incident-before-v222.sql || true
```

## Dry run

Run the full migration chain on MySQL 8:

```bash
cd backend && mvn -pl canvas-engine -am -P integration-tests -Dapi.version=1.54 -Dtest=CanvasExecutionRequestServiceIdempotencyIntegrationTest test
```

## Rollback owner

Data quality owner. Coordinate with operations because rollback can hide active quality incidents.
