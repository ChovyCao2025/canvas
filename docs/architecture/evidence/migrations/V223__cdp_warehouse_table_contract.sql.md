# V223 CDP Warehouse Table Contract

## Backup

Capture table contracts and inspection records if present:

```bash
mysqldump --single-transaction canvas cdp_warehouse_table_contract cdp_warehouse_table_inspection > cdp-warehouse-table-contract-before-v223.sql || true
```

## Restore

If rollback is required before governance checks depend on these tables, drop and restore:

```bash
mysql canvas -e "DROP TABLE IF EXISTS cdp_warehouse_table_inspection, cdp_warehouse_table_contract"
mysql canvas < cdp-warehouse-table-contract-before-v223.sql || true
```

## Dry run

Run the migration chain on MySQL 8:

```bash
cd backend && mvn -pl canvas-engine -am -P integration-tests -Dapi.version=1.54 -Dtest=CanvasExecutionRequestServiceIdempotencyIntegrationTest test
```

## Rollback owner

Data governance owner. Coordinate with warehouse and BI owners because table contracts gate consumers.
