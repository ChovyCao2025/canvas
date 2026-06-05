# V236 BI Export Job Retention

## Backup

Capture BI export jobs before altering retention columns or indexes:

```bash
mysqldump --single-transaction canvas bi_export_job > bi-export-job-before-v236.sql
mysqldump --single-transaction --no-data canvas bi_export_job > bi-export-job-schema-before-v236.sql
```

## Restore

If rollback is required and new export job writes can be replayed, restore the captured table:

```bash
mysql canvas < bi-export-job-schema-before-v236.sql
mysql canvas < bi-export-job-before-v236.sql
```

## Dry run

Run the full migration chain on MySQL 8:

```bash
cd backend && mvn -pl canvas-engine -am -P integration-tests -Dapi.version=1.54 -Dtest=CanvasExecutionRequestServiceIdempotencyIntegrationTest test
```

## Rollback owner

BI platform owner with Backend/DBA support. Coordinate with export and storage-retention owners.
