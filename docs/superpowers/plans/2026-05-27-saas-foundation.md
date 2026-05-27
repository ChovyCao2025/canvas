# SaaS Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add the first SaaS foundation: default tenant migration, tenant-aware users, JWT tenant claims, role expansion, tenant-scoped configuration, and tenant usage summary.

**Architecture:** Execute this in an isolated `feat/saas-foundation` worktree. Add tenant context as a small auth/domain layer, make high-risk service and controller entry points derive tenant from authenticated claims, and use `default` tenant migration to keep existing data usable. Avoid broad ORM magic in the first pass; make tenant filters explicit in services/controllers touched by this feature.

**Tech Stack:** Java 21, Spring Boot WebFlux, Spring Security, MyBatis-Plus, Flyway, MySQL 8, React 18, TypeScript, Ant Design 5, Vitest, Maven.

---

## Worktree Setup

- [ ] **Step 1: Create the isolated worktree**

Run from repository root using `superpowers:using-git-worktrees`:

```bash
git worktree add .worktrees/feat-saas-foundation -b feat/saas-foundation
cd .worktrees/feat-saas-foundation
```

Expected: worktree exists at `.worktrees/feat-saas-foundation` on branch `feat/saas-foundation`.

- [ ] **Step 2: Verify baseline compile**

```bash
cd backend/canvas-engine
mvn test -DskipTests
cd ../../frontend
npm test -- --run
```

Expected: Maven compilation succeeds. Frontend tests should pass or failures should be recorded as pre-existing before feature work starts.

## File Structure

### Backend

- Create: `backend/canvas-engine/src/main/resources/db/migration/V77__saas_foundation.sql`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/common/tenant/TenantContext.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/common/tenant/TenantContextResolver.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/common/tenant/RoleNames.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/TenantDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/TenantMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/tenant/TenantService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/tenant/TenantUsageDTO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/TenantController.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/SysUserDO.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/auth/domain/SysUserService.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/auth/dto/LoginResp.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/auth/dto/UserCreateReq.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/auth/util/JwtUtil.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/AuthController.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/AdminController.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/config/SecurityConfig.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/SystemOptionDO.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/SystemOptionService.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/SystemOptionController.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/auth/domain/SysUserServiceTenantTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/tenant/TenantServiceTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/meta/SystemOptionTenantScopeTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/config/SecurityConfigRoleTest.java`

### Frontend

- Modify: `frontend/src/services/api.ts`
- Modify: `frontend/src/context/AuthContext.tsx`
- Modify: `frontend/src/auth/guards.tsx`
- Modify: `frontend/src/pages/admin/index.tsx`
- Create: `frontend/src/pages/tenant-admin/index.tsx`
- Modify: `frontend/src/App.tsx`
- Modify: `frontend/src/components/layout/AppLayout.tsx`
- Test: `frontend/src/auth/guards.test.tsx`

## Task 1: Add Tenant Migration

**Files:**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V77__saas_foundation.sql`

- [ ] **Step 1: Add migration**

Create the migration with this SQL:

