# Spec: Data Security And Tenant Isolation

Source package: `docs/architecture/reviewed-packages/p0/data-security-and-tenant-isolation/`

Coverage matrix: `docs/architecture/reviewed-packages/coverage-matrix.md`


## Verification Status

Implemented for the P0-06 focused backend scope, with focused verification passing for datasource credential handling, demo credential cleanup, core and runtime tenant field mapping, canvas/data-source/execution-request tenant propagation, and cross-tenant read/mutation rejection on the implemented management surfaces.

Verification evidence:

- `CoreTenantFieldMappingTest`
- `DemoDatasourceCredentialMigrationTest`
- `CanvasTenantIsolationTest`
- `CanvasOpsServiceTenantTest`
- `CoreTenantNotNullMigrationTest`
- `DataSourceConfigDOTest`
- `DataSourceConfigControllerTest`
- `CanvasExecutionRequestManagementControllerTest`
- `CanvasExecutionRequestServiceTest`
- `CanvasExecutionRequestServiceIdempotencyTest`
- `SecretCipherTest`
- `JdbcConfigResolverTest`
- `CanvasExecutionServiceResumeTest`
- `ExecutionContextConcurrencyTest`
- Command: `JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH mvn -pl canvas-engine -Dtest=DataSourceConfigControllerTest,CanvasExecutionRequestManagementControllerTest,CanvasExecutionRequestServiceTest,CanvasExecutionRequestServiceIdempotencyTest,CoreTenantFieldMappingTest,CoreTenantNotNullMigrationTest,DemoDatasourceCredentialMigrationTest,DataSourceConfigDOTest,SecretCipherTest,JdbcConfigResolverTest,CanvasTenantIsolationTest,CanvasOpsServiceTenantTest,CanvasExecutionServiceResumeTest,ExecutionContextConcurrencyTest test`
- Module command: `JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH mvn -pl canvas-engine test`

## Resolved Problems

- Datasource API paths encrypt stored passwords through `SecretCipher`, hide passwords from API JSON, and decrypt only through `JdbcConfigResolver`.
- `V91__sanitize_demo_datasource_credentials.sql` rewrites historical local demo `root/root` datasource rows to explicit local sample credentials.
- `V92__enforce_core_tenant_not_null.sql` backfills and enforces NOT NULL tenant columns on core user/canvas/execution tables.
- `V93__tenant_scope_datasources_and_execution_requests.sql` adds, backfills, indexes, and enforces NOT NULL tenant columns on `data_source_config` and `canvas_execution_request`.
- `CanvasDO`, `CanvasVersionDO`, `CanvasExecutionDO`, `CanvasExecutionTraceDO`, `CanvasExecutionRequestDO`, and `DataSourceConfigDO` now expose `tenantId` mapped to `tenant_id`.
- Canvas create/list paths carry tenant identity from `TenantContextResolver`; tenant-scoped lists add a tenant predicate.
- Canvas get/update/publish/offline/archive/version/kill/canary/rollback/clone/diff/safe-update paths enforce tenant access before reading or mutating tenant-owned canvases.
- Canvas draft/published versions, cloned canvases, canary versions, execution records, execution traces, and persisted execution requests inherit tenant identity from the canvas/execution context.
- Datasource config list/create/update/delete/table-introspection paths enforce tenant ownership and keep SUPER_ADMIN cross-tenant access explicit.
- Execution-request management list and replay paths enforce tenant ownership, preventing one tenant from inspecting or replaying another tenant's queued work.

## Remaining Problems

- Credential key rotation remains open; current encrypted datasource values rely on the configured active key.
- Tenant filtering is implemented explicitly on the covered service/controller surfaces; a shared row-policy interceptor is still needed to reduce future ad hoc query risk.
- `system_option.tenant_id` intentionally remains nullable until global versus tenant-scoped option semantics are separated.
- Audience/CDP definition tables are still legacy global models and need a dedicated tenant model before full multi-tenant CDP rollout.

## Evidence

- `backend/canvas-engine/src/main/java/org/chovy/canvas/security/SecretCipher.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/JdbcConfigResolver.java`
- `backend/canvas-engine/src/main/resources/db/migration/V91__sanitize_demo_datasource_credentials.sql`
- `backend/canvas-engine/src/main/resources/db/migration/V92__enforce_core_tenant_not_null.sql`
- `backend/canvas-engine/src/main/resources/db/migration/V93__tenant_scope_datasources_and_execution_requests.sql`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CanvasDO.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CanvasVersionDO.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CanvasExecutionDO.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CanvasExecutionTraceDO.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CanvasExecutionRequestDO.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/DataSourceConfigDO.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasController.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/DataSourceConfigController.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasExecutionRequestManagementController.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasOpsService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasTransactionService.java`
- `docs/architecture/evidence/P0-06-data-security-and-tenant-isolation.md`

## Acceptance Criteria

- External datasource credentials are encrypted or moved to a credential vault abstraction.
- Demo seed data does not introduce real-looking root credentials into production migrations.
- Tenant columns are non-null where tenant isolation is required.
- Core DO/DTO/query paths carry tenant identity consistently.
- Mutation and list APIs enforce tenant boundaries with tests.
