# Project Governance Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add tenant-scoped Project governance so canvases can be grouped under long-lived Projects with members, simple roles, defaults, and statistics.

**Architecture:** Project governance is added as a focused layer over the existing tenant and canvas model. `CanvasProjectService` owns project CRUD, membership, defaults, and stats; `CanvasProjectPermissionService` owns role checks; `CanvasProjectFolderMetadataService` remains the compatibility layer for canvas Project/Folder assignment.

**Tech Stack:** Java 21, Spring Boot WebFlux, MyBatis-Plus, Flyway MySQL migrations, Reactor, React, TypeScript, Ant Design, Vitest, JUnit 5, AssertJ, Mockito.

---

## File Structure

- Create `backend/canvas-engine/src/main/resources/db/migration/V189__project_governance.sql`: creates/repairs Project, member, and assignment schema.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CanvasProjectDO.java`: maps `canvas_project`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CanvasProjectMemberDO.java`: maps `canvas_project_member`.
- Modify `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CanvasProjectFolderDO.java`: adds `tenantId` and `projectId`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CanvasProjectMapper.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CanvasProjectMemberMapper.java`.
- Modify `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/CanvasCreateReq.java`: accepts project assignment and defaultable runtime limits.
- Modify `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/canvas/ProjectFolderMetadataReq.java` and `ProjectFolderMetadataResp.java`: add `projectId`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/project/*.java`: request and response DTOs for Project APIs.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/project/CanvasProjectRole.java`: role/action vocabulary.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/project/CanvasProjectService.java`: Project CRUD, members, stats, canvas listing.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/project/CanvasProjectPermissionService.java`: tenant/admin/member action checks.
- Modify `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasProjectFolderMetadataService.java`: project-aware assignment reads/writes.
- Modify `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasService.java`: project defaults during create and project-filtered lists.
- Modify `backend/canvas-engine/src/main/java/org/chovy/canvas/query/CanvasListQuery.java`: adds `projectId`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasProjectController.java`: `/admin/projects` API.
- Modify `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasController.java`: project permission checks and metadata payload wiring.
- Modify `backend/canvas-engine/src/main/java/org/chovy/canvas/web/ExecutionController.java`: dry-run permission check; external signed direct/behavior trigger remains governed by tenant/signature and does not require project membership.
- Create backend tests under `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/project/` and modify existing canvas metadata tests.
- Modify `frontend/src/services/api.ts`: project types and API client methods.
- Create `frontend/src/services/projectApi.test.ts`: verifies endpoint payloads.
- Create `frontend/src/pages/projects/projectPresentation.ts` and `.test.ts`: role/status/stat formatting.
- Create `frontend/src/pages/projects/index.tsx`: project list and detail tabs.
- Modify `frontend/src/App.tsx` and `frontend/src/components/layout/AppLayout.tsx`: project route and menu item.
- Modify `frontend/src/pages/canvas-list/index.tsx`: Project filter and create form assignment.

## Scope Notes

- Project is a long-lived governance unit inside Tenant.
- Folder remains a navigation/category field inside Project.
- First phase governs canvas management and dry-run only.
- Signed production triggers (`/canvas/execute/direct/{canvasId}` and `/canvas/trigger/behavior`) are not blocked by project membership because they represent external runtime traffic, not console user actions.
- Disabled Projects block management actions and publishing, but they do not stop already published canvases from executing.

---

### Task 1: Schema and Mapping Foundation

**Files:**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V189__project_governance.sql`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CanvasProjectDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CanvasProjectMemberDO.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CanvasProjectFolderDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CanvasProjectMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CanvasProjectMemberMapper.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/migration/ProjectGovernanceMigrationTest.java`

- [ ] **Step 1: Write failing migration test**

Create `ProjectGovernanceMigrationTest.java`:

