# V233 BI Delivery Attachment Retention

## Backup

Capture the attachment table before altering retention columns or indexes:

```bash
mysqldump --single-transaction canvas bi_delivery_attachment > bi-delivery-attachment-before-v233.sql
mysqldump --single-transaction --no-data canvas bi_delivery_attachment > bi-delivery-attachment-schema-before-v233.sql
```

## Restore

If rollback is required and no new attachment writes must be preserved, restore the captured table:

```bash
mysql canvas < bi-delivery-attachment-schema-before-v233.sql
mysql canvas < bi-delivery-attachment-before-v233.sql
```

## Dry run

Run the migration chain on MySQL 8:

```bash
cd backend && mvn -pl canvas-engine -am -P integration-tests -Dapi.version=1.54 -Dtest=CanvasExecutionRequestServiceIdempotencyIntegrationTest test
```

## Rollback owner

BI delivery owner with Backend/DBA support. Coordinate with storage retention owners before reverting retention metadata.
