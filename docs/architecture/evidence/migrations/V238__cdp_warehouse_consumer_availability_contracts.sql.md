# V238 CDP Warehouse Consumer Availability Contracts

## Backup

Capture availability assets and consumer contracts if present:

```bash
mysqldump --single-transaction canvas cdp_warehouse_asset_availability cdp_warehouse_consumer_availability_contract > cdp-warehouse-availability-contracts-before-v238.sql || true
```

## Restore

If rollback is required before availability gates depend on these tables, drop and restore:

```bash
mysql canvas -e "DROP TABLE IF EXISTS cdp_warehouse_consumer_availability_contract, cdp_warehouse_asset_availability"
mysql canvas < cdp-warehouse-availability-contracts-before-v238.sql || true
```

## Dry run

Run the full migration chain on MySQL 8:

```bash
cd backend && mvn -pl canvas-engine -am -P integration-tests -Dapi.version=1.54 -Dtest=CanvasExecutionRequestServiceIdempotencyIntegrationTest test
```

## Rollback owner

CDP warehouse availability owner. Coordinate with BI, audience, and scheduler consumers because rollback disables availability contract gates.