```sql
CREATE TABLE IF NOT EXISTS tenant (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_key VARCHAR(64) NOT NULL,
  name VARCHAR(128) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  plan_code VARCHAR(64) NOT NULL DEFAULT 'default',
  quota_json JSON NULL,
  remark VARCHAR(500) NULL,
  created_by VARCHAR(64) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by VARCHAR(64) NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_tenant_key (tenant_key),
  KEY idx_tenant_status (status)
);

INSERT INTO tenant (tenant_key, name, status, plan_code, quota_json, remark, created_by)
VALUES ('default', '默认租户', 'ACTIVE', 'default',
        JSON_OBJECT('maxBatchReplay', 100, 'maxCanvases', 1000),
        '迁移历史单租户数据', 'migration')
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  status = VALUES(status),
  plan_code = VALUES(plan_code);

ALTER TABLE sys_user
  ADD COLUMN tenant_id BIGINT NULL AFTER id;

UPDATE sys_user
SET tenant_id = (SELECT id FROM tenant WHERE tenant_key = 'default')
WHERE tenant_id IS NULL;

ALTER TABLE sys_user
  MODIFY tenant_id BIGINT NOT NULL,
  MODIFY role VARCHAR(32) NOT NULL COMMENT 'SUPER_ADMIN / TENANT_ADMIN / OPERATOR',
  ADD INDEX idx_sys_user_tenant (tenant_id, enabled);

UPDATE sys_user SET role = 'SUPER_ADMIN' WHERE role = 'ADMIN';
UPDATE sys_user SET role = 'OPERATOR' WHERE role = 'OPERATOR';

ALTER TABLE system_option
  ADD COLUMN tenant_id BIGINT NULL AFTER id,
  DROP INDEX uk_system_option_category_key,
  ADD UNIQUE KEY uk_system_option_tenant_category_key (tenant_id, category, option_key),
  ADD KEY idx_system_option_tenant_category_enabled_sort (tenant_id, category, enabled, sort_order, id);

ALTER TABLE canvas ADD COLUMN tenant_id BIGINT NULL AFTER id;
UPDATE canvas SET tenant_id = (SELECT id FROM tenant WHERE tenant_key = 'default') WHERE tenant_id IS NULL;
ALTER TABLE canvas MODIFY tenant_id BIGINT NOT NULL, ADD KEY idx_canvas_tenant_status (tenant_id, status, updated_at);

ALTER TABLE canvas_version ADD COLUMN tenant_id BIGINT NULL AFTER id;
UPDATE canvas_version v JOIN canvas c ON c.id = v.canvas_id SET v.tenant_id = c.tenant_id WHERE v.tenant_id IS NULL;
ALTER TABLE canvas_version MODIFY tenant_id BIGINT NOT NULL, ADD KEY idx_canvas_version_tenant_canvas (tenant_id, canvas_id, version);

ALTER TABLE canvas_execution ADD COLUMN tenant_id BIGINT NULL AFTER id;
UPDATE canvas_execution e JOIN canvas c ON c.id = e.canvas_id SET e.tenant_id = c.tenant_id WHERE e.tenant_id IS NULL;
ALTER TABLE canvas_execution MODIFY tenant_id BIGINT NOT NULL, ADD KEY idx_execution_tenant_canvas_created (tenant_id, canvas_id, created_at);

ALTER TABLE canvas_execution_trace ADD COLUMN tenant_id BIGINT NULL AFTER id;
UPDATE canvas_execution_trace t JOIN canvas_execution e ON e.id = t.execution_id SET t.tenant_id = e.tenant_id WHERE t.tenant_id IS NULL;
ALTER TABLE canvas_execution_trace MODIFY tenant_id BIGINT NOT NULL, ADD KEY idx_trace_tenant_execution (tenant_id, execution_id);
```

- [ ] **Step 2: Run Flyway schema test**

```bash
cd backend/canvas-engine
mvn test -Dtest=FlywayConfigTest -q
```

Expected: PASS. If it fails because the repository already has a newer dirty migration in another worktree, stop and inspect migration ordering before continuing.

- [ ] **Step 3: Commit migration**

```bash
git add backend/canvas-engine/src/main/resources/db/migration/V77__saas_foundation.sql
git commit -m "feat: add tenant foundation migration"
```

## Task 2: Tenant Context and JWT Claims

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/common/tenant/TenantContext.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/common/tenant/TenantContextResolver.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/common/tenant/RoleNames.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/auth/util/JwtUtil.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/auth/dto/LoginResp.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/AuthController.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/SysUserDO.java`

- [ ] **Step 1: Write focused JWT test**

Add assertions to `backend/canvas-engine/src/test/java/org/chovy/canvas/auth/domain/SysUserServiceTenantTest.java`:

```java
@Test
void jwtIncludesTenantAndRoleClaims() {
    SysUserDO user = new SysUserDO();
    user.setId(7L);
    user.setTenantId(3L);
    user.setUsername("tenant_admin");
    user.setDisplayName("Tenant Admin");
    user.setRole("TENANT_ADMIN");

    JwtUtil jwtUtil = new JwtUtil("canvas-engine-jwt-secret-key-must-be-at-least-256-bits", 24);
    Claims claims = jwtUtil.parse(jwtUtil.generate(user));

    assertThat(claims.getSubject()).isEqualTo("7");
    assertThat(claims.get("tenantId", Long.class)).isEqualTo(3L);
    assertThat(claims.get("role", String.class)).isEqualTo("TENANT_ADMIN");
}
```

- [ ] **Step 2: Add tenant classes**

```java
package org.chovy.canvas.common.tenant;

public record TenantContext(Long tenantId, String role, String username) {
    public boolean isSuperAdmin() {
        return RoleNames.SUPER_ADMIN.equals(role);
    }
}
```

```java
package org.chovy.canvas.common.tenant;

public final class RoleNames {
    public static final String SUPER_ADMIN = "SUPER_ADMIN";
    public static final String TENANT_ADMIN = "TENANT_ADMIN";
    public static final String OPERATOR = "OPERATOR";

    private RoleNames() {}
}
```

```java
package org.chovy.canvas.common.tenant;