```java
package org.chovy.canvas.migration;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectGovernanceMigrationTest {

    @Test
    void migrationDefinesProjectMemberAndAssignmentTables() throws Exception {
        String sql = Files.readString(Path.of(
                "src/main/resources/db/migration/V189__project_governance.sql"));

        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS canvas_project");
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS canvas_project_member");
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS canvas_project_folder");
        assertThat(sql).contains("ADD COLUMN project_id BIGINT NULL");
        assertThat(sql).contains("uk_canvas_project_tenant_key");
        assertThat(sql).contains("uk_canvas_project_member_project_user");
    }
}
```

- [ ] **Step 2: Run test and verify RED**

Run:

```bash
cd backend && mvn -q -pl canvas-engine -Dtest=ProjectGovernanceMigrationTest test
```

Expected: FAIL because `V189__project_governance.sql` does not exist.

- [ ] **Step 3: Add migration**

Create `V189__project_governance.sql` with:

```sql
CREATE TABLE IF NOT EXISTS canvas_project (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  project_key VARCHAR(80) NOT NULL,
  project_name VARCHAR(160) NOT NULL,
  description VARCHAR(500) NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  default_settings_json JSON NULL,
  require_review_before_publish TINYINT NOT NULL DEFAULT 0,
  quiet_hours_json JSON NULL,
  created_by VARCHAR(64) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by VARCHAR(64) NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_canvas_project_tenant_key (tenant_id, project_key),
  KEY idx_canvas_project_tenant_status (tenant_id, status, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户内项目治理域';

CREATE TABLE IF NOT EXISTS canvas_project_member (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  project_id BIGINT NOT NULL,
  user_id BIGINT NULL,
  username VARCHAR(64) NOT NULL,
  role VARCHAR(32) NOT NULL,
  source VARCHAR(32) NOT NULL DEFAULT 'MANUAL',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_canvas_project_member_project_user (project_id, username),
  KEY idx_canvas_project_member_tenant_user (tenant_id, username),
  KEY idx_canvas_project_member_project_role (project_id, role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目成员与项目角色';

CREATE TABLE IF NOT EXISTS canvas_project_folder (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NULL,
  canvas_id BIGINT NOT NULL,
  project_id BIGINT NULL,
  project_key VARCHAR(80) NULL,
  project_name VARCHAR(160) NULL,
  folder_key VARCHAR(80) NULL,
  folder_name VARCHAR(160) NULL,
  updated_by VARCHAR(64) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_canvas_project_folder_canvas (canvas_id),
  KEY idx_canvas_project_folder_project (tenant_id, project_id, folder_key),
  KEY idx_canvas_project_folder_key (tenant_id, project_key, folder_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='画布项目和文件夹归属';

SET @cpf_tenant_exists := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'canvas_project_folder' AND column_name = 'tenant_id'
);
SET @cpf_tenant_sql := IF(@cpf_tenant_exists = 0,
  "ALTER TABLE canvas_project_folder ADD COLUMN tenant_id BIGINT NULL AFTER id",
  "SELECT 1");
PREPARE cpf_tenant_stmt FROM @cpf_tenant_sql;
EXECUTE cpf_tenant_stmt;
DEALLOCATE PREPARE cpf_tenant_stmt;

SET @cpf_project_exists := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'canvas_project_folder' AND column_name = 'project_id'
);
SET @cpf_project_sql := IF(@cpf_project_exists = 0,
  "ALTER TABLE canvas_project_folder ADD COLUMN project_id BIGINT NULL AFTER canvas_id",
  "SELECT 1");
PREPARE cpf_project_stmt FROM @cpf_project_sql;
EXECUTE cpf_project_stmt;
DEALLOCATE PREPARE cpf_project_stmt;

UPDATE canvas_project_folder cpf
JOIN canvas c ON c.id = cpf.canvas_id
SET cpf.tenant_id = c.tenant_id
WHERE cpf.tenant_id IS NULL;

INSERT INTO canvas_project (tenant_id, project_key, project_name, created_by)
SELECT DISTINCT cpf.tenant_id, cpf.project_key, COALESCE(NULLIF(cpf.project_name, ''), cpf.project_key), 'migration'
FROM canvas_project_folder cpf
WHERE cpf.tenant_id IS NOT NULL
  AND cpf.project_key IS NOT NULL
  AND cpf.project_key <> ''
ON DUPLICATE KEY UPDATE
  project_name = VALUES(project_name);

UPDATE canvas_project_folder cpf
JOIN canvas_project p ON p.tenant_id = cpf.tenant_id AND p.project_key = cpf.project_key
SET cpf.project_id = p.id
WHERE cpf.project_id IS NULL
  AND cpf.project_key IS NOT NULL
  AND cpf.project_key <> '';
```

