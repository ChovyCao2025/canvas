# V186 Widen Context Field Source Node Type

## Backup

Before applying, export the `context_field` table and capture the current schema:

```bash
mysqldump --single-transaction --no-create-info canvas context_field > context_field-data-before-v186.sql
mysqldump --single-transaction --no-data canvas context_field > context_field-schema-before-v186.sql
```

## Restore

If rollback is required, restore the pre-change table from the captured dump after confirming no values exceed the previous width:

```bash
mysql canvas < context_field-schema-before-v186.sql
mysql canvas < context_field-data-before-v186.sql
```

## Dry run

Run the migration against a MySQL 8 clone and verify that long source node lists are retained:

```bash
cd backend && mvn -pl canvas-engine -am -P integration-tests -Dapi.version=1.54 -Dtest=CanvasExecutionRequestServiceIdempotencyIntegrationTest test
```

## Rollback owner

Backend/DBA owner. Rollback must be coordinated with runtime platform because context field metadata is read by canvas editor and execution services.