import io.jsonwebtoken.Claims;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class TenantContextResolver {
    public Mono<TenantContext> current() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> (Claims) ctx.getAuthentication().getPrincipal())
                .map(claims -> new TenantContext(
                        claims.get("tenantId", Long.class),
                        claims.get("role", String.class),
                        claims.get("username", String.class)));
    }
}
```

- [ ] **Step 3: Update JWT and login response**

Add `tenantId` to `SysUserDO`, `LoginResp`, and frontend `LoginResp`. Update `JwtUtil.generate`:

```java
.claim("tenantId", user.getTenantId())
.claim("role", user.getRole())
```

Update `AuthController.login` and `AuthController.me` to set `resp.setTenantId(user.getTenantId())`.

- [ ] **Step 4: Run tests**

```bash
cd backend/canvas-engine
mvn test -Dtest=SysUserServiceTenantTest -q
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/common/tenant \
  backend/canvas-engine/src/main/java/org/chovy/canvas/auth \
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/AuthController.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/SysUserDO.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/auth/domain/SysUserServiceTenantTest.java
git commit -m "feat: add tenant claims to auth context"
```

## Task 3: Tenant and User Administration

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/TenantDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/TenantMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/tenant/TenantService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/tenant/TenantUsageDTO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/TenantController.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/auth/domain/SysUserService.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/AdminController.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/tenant/TenantServiceTest.java`

- [ ] **Step 1: Write tenant service tests**

```java
@Test
void createTenantNormalizesKeyAndSetsActiveStatus() {
    TenantMapper mapper = mock(TenantMapper.class);
    CanvasMapper canvasMapper = mock(CanvasMapper.class);
    CanvasExecutionMapper executionMapper = mock(CanvasExecutionMapper.class);
    CanvasExecutionDlqMapper dlqMapper = mock(CanvasExecutionDlqMapper.class);
    SysUserMapper userMapper = mock(SysUserMapper.class);
    TenantService service = new TenantService(mapper, canvasMapper, executionMapper, dlqMapper, userMapper);

    TenantDO tenant = service.create("Acme Travel", "acme", "pro", "{\"maxBatchReplay\":50}", "owner");

    assertThat(tenant.getTenantKey()).isEqualTo("acme");
    assertThat(tenant.getStatus()).isEqualTo("ACTIVE");
    verify(mapper).insert(tenant);
}
```

- [ ] **Step 2: Implement `TenantDO` and `TenantMapper`**

Use `@TableName("tenant")`, `@TableId(type = IdType.AUTO)`, and fields matching the migration: `id`, `tenantKey`, `name`, `status`, `planCode`, `quotaJson`, `remark`, `createdBy`, `createdAt`, `updatedBy`, `updatedAt`.

- [ ] **Step 3: Implement `TenantService`**

Public methods:

```java
public TenantDO create(String name, String tenantKey, String planCode, String quotaJson, String operator)
public void disable(Long id, String operator)
public void activate(Long id, String operator)
public TenantUsageDTO usage(Long tenantId)
public List<TenantDO> list()
```

`usage` should count canvases, published canvases, executions, failed executions, and DLQ rows with explicit tenant filters.

- [ ] **Step 4: Add `/admin/tenants` controller**

Routes:

- `GET /admin/tenants`
- `POST /admin/tenants`
- `PUT /admin/tenants/{id}/disable`
- `PUT /admin/tenants/{id}/activate`
- `GET /admin/tenants/{id}/usage`

All routes require `SUPER_ADMIN`.

- [ ] **Step 5: Update `SysUserService` role validation**

Allowed roles become:

```java
Set.of(RoleNames.SUPER_ADMIN, RoleNames.TENANT_ADMIN, RoleNames.OPERATOR)
```

`create` must require `tenantId`; `TENANT_ADMIN` callers can only create users in their own tenant.

- [ ] **Step 6: Run focused tests**

```bash
cd backend/canvas-engine
mvn test -Dtest=TenantServiceTest,SysUserServiceTenantTest -q
```

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/TenantDO.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/TenantMapper.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/tenant \
  backend/canvas-engine/src/main/java/org/chovy/canvas/dto/tenant \
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/TenantController.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/auth/domain/SysUserService.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/AdminController.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/tenant/TenantServiceTest.java
git commit -m "feat: add tenant administration"
```

## Task 4: Tenant-Scoped System Options

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/SystemOptionDO.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/SystemOptionService.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/SystemOptionController.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/meta/SystemOptionTenantScopeTest.java`

- [ ] **Step 1: Write tenant option fallback test**

