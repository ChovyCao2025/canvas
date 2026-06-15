# Flyway Migration History Repair - 2026-06-06

## Problem

The integration history introduced duplicate Flyway numeric versions:

- `V91__data_security_and_tenant_isolation.sql`
- `V91__sanitize_demo_datasource_credentials.sql`
- `V92__execution_context_cold_backup.sql`
- `V92__enforce_core_tenant_not_null.sql`

Flyway versioned migrations are ordered by version, and a production migration directory must not contain two different `V<version>__*.sql` files with the same version. The resolved migration directory therefore keeps the security/cold-backup migrations at `V91` and `V92`, and moves the conflicting snapshot migrations forward:

- `V91__sanitize_demo_datasource_credentials.sql` -> `V272__sanitize_demo_datasource_credentials.sql`
- `V92__enforce_core_tenant_not_null.sql` -> `V273__enforce_core_tenant_not_null.sql`

`V93__tenant_scope_datasources_and_execution_requests.sql` is intentionally idempotent for `data_source_config.tenant_id` and `idx_data_source_tenant_type_enabled`, because `V91__data_security_and_tenant_isolation.sql` already owns that tenant-scope hardening in the merged sequence.

## Production Decision Rule

Use this resolved sequence only when the target environment has not already applied the conflicting snapshot migrations under the old `V91` or `V92` descriptions.

Before deploying to a persistent environment, inspect Flyway schema history:

```bash
DB_HOST=127.0.0.1 \
DB_PORT=3306 \
DB_NAME=canvas \
DB_USER=canvas_app \
DB_PASSWORD="$DB_PASSWORD" \
scripts/verify-flyway-history.sh
```

For CI or runbook drills, the verifier also accepts tab-separated `version	description	success` rows:

```bash
scripts/verify-flyway-history.sh --input-file /tmp/flyway-history.tsv
```

The manual SQL behind the verifier is:

```sql
SELECT version, description, checksum, success
FROM flyway_schema_history
WHERE version IN ('91', '92', '93', '272', '273')
ORDER BY installed_rank;
```

Allowed clean state:

- No records for versions `91`, `92`, `93`, `272`, or `273`.
- Or records for `91=data security and tenant isolation`, `92=execution context cold backup`, and `93=tenant scope datasources and execution requests`.

Stop and do not deploy automatically if schema history contains either old conflicting description:

- `sanitize demo datasource credentials` at version `91`
- `enforce core tenant not null` at version `92`

Those environments need an operator-led Flyway repair plan after proving the applied SQL is equivalent to the new forward versions. Do not silently change checksums on a live database.

## Verification

Repository gate:

```bash
cd backend
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home \
PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH \
mvn -pl canvas-boot -am test -Dtest=FlywayMigrationPolicyTest,DataSecurityMigrationTest,CoreTenantNotNullMigrationTest,DemoDatasourceCredentialMigrationTest -DfailIfNoTests=false
```

Expected:

- No duplicate numeric migration versions.
- `V272__sanitize_demo_datasource_credentials.sql` exists.
- `V273__enforce_core_tenant_not_null.sql` exists.
- The old conflicting `V91__sanitize...` and `V92__enforce...` files are absent from the runtime migration directory.
- `V93__tenant_scope_datasources_and_execution_requests.sql` keeps idempotent guards for `data_source_config` tenant scope.
- `scripts/verify-flyway-history.sh` rejects old conflicting Flyway history before automatic deployment.

## Follow-Up Rule

New schema/data changes must use a new version after the current highest migration. Do not reuse freed version numbers, and do not edit a migration after it has been applied in a shared or production environment.
