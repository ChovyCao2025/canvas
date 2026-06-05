# V237 BI Dashboard Version History

## Backup

Capture dashboard version metadata if present:

```bash
mysqldump --single-transaction canvas bi_dashboard_version > bi-dashboard-version-before-v237.sql || true
```

## Restore

If rollback is required before dashboard versioning is used, drop and restore:

```bash
mysql canvas -e "DROP TABLE IF EXISTS bi_dashboard_version"
mysql canvas < bi-dashboard-version-before-v237.sql || true
```

## Dry run

Run the migration chain on MySQL 8:

```bash
cd backend && mvn -pl canvas-engine -am -P integration-tests -Dapi.version=1.54 -Dtest=CanvasExecutionRequestServiceIdempotencyIntegrationTest test
```

## Rollback owner

BI dashboard owner. Coordinate with frontend and BI platform owners because rollback removes version-history recovery data.
