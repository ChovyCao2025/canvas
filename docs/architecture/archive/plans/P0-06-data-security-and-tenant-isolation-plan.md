# Data Security And Tenant Isolation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Protect external datasource credentials and enforce tenant identity consistently across schema, data objects, query paths, list/get/mutation APIs, and response masking.

**Architecture:** Do not edit applied Flyway migrations. Add the next migration for credential and tenant corrections, keep encryption behind a small service abstraction, carry `tenantId` through core DO/DTO/query models, and verify cross-tenant denial at service and controller boundaries.

**Tech Stack:** Java 21, Spring Boot WebFlux, MyBatis-Plus, Flyway, Redis, JUnit 5, AssertJ, Maven.

**Implementation Status:** Focused backend scope implemented. Credential encryption/masking, demo credential cleanup, core tenant NOT NULL migration, datasource/execution-request tenant migration, canvas/data-source/execution-request tenant propagation, and cross-tenant read/mutation denial are implemented and verified. Remaining architectural hardening: credential key rotation, global row-policy/interceptor coverage, `system_option` global versus tenant semantics, and Audience/CDP tenant modeling.

---

## Source Material

- Spec: `../specs/P0-06-data-security-and-tenant-isolation-spec.md`
- Source package: `../todo/p0/data-security-and-tenant-isolation/`
- Coverage matrix: `../todo/coverage-matrix.md`

## File Structure

- Legacy evidence only: `backend/canvas-engine/src/main/resources/db/migration/V41__audience_demo_data.sql`
- Legacy evidence only: `backend/canvas-engine/src/main/resources/db/migration/V43__audience_demo_repoint_to_canvas_demo.sql`
- Legacy evidence only: `backend/canvas-engine/src/main/resources/db/migration/V71__data_source_config.sql`
- Legacy evidence only: `backend/canvas-engine/src/main/resources/db/migration/V78__saas_foundation.sql`
- New migration: `backend/canvas-engine/src/main/resources/db/migration/V91__sanitize_demo_datasource_credentials.sql`
- New migration: `backend/canvas-engine/src/main/resources/db/migration/V92__enforce_core_tenant_not_null.sql`
- New migration: `backend/canvas-engine/src/main/resources/db/migration/V93__tenant_scope_datasources_and_execution_requests.sql`
- Credential DO: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/DataSourceConfigDO.java`
- Credential service: `backend/canvas-engine/src/main/java/org/chovy/canvas/security/SecretCipher.java`
- Credential API: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/DataSourceConfigController.java`
- Tenant context: `backend/canvas-engine/src/main/java/org/chovy/canvas/common/tenant/TenantContext.java`
- Tenant context: `backend/canvas-engine/src/main/java/org/chovy/canvas/common/tenant/TenantContextResolver.java`
- Tenant schema model: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CanvasDO.java`
- Tenant service: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/tenant/TenantService.java`
- Tenant query: `backend/canvas-engine/src/main/java/org/chovy/canvas/query/CanvasListQuery.java`
- Canvas service: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasService.java`
- Execution request service: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/request/CanvasExecutionRequestService.java`
- Execution request API: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasExecutionRequestManagementController.java`
- Tests: `backend/canvas-engine/src/test/java/org/chovy/canvas/security/SecretCipherTest.java`
- Tests: `backend/canvas-engine/src/test/java/org/chovy/canvas/dal/dataobject/DataSourceConfigDOTest.java`
- Tests: `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/DataSourceConfigControllerTest.java`
- Tests: `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/CanvasExecutionRequestManagementControllerTest.java`
- Tests: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/request/CanvasExecutionRequestServiceTest.java`
- Tests: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/tenant/TenantServiceTest.java`
- Tests: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/CanvasUserQueryServiceTest.java`
- Evidence: `docs/architecture/evidence/P0-06-data-security-and-tenant-isolation.md`

### Task 1: Design credential storage

**Files:**
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/DataSourceConfigDO.java`
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/security/SecretCipher.java`
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/DataSourceConfigController.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/security/SecretCipherTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/dal/dataobject/DataSourceConfigDOTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/DataSourceConfigControllerTest.java`

- [x] Store datasource passwords as ciphertext or credential references, never plaintext response fields.
- [x] Keep encryption/decryption inside `SecretCipher` and API/service methods that explicitly need cleartext for connection checks.
- [x] Mask credential values in list/detail responses and cover create/update/read behavior.

Run:

```bash
cd backend && mvn -pl canvas-engine -Dtest=SecretCipherTest,DataSourceConfigDOTest,DataSourceConfigControllerTest test
# Do not stage or commit in this session unless the user explicitly asks.
```

Expected: credential tests pass; API tests show stored passwords are encrypted and returned passwords are masked.

### Task 2: Clean seed data with a forward migration

**Files:**
- Legacy evidence only: `backend/canvas-engine/src/main/resources/db/migration/V41__audience_demo_data.sql`
- Legacy evidence only: `backend/canvas-engine/src/main/resources/db/migration/V43__audience_demo_repoint_to_canvas_demo.sql`
- Legacy evidence only: `backend/canvas-engine/src/main/resources/db/migration/V71__data_source_config.sql`
- New migration: `backend/canvas-engine/src/main/resources/db/migration/V91__sanitize_demo_datasource_credentials.sql`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasExampleTemplateMigrationTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/dal/dataobject/DataSourceConfigDOTest.java`

