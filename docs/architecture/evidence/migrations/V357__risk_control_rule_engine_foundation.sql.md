# V357 risk control rule engine foundation

Migration: `V357__risk_control_rule_engine_foundation.sql`

Touched tables identified from the migration SQL:

- `risk_decision_run`
- `risk_list`
- `risk_list_entry`
- `risk_rule_hit`
- `risk_scene`
- `risk_strategy`
- `risk_strategy_version`

High-risk statements requiring release evidence:

- `UNIQUE KEY uk_risk_scene_tenant_key (tenant_id, scene_key),`
- `UNIQUE KEY uk_risk_strategy_tenant_key (tenant_id, strategy_key),`
- `UNIQUE KEY uk_risk_strategy_version (tenant_id, strategy_key, version),`
- `UNIQUE KEY uk_risk_list_tenant_key (tenant_id, list_key)`
- `UNIQUE KEY uk_risk_list_entry_subject (tenant_id, list_key, subject_hash),`
- `UNIQUE KEY uk_risk_decision_request (tenant_id, request_id),`

## Backup

Before applying this migration in staging or production, capture a consistent backup of Flyway history and the touched tables. Use the deployment database name and credentials from the release secret store.

```bash
export CANVAS_DB_NAME=canvas
export MIGRATION_ID=V357__risk_control_rule_engine_foundation
export TABLES="risk_decision_run risk_list risk_list_entry risk_rule_hit risk_scene risk_strategy risk_strategy_version"
mysqldump --single-transaction --set-gtid-purged=OFF "$CANVAS_DB_NAME" flyway_schema_history $TABLES > "backup-$MIGRATION_ID.sql"
sha256sum "backup-$MIGRATION_ID.sql" > "backup-$MIGRATION_ID.sql.sha256"
```

If a listed table does not exist before the migration because the migration creates it, keep the command output with that table removed and capture SHOW CREATE TABLE output for every existing dependency table referenced by the SQL.

## Restore

Rollback is restore-first for this migration family. Validate the backup on a clone before touching production, then restore the captured tables and Flyway history only with DBA approval.

```bash
export CANVAS_DB_NAME=canvas
export MIGRATION_ID=V357__risk_control_rule_engine_foundation
mysql "$CANVAS_DB_NAME" < "backup-$MIGRATION_ID.sql"
mysql "$CANVAS_DB_NAME" -e "SELECT version, success FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;"
```

If application writes have occurred after the migration, do not restore blindly. Stop writers, export post-migration deltas, and get Risk platform owner plus DBA approval for a merge or point-in-time restore.

## Dry run

Run the migration chain against a MySQL 8 clone using the boot-packaged migration directory before release. Keep the command output with the release evidence.

```bash
bash scripts/release/check-flyway-migration.sh
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -f backend/pom.xml -pl canvas-boot -am -DskipTests package
```

For production deploys, also run the full `scripts/release/pre-deploy-check.sh --dry-run` after this note is added.

## Rollback owner

Risk platform owner owns the functional rollback decision. Runtime platform owns Flyway execution ordering. DBA approval is required before restoring data or editing `flyway_schema_history`.
