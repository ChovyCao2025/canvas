# V226 CDP Warehouse SLO Policy

## Backup

Capture SLO policy state if present:

```bash
mysqldump --single-transaction canvas cdp_warehouse_slo_policy > cdp-warehouse-slo-policy-before-v226.sql || true
```

## Restore

If rollback is required before SLO gates use this table, drop and restore:

```bash
mysql canvas -e "DROP TABLE IF EXISTS cdp_warehouse_slo_policy"
mysql canvas < cdp-warehouse-slo-policy-before-v226.sql || true
```

## Dry run

Run the migration chain on MySQL 8:

```bash
cd backend && mvn -pl canvas-engine -am -P integration-tests -Dapi.version=1.54 -Dtest=CanvasExecutionRequestServiceIdempotencyIntegrationTest test
```

## Rollback owner

Runtime platform owner with data reliability owner approval. Coordinate with consumers because SLO gates affect availability decisions.
