# P0-06 Data Security And Tenant Isolation Evidence

## Implemented Behavior

- Datasource passwords are encrypted through `SecretCipher`, excluded from API JSON responses, and decrypted only when JDBC connection metadata or audience compute code explicitly needs cleartext.
- `V91__sanitize_demo_datasource_credentials.sql` rewrites historical demo `root/root` datasource credentials in upgraded databases without editing immutable historical migrations.
- `V92__enforce_core_tenant_not_null.sql` backfills and enforces `tenant_id NOT NULL` on `sys_user`, `canvas`, `canvas_version`, `canvas_execution`, and `canvas_execution_trace`.
- `V93__tenant_scope_datasources_and_execution_requests.sql` adds, backfills, indexes, and enforces `tenant_id NOT NULL` on `data_source_config` and `canvas_execution_request`.
- `CanvasDO`, `CanvasVersionDO`, `CanvasExecutionDO`, `CanvasExecutionTraceDO`, `CanvasExecutionRequestDO`, `DataSourceConfigDO`, and `ExecutionContext` now carry tenant identity.
- Canvas create/list/get/update/publish/offline/archive/version/kill/canary/rollback/clone/diff/safe-update paths enforce tenant boundaries through `TenantContextResolver` and `CanvasService.requireTenantAccess(...)`.
- `CanvasOpsService` preserves tenant identity when creating canary versions and cloned canvases/drafts.
- Execution request enqueue writes `tenant_id` from the target canvas, and execution-request management list/replay paths filter or reject by tenant.
- Datasource config list/create/update/delete/table-introspection paths filter, stamp, preserve, or reject by tenant; SUPER_ADMIN keeps cross-tenant visibility.
- FORCE kill updates execution-request queue state through a tenant-aware mapper path when tenant identity is present.

## Focused Verification

Command:

```bash
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH mvn -pl canvas-engine -Dtest=DataSourceConfigControllerTest,CanvasExecutionRequestManagementControllerTest,CanvasExecutionRequestServiceTest,CanvasExecutionRequestServiceIdempotencyTest,CoreTenantFieldMappingTest,CoreTenantNotNullMigrationTest,DemoDatasourceCredentialMigrationTest,DataSourceConfigDOTest,SecretCipherTest,JdbcConfigResolverTest,CanvasTenantIsolationTest,CanvasOpsServiceTenantTest,CanvasExecutionServiceResumeTest,ExecutionContextConcurrencyTest test
```

Result: 57 tests, 0 failures, 0 errors.

Module command:

```bash
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH mvn -pl canvas-engine test
```

Result: 439 tests, 0 failures, 0 errors.

Expected log noise:

- `CanvasExecutionServiceResumeTest` logs missing Redis context failures intentionally.
- `GlobalExceptionHandlerTest`, `ApiCallHandlerRateLimitTest`, and `AsyncTaskServiceTest` log synthetic exceptions intentionally.

## Remaining Risk

- Credential key rotation is not implemented; encrypted values currently depend on the configured active key.
- Tenant isolation still relies on explicit service/controller predicates for the implemented surfaces, not a global MyBatis row-policy interceptor.
- `system_option.tenant_id` remains nullable because the table still carries global option semantics.
- Audience/CDP definition tables are still legacy global models; they need their own tenant model before full multi-tenant CDP rollout.
