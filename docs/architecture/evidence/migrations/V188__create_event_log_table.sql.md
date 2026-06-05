# V188 Create Event Log Table

## Backup

Before applying, capture any existing event log schema and data if the table exists:

```bash
mysqldump --single-transaction --no-data canvas event_log > event-log-schema-before-v188.sql || true
mysqldump --single-transaction canvas event_log > event-log-data-before-v188.sql || true
```

## Restore

If rollback is required and the table was created by V188, drop it after confirming no production event reports were written after deployment:

```bash
mysql canvas -e "DROP TABLE IF EXISTS event_log"
```

If data existed before migration, restore the captured dump:

```bash
mysql canvas < event-log-schema-before-v188.sql
mysql canvas < event-log-data-before-v188.sql
```

## Dry run

Run on a MySQL 8 clone and verify clean Flyway migration plus event reporting smoke coverage:

```bash
cd backend && mvn -pl canvas-engine -am -P integration-tests -Dapi.version=1.54 -Dtest=CanvasExecutionRequestServiceIdempotencyIntegrationTest test
```

## Rollback owner

Backend/DBA owner. Coordinate with runtime platform because event ingestion, WAIT resume responses, and performance tracking read or write `event_log`.
