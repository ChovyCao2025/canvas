# V219 CDP Warehouse Catalog Lineage

## Backup

Capture catalog and lineage tables if present:

```bash
mysqldump --single-transaction canvas cdp_warehouse_dataset_catalog cdp_warehouse_lineage_edge > cdp-warehouse-catalog-lineage-before-v219.sql || true
```

## Restore

If rollback is required before consumers depend on catalog lineage, drop and restore:

```bash
mysql canvas -e "DROP TABLE IF EXISTS cdp_warehouse_lineage_edge, cdp_warehouse_dataset_catalog"
mysql canvas < cdp-warehouse-catalog-lineage-before-v219.sql || true
```

## Dry run

Run the full migration chain on MySQL 8:

```bash
cd backend && mvn -pl canvas-engine -am -P integration-tests -Dapi.version=1.54 -Dtest=CanvasExecutionRequestServiceIdempotencyIntegrationTest test
```

## Rollback owner

CDP warehouse governance owner. Coordinate with BI and audience materialization owners because lineage powers impact analysis.
