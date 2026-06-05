# V189 Project Governance

## Backup

Before applying, capture project governance tables if they already exist and export the current `canvas` schema used to backfill folder tenancy:

```bash
mysqldump --single-transaction --no-data canvas canvas_project canvas_project_member canvas_project_folder > project-governance-schema-before-v189.sql || true
mysqldump --single-transaction canvas canvas_project canvas_project_member canvas_project_folder > project-governance-data-before-v189.sql || true
mysqldump --single-transaction --no-data canvas canvas > canvas-schema-before-v189-project-governance.sql
```

## Restore

If rollback is required before production writes depend on the project tables, drop the created governance tables and restore any previous dumps:

```bash
mysql canvas -e "DROP TABLE IF EXISTS canvas_project_member, canvas_project_folder, canvas_project"
mysql canvas < project-governance-schema-before-v189.sql || true
mysql canvas < project-governance-data-before-v189.sql || true
```

## Dry run

Run the full migration chain on MySQL 8 and verify the project backfill completes:

```bash
cd backend && mvn -pl canvas-engine -am -P integration-tests -Dapi.version=1.54 -Dtest=CanvasExecutionRequestServiceIdempotencyIntegrationTest test
```

## Rollback owner

Backend/DBA owner. Coordinate rollback with canvas product owners because project and folder assignments are user-visible governance metadata.
