# V187 Create AB Experiment Table

## Backup

Before applying, capture any existing AB experiment table metadata and group data:

```bash
mysqldump --single-transaction --no-data canvas ab_experiment ab_experiment_group > ab-experiment-schema-before-v187.sql
mysqldump --single-transaction canvas ab_experiment_group > ab-experiment-groups-before-v187.sql
```

## Restore

If rollback is required, drop the newly created `ab_experiment` table only after confirming no experiments were created after the migration:

```bash
mysql canvas -e "DROP TABLE IF EXISTS ab_experiment"
mysql canvas < ab-experiment-groups-before-v187.sql
```

## Dry run

Run the migration on a MySQL 8 clone and verify fresh Flyway migration plus AB experiment API metadata access:

```bash
cd backend && mvn -pl canvas-engine -am -P integration-tests -Dapi.version=1.54 -Dtest=CanvasExecutionRequestServiceIdempotencyIntegrationTest test
```

## Rollback owner

Backend/DBA owner. Coordinate with frontend and runtime platform because `/canvas/ab-experiments` and `/meta/ab-experiments` depend on this table.