```java
@Test
void activeOptionsPrefersTenantOverrideAndFallsBackToDefault() {
    SystemOptionMapper mapper = mock(SystemOptionMapper.class);
    SystemOptionDO tenantValue = option(9L, "execution_governance", "max_batch_replay", "50");
    SystemOptionDO defaultValue = option(null, "execution_governance", "max_batch_replay", "100");
    when(mapper.selectList(any())).thenReturn(List.of(tenantValue), List.of(defaultValue));

    SystemOptionService service = new SystemOptionService(mapper);

    List<SystemOptionDO> rows = service.activeSystemOptions("execution_governance", 9L);

    assertThat(rows).containsExactly(tenantValue);
}

private SystemOptionDO option(Long tenantId, String category, String optionKey, String label) {
    SystemOptionDO option = new SystemOptionDO();
    option.setTenantId(tenantId);
    option.setCategory(category);
    option.setOptionKey(optionKey);
    option.setLabel(label);
    option.setEnabled(1);
    option.setSortOrder(10);
    return option;
}
```

- [ ] **Step 2: Add `tenantId` field to `SystemOptionDO`**

```java
private Long tenantId;
```

- [ ] **Step 3: Add tenant-aware methods**

Add overloads:

```java
public List<SystemOptionDO> activeSystemOptions(String category, Long tenantId)
public List<StubOption> activeOptions(String category, Long tenantId)
public List<SystemOptionDO> listForAdmin(String category, Integer enabled, String keyword, Long tenantId, boolean superAdmin)
```

Rules:

- Tenant users see rows where `tenant_id = currentTenantId` or `tenant_id IS NULL`.
- `SUPER_ADMIN` can pass `tenantId`; if absent, sees all rows.
- Active lookup prefers tenant rows for duplicate `category + optionKey`.

- [ ] **Step 4: Update controller**

`SystemOptionController.list` must resolve current tenant and role, then call tenant-aware service. `update` must deny editing rows from another tenant unless `SUPER_ADMIN`.

- [ ] **Step 5: Run tests**

```bash
cd backend/canvas-engine
mvn test -Dtest=SystemOptionTenantScopeTest -q
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/SystemOptionDO.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/SystemOptionService.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/SystemOptionController.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/meta/SystemOptionTenantScopeTest.java
git commit -m "feat: scope system options by tenant"
```

## Task 5: Frontend Role and Tenant Awareness

**Files:**
- Modify: `frontend/src/services/api.ts`
- Modify: `frontend/src/context/AuthContext.tsx`
- Modify: `frontend/src/auth/guards.tsx`
- Modify: `frontend/src/App.tsx`
- Modify: `frontend/src/components/layout/AppLayout.tsx`
- Create: `frontend/src/pages/tenant-admin/index.tsx`
- Test: `frontend/src/auth/guards.test.tsx`

- [ ] **Step 1: Update frontend auth types**

`LoginResp.role` becomes:

```ts
export type UserRole = 'SUPER_ADMIN' | 'TENANT_ADMIN' | 'OPERATOR'
```

`LoginResp` gains:

```ts
tenantId: number
```

- [ ] **Step 2: Update auth helpers**

`AuthContext` exposes:

```ts
isSuperAdmin: user?.role === 'SUPER_ADMIN'
isTenantAdmin: user?.role === 'TENANT_ADMIN'
isAdmin: user?.role === 'SUPER_ADMIN' || user?.role === 'TENANT_ADMIN'
```

- [ ] **Step 3: Update route guards**

`RequireAdmin` accepts tenant admin and super admin. Add `RequireSuperAdmin` for `/admin/tenants`.

- [ ] **Step 4: Add tenant admin page**

`frontend/src/pages/tenant-admin/index.tsx` renders tenant list and usage using new `tenantApi` methods in `services/api.ts`.

- [ ] **Step 5: Run frontend tests**

```bash
cd frontend
npm test -- --run src/auth/guards.test.tsx
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add frontend/src/services/api.ts frontend/src/context/AuthContext.tsx frontend/src/auth/guards.tsx \
  frontend/src/App.tsx frontend/src/components/layout/AppLayout.tsx frontend/src/pages/tenant-admin \
  frontend/src/auth/guards.test.tsx
git commit -m "feat: add tenant-aware frontend auth"
```

## Final Verification

- [ ] **Step 1: Backend verification**

```bash
cd backend/canvas-engine
mvn test -Dtest=SysUserServiceTenantTest,TenantServiceTest,SystemOptionTenantScopeTest,SecurityConfigRoleTest -q
```

Expected: PASS.

- [ ] **Step 2: Frontend verification**

```bash
cd frontend
npm test -- --run src/auth/guards.test.tsx
npm run build
```

Expected: PASS and production build succeeds.

- [ ] **Step 3: Record status**

```bash
git status --short
```

Expected: only intentional untracked build artifacts if the project already produces them; no unstaged source changes.