- [ ] **Step 4: Add data objects and mappers**

Create `CanvasProjectDO.java`:

```java
package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("canvas_project")
public class CanvasProjectDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private String projectKey;
    private String projectName;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String description;
    private String status;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String defaultSettingsJson;
    private Integer requireReviewBeforePublish;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String quietHoursJson;
    private String createdBy;
    private String updatedBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
```

Create `CanvasProjectMemberDO.java`:

```java
package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("canvas_project_member")
public class CanvasProjectMemberDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long projectId;
    private Long userId;
    private String username;
    private String role;
    private String source;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
```

Add these fields to `CanvasProjectFolderDO`:

```java
private Long tenantId;
private Long projectId;
```

Create mappers:

```java
package org.chovy.canvas.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.chovy.canvas.dal.dataobject.CanvasProjectDO;

@Mapper
public interface CanvasProjectMapper extends BaseMapper<CanvasProjectDO> {
}
```

```java
package org.chovy.canvas.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.chovy.canvas.dal.dataobject.CanvasProjectMemberDO;

@Mapper
public interface CanvasProjectMemberMapper extends BaseMapper<CanvasProjectMemberDO> {
}
```

- [ ] **Step 5: Run test and verify GREEN**

Run:

```bash
cd backend && mvn -q -pl canvas-engine -Dtest=ProjectGovernanceMigrationTest test
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add backend/canvas-engine/src/main/resources/db/migration/V189__project_governance.sql \
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CanvasProjectDO.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CanvasProjectMemberDO.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CanvasProjectFolderDO.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CanvasProjectMapper.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CanvasProjectMemberMapper.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/migration/ProjectGovernanceMigrationTest.java
git commit -m "feat: add project governance schema"
```

### Task 2: Project DTOs, Roles, and Service

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/project/CanvasProjectRole.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/project/ProjectCreateReq.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/project/ProjectUpdateReq.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/project/ProjectMemberUpdateReq.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/project/ProjectSummaryResp.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/project/ProjectDetailResp.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/project/ProjectMemberResp.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/project/ProjectStatsResp.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/project/CanvasProjectService.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/project/CanvasProjectServiceTest.java`

- [ ] **Step 1: Write failing service tests**

Create tests proving:

```java
@Test
void createNormalizesProjectKeyAndDefaultsStatus() {
    CanvasProjectMapper projectMapper = mock(CanvasProjectMapper.class);
    CanvasProjectService service = service(projectMapper);

    ProjectCreateReq req = new ProjectCreateReq(
            " Growth Team ", " Growth Team ", "member growth", null,
            0, null, "alice");
    ProjectDetailResp resp = service.create(9L, req);

    ArgumentCaptor<CanvasProjectDO> captor = ArgumentCaptor.forClass(CanvasProjectDO.class);
    verify(projectMapper).insert(captor.capture());
    assertThat(captor.getValue().getTenantId()).isEqualTo(9L);
    assertThat(captor.getValue().getProjectKey()).isEqualTo("growth-team");
    assertThat(captor.getValue().getStatus()).isEqualTo("ACTIVE");
    assertThat(resp.projectKey()).isEqualTo("growth-team");
}

