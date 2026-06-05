# V232 BI Delivery Attachment

## Backup

Capture BI delivery attachment metadata if present:

```bash
mysqldump --single-transaction canvas bi_delivery_attachment > bi-delivery-attachment-before-v232.sql || true
```

## Restore

If rollback is required before scheduled deliveries depend on attachments, drop and restore:

```bash
mysql canvas -e "DROP TABLE IF EXISTS bi_delivery_attachment"
mysql canvas < bi-delivery-attachment-before-v232.sql || true
```

## Dry run

Run the full migration chain on MySQL 8:

```bash
cd backend && mvn -pl canvas-engine -am -P integration-tests -Dapi.version=1.54 -Dtest=CanvasExecutionRequestServiceIdempotencyIntegrationTest test
```

## Rollback owner

BI delivery owner. Coordinate with notification/export owners because attachment metadata is part of delivery fulfillment.