- [x] Add a forward migration that neutralizes seeded `root/root` datasource credentials in upgraded databases.
- [x] Keep historical migrations unchanged and document the reason in the evidence file.
- [x] Ensure demo credentials used for local examples are synthetic and cannot be mistaken for production secrets.

Run:

```bash
cd backend && mvn -pl canvas-engine -Dtest=CanvasExampleTemplateMigrationTest,DataSourceConfigDOTest test
# Do not stage or commit in this session unless the user explicitly asks.
```

Expected: migration tests pass; evidence confirms V41, V43, and V71 were treated as immutable historical migrations.

### Task 3: Normalize tenant fields

**Files:**
- Legacy evidence only: `backend/canvas-engine/src/main/resources/db/migration/V78__saas_foundation.sql`
- New migration: `backend/canvas-engine/src/main/resources/db/migration/V92__enforce_core_tenant_not_null.sql`
- New migration: `backend/canvas-engine/src/main/resources/db/migration/V93__tenant_scope_datasources_and_execution_requests.sql`
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CanvasDO.java`
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CanvasExecutionDO.java`
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CanvasExecutionRequestDO.java`
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/DataSourceConfigDO.java`
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/query/CanvasListQuery.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/dal/dataobject/CoreTenantFieldMappingTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CoreTenantNotNullMigrationTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/tenant/TenantServiceTest.java`

- [x] Backfill tenant columns and move required isolation columns toward `NOT NULL` through the new migration.
- [x] Add `tenantId` fields to core DO/query models that map tables with tenant columns.
- [x] Ensure create/list/update paths populate tenant identity from `TenantContext`.

Run:

```bash
cd backend && mvn -pl canvas-engine -Dtest=TenantServiceTest test
# Do not stage or commit in this session unless the user explicitly asks.
```

Expected: tenant service tests pass and required tenant-scoped models expose `tenantId`.

### Task 4: Enforce tenant scope

**Files:**
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/common/tenant/TenantContext.java`
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/common/tenant/TenantContextResolver.java`
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasService.java`
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasOpsService.java`
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/tenant/TenantService.java`
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CanvasUserQueryService.java`
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/DataSourceConfigController.java`
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasExecutionRequestManagementController.java`
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/request/CanvasExecutionRequestService.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/tenant/TenantServiceTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/CanvasUserQueryServiceTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/DataSourceConfigControllerTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/CanvasExecutionRequestManagementControllerTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasTenantIsolationTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasOpsServiceTenantTest.java`

- [x] Apply tenant predicates to list, get, update, delete, and usage queries through shared helpers or explicit wrappers.
- [x] Reject cross-tenant mutations and reads at controller or service boundaries.
- [x] Cover default-tenant, missing-tenant, and cross-tenant denial cases.

Run:

```bash
cd backend && mvn -pl canvas-engine -Dtest=TenantServiceTest,CanvasUserQueryServiceTest,DataSourceConfigControllerTest test
# Do not stage or commit in this session unless the user explicitly asks.
```

Expected: cross-tenant tests fail before the scope change and pass after it; tenant-scoped APIs never return rows from another tenant.

### Task 5: Validate data security and tenant isolation

**Files:**
- Docs: `docs/architecture/evidence/P0-06-data-security-and-tenant-isolation.md`
- Plan: `docs/architecture/archive/plans/P0-06-data-security-and-tenant-isolation-plan.md`
- Spec: `docs/architecture/archive/specs/P0-06-data-security-and-tenant-isolation-spec.md`

- [x] Record credential storage, migration, tenant column, and cross-tenant test evidence.
- [x] Run the focused package test set.
- [x] Run the backend module suite after focused checks pass.

Run:

```bash
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH mvn -pl canvas-engine -Dtest=DataSourceConfigControllerTest,CanvasExecutionRequestManagementControllerTest,CanvasExecutionRequestServiceTest,CanvasExecutionRequestServiceIdempotencyTest,CoreTenantFieldMappingTest,CoreTenantNotNullMigrationTest,DemoDatasourceCredentialMigrationTest,DataSourceConfigDOTest,SecretCipherTest,JdbcConfigResolverTest,CanvasTenantIsolationTest,CanvasOpsServiceTenantTest,CanvasExecutionServiceResumeTest,ExecutionContextConcurrencyTest test
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH mvn -pl canvas-engine test
# Do not stage or commit in this session unless the user explicitly asks.
```

Expected: focused data-security tests and backend module tests pass; evidence names remaining architectural hardening that is outside the focused P0-06 implementation.
