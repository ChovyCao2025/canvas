# V225 CDP Warehouse Field Governance

## Backup

Capture field policy and access audit data if present:

```bash
mysqldump --single-transaction canvas cdp_warehouse_field_policy cdp_warehouse_field_access_audit > cdp-warehouse-field-governance-before-v225.sql || true
```

## Restore

If rollback is required before access policy enforcement depends on these tables, drop and restore:

```bash
mysql canvas -e "DROP TABLE IF EXISTS cdp_warehouse_field_access_audit, cdp_warehouse_field_policy"
mysql canvas < cdp-warehouse-field-governance-before-v225.sql || true
```

## Dry run

Run the full migration chain on MySQL 8:

```bash
cd backend && mvn -pl canvas-engine -am -P integration-tests -Dapi.version=1.54 -Dtest=CanvasExecutionRequestServiceIdempotencyIntegrationTest test
```

## Rollback owner

Security/data governance owner. Coordinate with BI owners because field policy rollback can broaden or remove access controls.
