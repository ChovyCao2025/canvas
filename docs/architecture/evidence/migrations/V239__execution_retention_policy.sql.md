# V239 Execution Retention Policy

## Backup

Before applying, capture any existing retention metadata and the online execution tables governed by the policy:

```bash
mysqldump --single-transaction canvas execution_retention_policy execution_retention_run execution_retention_archive_manifest > execution-retention-metadata-before-v239.sql || true
mysqldump --single-transaction --no-data canvas canvas_execution canvas_execution_trace canvas_execution_dlq canvas_execution_request canvas_execution_stats event_log > execution-retention-governed-schema-before-v239.sql
```

## Restore

If rollback is required before cleanup jobs consume the policy registry, drop the new metadata tables and restore any prior dump:

```bash
mysql canvas -e "DROP TABLE IF EXISTS execution_retention_archive_manifest, execution_retention_run, execution_retention_policy"
mysql canvas < execution-retention-metadata-before-v239.sql || true
```

## Dry run

Run the full migration chain against a MySQL 8 clone and confirm retention schema tests pass:

```bash
cd backend && mvn -pl canvas-engine -am -P integration-tests -Dapi.version=1.54 -Dtest=CanvasExecutionRequestServiceIdempotencyIntegrationTest test
cd backend && mvn -pl canvas-engine -Dtest=PerfRunTrackingSchemaTest,CanvasExecutionDlqSchemaTest test
```

## Rollback owner

Runtime platform owner with Backend/DBA approval. Coordinate with compliance and data platform owners before disabling retention policy metadata.