@Test
void setMemberRejectsUnknownRole() {
    CanvasProjectService service = service(mock(CanvasProjectMapper.class));

    assertThatThrownBy(() -> service.setMember(9L, 11L, 5L,
            new ProjectMemberUpdateReq("alice", "OWNER")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported project role");
}
```

- [ ] **Step 2: Run test and verify RED**

```bash
cd backend && mvn -q -pl canvas-engine -Dtest=CanvasProjectServiceTest test
```

Expected: FAIL because DTOs and service do not exist.

- [ ] **Step 3: Implement DTOs and role enum**

Use Java records with validation annotations. `CanvasProjectRole` must contain:

```java
public enum CanvasProjectRole {
    PROJECT_ADMIN, EDITOR, EXECUTOR, VIEWER;

    public static CanvasProjectRole parse(String raw) {
        try {
            return CanvasProjectRole.valueOf(raw == null ? "" : raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unsupported project role: " + raw);
        }
    }
}
```

- [ ] **Step 4: Implement `CanvasProjectService`**

Core methods:

```java
public List<ProjectSummaryResp> list(Long tenantId, String username, boolean superAdmin, boolean tenantAdmin);
public ProjectDetailResp create(Long tenantId, ProjectCreateReq req);
public ProjectDetailResp update(Long tenantId, Long projectId, ProjectUpdateReq req);
public void disable(Long tenantId, Long projectId, String operator);
public List<ProjectMemberResp> listMembers(Long tenantId, Long projectId);
public ProjectMemberResp setMember(Long tenantId, Long projectId, Long userId, ProjectMemberUpdateReq req);
public void removeMember(Long tenantId, Long projectId, Long userId);
public ProjectStatsResp stats(Long tenantId, Long projectId);
```

Use helper methods:

```java
private String normalizeKey(String value) {
    String trimmed = requireText(value, "projectKey").trim().toLowerCase();
    return trimmed.replaceAll("[^a-z0-9_-]+", "-").replaceAll("(^-|-$)", "");
}
```

- [ ] **Step 5: Run test and verify GREEN**

```bash
cd backend && mvn -q -pl canvas-engine -Dtest=CanvasProjectServiceTest test
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/domain/project \
  backend/canvas-engine/src/main/java/org/chovy/canvas/dto/project \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/project/CanvasProjectServiceTest.java
git commit -m "feat: add project service"
```

### Task 3: Project Assignment Compatibility

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/canvas/ProjectFolderMetadataReq.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/canvas/ProjectFolderMetadataResp.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasProjectFolderMetadataService.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasProjectFolderMetadataServiceTest.java`

- [ ] **Step 1: Update failing metadata tests**

Add assertions:

```java
var resp = service.saveMetadata(9L, 62L, new ProjectFolderMetadataReq(
        11L, " growth ", " Growth ", " new-user ", " New User ", " alice "));
assertThat(resp.projectId()).isEqualTo(11L);
```

Add a test that saving by `projectId` fills project key/name from `canvas_project`.

- [ ] **Step 2: Run test and verify RED**

```bash
cd backend && mvn -q -pl canvas-engine -Dtest=CanvasProjectFolderMetadataServiceTest test
```

Expected: FAIL because request/response records lack `projectId` and service lacks tenant-aware overloads.

- [ ] **Step 3: Extend records**

`ProjectFolderMetadataReq`:

```java
public record ProjectFolderMetadataReq(
        Long projectId,
        String projectKey,
        String projectName,
        String folderKey,
        String folderName,
        String operator
) {}
```

`ProjectFolderMetadataResp`:

```java
public record ProjectFolderMetadataResp(
        Long canvasId,
        Long projectId,
        String projectKey,
        String projectName,
        String folderKey,
        String folderName
) {}
```

- [ ] **Step 4: Implement tenant-aware service overloads**

Add:

```java
public ProjectFolderMetadataResp getMetadata(Long canvasId);
public ProjectFolderMetadataResp getMetadata(Long tenantId, Long canvasId);
public ProjectFolderMetadataResp saveMetadata(Long tenantId, Long canvasId, ProjectFolderMetadataReq req);
```

When `projectId` is present, load `CanvasProjectDO` by tenant/id and copy `projectKey/projectName` into the assignment row.

- [ ] **Step 5: Run test and verify GREEN**

```bash
cd backend && mvn -q -pl canvas-engine -Dtest=CanvasProjectFolderMetadataServiceTest test
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/dto/canvas/ProjectFolderMetadataReq.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/dto/canvas/ProjectFolderMetadataResp.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasProjectFolderMetadataService.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasProjectFolderMetadataServiceTest.java
git commit -m "feat: support project-aware canvas assignment"
```

### Task 4: Permission Service

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/project/CanvasProjectPermissionService.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/project/CanvasProjectPermissionServiceTest.java`

- [ ] **Step 1: Write failing permission tests**

Cover:

```java
@Test
void editorCanEditButExecutorCannotEditProjectCanvas() {
    CanvasProjectPermissionService service = serviceWithMember("alice", "EDITOR");
    assertThatCode(() -> service.requireCanvasAction(canvas(9L), context("alice"), CanvasProjectAction.EDIT))
            .doesNotThrowAnyException();

    CanvasProjectPermissionService executorService = serviceWithMember("alice", "EXECUTOR");
    assertThatThrownBy(() -> executorService.requireCanvasAction(canvas(9L), context("alice"), CanvasProjectAction.EDIT))
            .isInstanceOf(AccessDeniedException.class);
}
```

- [ ] **Step 2: Run test and verify RED**

```bash
cd backend && mvn -q -pl canvas-engine -Dtest=CanvasProjectPermissionServiceTest test
```

Expected: FAIL because service and `CanvasProjectAction` do not exist.

- [ ] **Step 3: Implement permission service**

Create action enum:

```java
public enum CanvasProjectAction {
    READ, EDIT, PUBLISH, EXECUTE, MANAGE_PROJECT, MANAGE_MEMBERS
}
```

Rules:

```java
TENANT_ADMIN/SUPER_ADMIN => allowed
unassigned canvas => allowed for same-tenant authenticated users
VIEWER => READ
EXECUTOR => READ, EXECUTE
EDITOR => READ, EDIT, PUBLISH, EXECUTE
PROJECT_ADMIN => all project actions
disabled project => deny EDIT, PUBLISH, MANAGE_PROJECT, MANAGE_MEMBERS, but allow READ
```

- [ ] **Step 4: Run test and verify GREEN**

```bash
cd backend && mvn -q -pl canvas-engine -Dtest=CanvasProjectPermissionServiceTest test
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/domain/project/CanvasProjectPermissionService.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/project/CanvasProjectPermissionServiceTest.java
git commit -m "feat: add project permission checks"
```

### Task 5: Canvas Service Integration

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/CanvasCreateReq.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/query/CanvasListQuery.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasService.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasTenantIsolationTest.java`

- [ ] **Step 1: Write failing canvas tests**

Add tests:

```java
@Test
void listAddsProjectFilterWhenProjectIdIsPresent() {
    CanvasListQuery query = new CanvasListQuery();
    query.setTenantId(9L);
    query.setProjectId(11L);

    service.list(query);

    verify(canvasMapper).selectPage(any(), any());
    verify(projectFolderMapper).selectList(any());
}
```

Add a create test proving `projectId` is saved through `CanvasProjectFolderMetadataService`.

- [ ] **Step 2: Run test and verify RED**

```bash
cd backend && mvn -q -pl canvas-engine -Dtest=CanvasTenantIsolationTest test
```

Expected: FAIL because query and service do not support `projectId`.

- [ ] **Step 3: Extend request/query**

Add to `CanvasCreateReq`:

```java
private Long projectId;
private String folderKey;
private String folderName;
private Integer maxTotalExecutions;
private Integer perUserDailyLimit;
private Integer perUserTotalLimit;
private Integer cooldownSeconds;
```

Add to `CanvasListQuery`:

```java
private Long projectId;
```

- [ ] **Step 4: Implement create/list integration**

Inject `CanvasProjectFolderMetadataService` into `CanvasService`. After inserting a canvas, call:

```java
if (req.getProjectId() != null || req.getFolderKey() != null || req.getFolderName() != null) {
    projectFolderMetadataService.saveMetadata(canvas.getTenantId(), canvas.getId(),
            new ProjectFolderMetadataReq(req.getProjectId(), null, null,
                    req.getFolderKey(), req.getFolderName(), req.getCreatedBy()));
}
```

For project-filtered list, first read assignment rows for the project and add `in(CanvasDO::getId, canvasIds)`. If the list is empty, return `PageResult.of(0, List.of())`.

- [ ] **Step 5: Run test and verify GREEN**

```bash
cd backend && mvn -q -pl canvas-engine -Dtest=CanvasTenantIsolationTest test
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/dto/CanvasCreateReq.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/query/CanvasListQuery.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasService.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasTenantIsolationTest.java
git commit -m "feat: connect canvases to projects"
```

### Task 6: Backend Controllers

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasProjectController.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasController.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/ExecutionController.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/web/CanvasProjectControllerTest.java`

- [ ] **Step 1: Write failing controller tests**

Test that `CanvasProjectController#create` delegates to service with the current tenant and that `CanvasController#publish` calls project permission before publish.

- [ ] **Step 2: Run test and verify RED**

```bash
cd backend && mvn -q -pl canvas-engine -Dtest=CanvasProjectControllerTest test
```

Expected: FAIL because controller does not exist.

- [ ] **Step 3: Implement `/admin/projects` controller**

Expose:

```java
@GetMapping
public Mono<R<List<ProjectSummaryResp>>> list()
@PostMapping
public Mono<R<ProjectDetailResp>> create(@Valid @RequestBody ProjectCreateReq req)
@GetMapping("/{projectId}")
public Mono<R<ProjectDetailResp>> detail(@PathVariable Long projectId)
@PutMapping("/{projectId}")
public Mono<R<ProjectDetailResp>> update(@PathVariable Long projectId, @Valid @RequestBody ProjectUpdateReq req)
@PutMapping("/{projectId}/disable")
public Mono<R<Void>> disable(@PathVariable Long projectId)
@GetMapping("/{projectId}/members")
public Mono<R<List<ProjectMemberResp>>> members(@PathVariable Long projectId)
@PutMapping("/{projectId}/members/{userId}")
public Mono<R<ProjectMemberResp>> setMember(...)
@DeleteMapping("/{projectId}/members/{userId}")
public Mono<R<Void>> removeMember(...)
@GetMapping("/{projectId}/canvases")
public Mono<R<PageResult<CanvasDO>>> canvases(...)
@GetMapping("/{projectId}/stats")
public Mono<R<ProjectStatsResp>> stats(...)
```

- [ ] **Step 4: Add permission checks to existing canvas endpoints**

In `CanvasController`, after tenant access loads the canvas, call `permissionService.requireCanvasAction(...)` for read/edit/publish/execute-equivalent actions.

In `ExecutionController`, check dry-run only:

```java
CanvasDO canvas = canvasService.requireTenantAccess(canvasId, context.tenantId(), context.isSuperAdmin());
permissionService.requireCanvasAction(canvas, context, CanvasProjectAction.EXECUTE);
```

Keep signed direct and behavior triggers unchanged except tenant/signature validation already present.

- [ ] **Step 5: Run tests and verify GREEN**

```bash
cd backend && mvn -q -pl canvas-engine -Dtest=CanvasProjectControllerTest test
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasProjectController.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasController.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/ExecutionController.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/CanvasProjectControllerTest.java
git commit -m "feat: expose project governance APIs"
```

### Task 7: Frontend Project API and Presentation

**Files:**
- Modify: `frontend/src/services/api.ts`
- Create: `frontend/src/services/projectApi.test.ts`
- Create: `frontend/src/pages/projects/projectPresentation.ts`
- Create: `frontend/src/pages/projects/projectPresentation.test.ts`

- [ ] **Step 1: Write failing frontend tests**

`projectApi.test.ts` should verify:

```ts
await api.list()
await api.create({ projectKey: 'growth', projectName: 'Growth' })
await api.setMember(3, 9, { username: 'alice', role: 'EDITOR' })
expect(http.get).toHaveBeenCalledWith('/admin/projects')
expect(http.post).toHaveBeenCalledWith('/admin/projects', { projectKey: 'growth', projectName: 'Growth' })
expect(http.put).toHaveBeenCalledWith('/admin/projects/3/members/9', { username: 'alice', role: 'EDITOR' })
```

- [ ] **Step 2: Run test and verify RED**

```bash
cd frontend && npm test -- src/services/projectApi.test.ts src/pages/projects/projectPresentation.test.ts
```

Expected: FAIL because API and presentation files do not exist.

- [ ] **Step 3: Add project types and API client**

Add `projectApi` methods in `api.ts`:

```ts
export const projectApi = {
  list: () => http.get<R<ProjectSummary[]>, R<ProjectSummary[]>>('/admin/projects'),
  create: (body: ProjectPayload) => http.post<R<ProjectDetail>, R<ProjectDetail>>('/admin/projects', body),
  update: (id: number, body: ProjectPayload) => http.put<R<ProjectDetail>, R<ProjectDetail>>(`/admin/projects/${id}`, body),
  disable: (id: number) => http.put<R<void>, R<void>>(`/admin/projects/${id}/disable`),
  members: (id: number) => http.get<R<ProjectMember[]>, R<ProjectMember[]>>(`/admin/projects/${id}/members`),
  setMember: (projectId: number, userId: number, body: ProjectMemberPayload) =>
    http.put<R<ProjectMember>, R<ProjectMember>>(`/admin/projects/${projectId}/members/${userId}`, body),
  removeMember: (projectId: number, userId: number) =>
    http.delete<R<void>, R<void>>(`/admin/projects/${projectId}/members/${userId}`),
  canvases: (id: number, params?: CanvasListQuery) =>
    http.get<R<PageResult<Canvas>>, R<PageResult<Canvas>>>(`/admin/projects/${id}/canvases`, { params }),
  stats: (id: number) => http.get<R<ProjectStats>, R<ProjectStats>>(`/admin/projects/${id}/stats`),
}
```

Add `projectPresentation.ts` helpers for role labels, status labels, and stats cards.

- [ ] **Step 4: Run test and verify GREEN**

```bash
cd frontend && npm test -- src/services/projectApi.test.ts src/pages/projects/projectPresentation.test.ts
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/services/api.ts frontend/src/services/projectApi.test.ts \
  frontend/src/pages/projects/projectPresentation.ts frontend/src/pages/projects/projectPresentation.test.ts
git commit -m "feat: add project frontend API"
```

### Task 8: Frontend Project Pages and Navigation

**Files:**
- Create: `frontend/src/pages/projects/index.tsx`
- Modify: `frontend/src/App.tsx`
- Modify: `frontend/src/components/layout/AppLayout.tsx`

- [ ] **Step 1: Implement project list/detail page**

Create a page with:

```text
Project list table
Create/edit modal
Detail drawer or route section with tabs: Canvases, Members, Default Policies, Statistics
```

Use `projectApi` and Ant Design `Table`, `Modal`, `Form`, `Tabs`, and `Descriptions`.

- [ ] **Step 2: Wire route and menu**

Add lazy import:

```ts
const ProjectsPage = lazy(() => import('./pages/projects'))
```

Add route under `RequireAdmin`:

```tsx
<Route path="/admin/projects" element={<ProjectsPage />} />
```

Add menu item under settings:

```tsx
{
  key: 'admin-projects',
  icon: <ApartmentOutlined />,
  label: '项目管理',
  onClick: () => navigate('/admin/projects'),
}
```

- [ ] **Step 3: Run frontend build**

```bash
cd frontend && npm run build
```

Expected: build exits 0.

- [ ] **Step 4: Commit**

```bash
git add frontend/src/pages/projects/index.tsx frontend/src/App.tsx frontend/src/components/layout/AppLayout.tsx
git commit -m "feat: add project management UI"
```

### Task 9: Canvas List Project Integration

**Files:**
- Modify: `frontend/src/services/api.ts`
- Modify: `frontend/src/pages/canvas-list/index.tsx`
- Create: `frontend/src/pages/canvas-list/canvasProjectPayload.test.ts`
- Create: `frontend/src/pages/canvas-list/canvasProjectPayload.ts`

- [ ] **Step 1: Write failing payload tests**

Test:

```ts
expect(buildCanvasCreatePayload({
  name: 'Welcome',
  projectId: 3,
  folderKey: 'new-user',
  folderName: 'New User',
})).toEqual({
  name: 'Welcome',
  projectId: 3,
  folderKey: 'new-user',
  folderName: 'New User',
})
```

- [ ] **Step 2: Run test and verify RED**

```bash
cd frontend && npm test -- src/pages/canvas-list/canvasProjectPayload.test.ts
```

Expected: FAIL because helper does not exist.

- [ ] **Step 3: Extend canvas request types and list page**

Update `CanvasCreateReq` and `CanvasListQuery` in `api.ts`:

```ts
projectId?: number
folderKey?: string
folderName?: string
```

In `CanvasListPage`:

- load `projectApi.list()`
- add Project filter above table
- pass `{ projectId }` to `canvasApi.list`
- add Project and Folder fields to the create modal
- use `buildCanvasCreatePayload(values)`

- [ ] **Step 4: Run test and verify GREEN**

```bash
cd frontend && npm test -- src/pages/canvas-list/canvasProjectPayload.test.ts
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/services/api.ts frontend/src/pages/canvas-list/index.tsx \
  frontend/src/pages/canvas-list/canvasProjectPayload.ts frontend/src/pages/canvas-list/canvasProjectPayload.test.ts
git commit -m "feat: assign canvases to projects in UI"
```

### Task 10: Verification and Completion

**Files:**
- All files changed by prior tasks.

- [ ] **Step 1: Run backend focused tests**

```bash
cd backend && mvn -q -pl canvas-engine \
  -Dtest=ProjectGovernanceMigrationTest,CanvasProjectServiceTest,CanvasProjectPermissionServiceTest,CanvasProjectFolderMetadataServiceTest,CanvasTenantIsolationTest,CanvasProjectControllerTest \
  test
```

Expected: PASS.

- [ ] **Step 2: Run frontend focused tests**

```bash
cd frontend && npm test -- \
  src/services/projectApi.test.ts \
  src/pages/projects/projectPresentation.test.ts \
  src/pages/canvas-list/canvasProjectPayload.test.ts
```

Expected: PASS.

- [ ] **Step 3: Run compile/build smoke checks**

```bash
cd backend && mvn -q -pl canvas-engine -DskipTests compile
cd frontend && npm run build
```

Expected: both commands exit 0.

- [ ] **Step 4: Audit changed files**

```bash
git status --short
git diff --name-only HEAD~10..HEAD
```

Expected: project-governance implementation files are present; unrelated existing worktree changes are not committed by these tasks.

- [ ] **Step 5: Commit any remaining project-governance changes**

```bash
git add backend/canvas-engine/src/main frontend/src docs/superpowers/plans/2026-06-04-project-governance.md
git commit -m "feat: complete project governance"
```

Only run this commit if `git status --short` shows uncommitted files from this plan.
