# Flyway Backup And Rollback Runbook

## Scope

Use this runbook before any production release that includes Flyway migrations. The release is blocked unless the migration policy script passes and high-risk migrations have backup notes.

## Backup

Capture a logical backup before deploying application code:

```bash
export BACKUP_DIR="docs/architecture/evidence/release-$(date +%Y%m%d-%H%M%S)"
mkdir -p "$BACKUP_DIR"
mysqldump \
  --single-transaction \
  --routines \
  --triggers \
  --set-gtid-purged=OFF \
  -h "$CANVAS_DB_HOST" \
  -P "${CANVAS_DB_PORT:-3306}" \
  -u "$CANVAS_DB_USER" \
  -p"$CANVAS_DB_PASSWORD" \
  "$CANVAS_DB_NAME" \
  > "$BACKUP_DIR/canvas-before-flyway.sql"
sha256sum "$BACKUP_DIR/canvas-before-flyway.sql" > "$BACKUP_DIR/canvas-before-flyway.sql.sha256"
```

Record the backup path in `CANVAS_MIGRATION_BACKUP_EVIDENCE` before running `scripts/release/pre-deploy-check.sh`.

## Dry run

Validate migration policy from the repository root:

```bash
bash scripts/release/check-flyway-migration.sh
```

For a release that starts from a known production version:

```bash
CANVAS_MIGRATION_BASE_VERSION=185 bash scripts/release/check-flyway-migration.sh
```

For database-level dry-run validation, restore the backup into staging and start the engine once with the target image and `SPRING_PROFILES_ACTIVE=staging`.

## Restore

Use restore only after the rollback owner approves the database decision point:

```bash
mysql \
  -h "$CANVAS_DB_HOST" \
  -P "${CANVAS_DB_PORT:-3306}" \
  -u "$CANVAS_DB_USER" \
  -p"$CANVAS_DB_PASSWORD" \
  "$CANVAS_DB_NAME" \
  < "$CANVAS_RESTORE_SQL"
```

After restore, run:

```bash
bash scripts/release/post-deploy-check.sh
```

## Rollback owner

Default rollback decision owner: Runtime lead with DBA approval.

The owner must decide whether rollback is application-only or application plus database restore. Application rollback can proceed with `scripts/release/rollback-drill.sh`; database restore requires the backup evidence path, restore command, and approval captured in the release evidence file.

## Evidence

Store release evidence under:

```text
docs/architecture/evidence/release-<yyyyMMdd-HHmmss>/
```

Minimum evidence:

- Migration policy output.
- Backup command and checksum path.
- Target image tag.
- Rollback owner and database restore decision.
- Post-deploy health and Prometheus check output.
