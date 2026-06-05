# V214 CDP OLAP Audience Materialization

## Backup

Capture any pre-existing audience materialization metadata before applying:

```bash
mysqldump --single-transaction canvas cdp_user_index audience_bitmap_version audience_materialization_run audience_quality_check > audience-materialization-before-v214.sql || true
```

## Restore

If rollback is required before consumers rely on materialized audiences, drop the created tables and restore any previous dump:

```bash
mysql canvas -e "DROP TABLE IF EXISTS audience_quality_check, audience_materialization_run, audience_bitmap_version, cdp_user_index"
mysql canvas < audience-materialization-before-v214.sql || true
```

## Dry run

Run the migration chain against a MySQL 8 clone:

```bash
cd backend && mvn -pl canvas-engine -am -P integration-tests -Dapi.version=1.54 -Dtest=CanvasExecutionRequestServiceIdempotencyIntegrationTest test
```

## Rollback owner

CDP platform owner with Backend/DBA support. Coordinate with audience operations because bitmap versions and quality checks may drive targeting.
