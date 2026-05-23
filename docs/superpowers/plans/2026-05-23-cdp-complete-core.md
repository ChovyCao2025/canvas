# Complete CDP Core Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the first shippable CDP loop: user profiles, user tag instances, canvas user data views, manual/batch tagging, and a canvas node that writes CDP tags.

**Architecture:** Add a focused CDP domain beside the existing canvas, audience, and meta domains. Keep `tag_definition` as the tag metadata source, add CDP current-state/history tables for user tags, query canvas user data from existing `canvas_execution` and `canvas_execution_trace`, and expose new frontend pages under `/cdp/users` and `/canvas/:id/users`.

**Tech Stack:** Spring Boot WebFlux, MyBatis-Plus, Flyway, Reactor, JUnit 5, Mockito, AssertJ, React 18, Vite, TypeScript, Ant Design, Vitest.

---

## Scope Check

The approved spec describes a full CDP roadmap. This implementation plan covers the first shippable batch only:

- CDP user profile and identity storage.
- User tag current state and tag history.
- Manual single-user tagging and removal.
- Batch tagging task API.
- Canvas user list/detail APIs.
- `CDP_TAG_WRITE` canvas node.
- Frontend CDP user center, user detail, canvas user data page, and tag definition field extensions.

Event ingestion, advanced ID merge UI, rule-tag computation, CDP user 360 insight charts, field-level privacy permissions, and governance workflows are separate future plans.

## File Structure

### Backend Create

- `backend/canvas-engine/src/main/resources/db/migration/V49__cdp_core.sql`  
  Adds CDP tables, extends `tag_definition`, seeds `CDP_TAG_WRITE` node type.

- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpUserProfile.java`  
  MyBatis entity for `cdp_user_profile`.

- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpUserIdentity.java`  
  MyBatis entity for `cdp_user_identity`.

- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpUserTag.java`  
  MyBatis entity for current user tags.

- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpUserTagHistory.java`  
  MyBatis entity for tag changes.

- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpTagOperation.java`  
  MyBatis entity for batch tag jobs.

- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpUserProfileMapper.java`  
  MyBatis mapper for user profiles.

- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpUserIdentityMapper.java`  
  MyBatis mapper for user identities.

- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpUserTagMapper.java`  
  MyBatis mapper for current user tags.

- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpUserTagHistoryMapper.java`  
  MyBatis mapper for user tag history.

- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpTagOperationMapper.java`  
  MyBatis mapper for batch tag jobs.

- `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/cdp/CdpUserDetailDTO.java`  
  User detail response DTO.

- `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/cdp/CdpTagWriteReq.java`  
  Single user tag write request DTO.

- `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/cdp/CdpUserTagDTO.java`  
  Current user tag response DTO.

- `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/cdp/CdpUserTagHistoryDTO.java`  
  User tag history response DTO.

- `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/cdp/CdpBatchTagReq.java`  
  Batch tag operation request DTO.

- `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/cdp/CanvasUserRowDTO.java`  
  Canvas user list row DTO.

- `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/cdp/CanvasUserDetailDTO.java`  
  Canvas user detail response DTO.

- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpUserService.java`  
  Owns profile/identity creation, lookup, masking, and `ensureUser`.

- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpTagService.java`  
  Owns tag validation, current-state upsert/removal, history writes, and idempotency.

- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpTagOperationService.java`  
  Owns asynchronous batch set/remove jobs.

- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CanvasUserQueryService.java`  
  Aggregates canvas users from `canvas_execution` and joins CDP current tags.

- `backend/canvas-engine/src/main/java/org/chovy/canvas/controller/CdpUserController.java`  
  `/cdp/users` API.

- `backend/canvas-engine/src/main/java/org/chovy/canvas/controller/CdpTagOperationController.java`  
  `/cdp/tag-operations` API.

- `backend/canvas-engine/src/main/java/org/chovy/canvas/controller/CanvasUserController.java`  
  `/canvas/{id}/users` API.

- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/CdpTagWriteHandler.java`  
  `CDP_TAG_WRITE` node handler.

### Backend Modify

- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/TagDefinition.java`  
  Add CDP metadata fields.

- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/CanvasExecutionService.java`  
  Call `CdpUserService.ensureUser` when a non-blank `userId` enters execution.

- `backend/canvas-engine/src/main/java/org/chovy/canvas/controller/TagDefinitionController.java`  
  Continue CRUD with new entity fields and expose existing list shape.

### Frontend Create

- `frontend/src/services/cdpApi.ts`  
  CDP API client and types.

- `frontend/src/pages/cdp-users/cdpPresentation.ts`  
  Pure helpers for masking labels, tag badges, status labels, and request payload normalization.

- `frontend/src/pages/cdp-users/cdpPresentation.test.ts`  
  Vitest coverage for the pure helpers.

- `frontend/src/pages/cdp-users/index.tsx`  
  CDP user list page.

- `frontend/src/pages/cdp-user-detail/index.tsx`  
  CDP user detail page with current tags, history, journey records, and tag modal.

- `frontend/src/pages/canvas-users/index.tsx`  
  Canvas user data page.

### Frontend Modify

- `frontend/src/App.tsx`  
  Add routes for `/cdp/users`, `/cdp/users/:userId`, and `/canvas/:id/users`.

- `frontend/src/components/layout/AppLayout.tsx`  
  Add CDP user center menu item.

- `frontend/src/pages/canvas-list/index.tsx`  
  Add "用户数据" action.

- `frontend/src/pages/canvas-editor/index.tsx`  
  Add toolbar entry to `/canvas/:id/users`.

- `frontend/src/pages/canvas-stats/index.tsx`  
  Add "用户明细" entry.

- `frontend/src/pages/tag-config/index.tsx`  
  Add CDP metadata fields and columns.

---

### Task 1: Database Migration and Tag Definition Model

**Files:**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V49__cdp_core.sql`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/TagDefinition.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/meta/TagDefinitionCdpFieldsTest.java`

- [ ] **Step 1: Write the failing entity mapping test**

Create `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/meta/TagDefinitionCdpFieldsTest.java`:

```java
package org.chovy.canvas.domain.meta;

import com.baomidou.mybatisplus.annotation.TableName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TagDefinitionCdpFieldsTest {

    @Test
    void exposes_cdp_metadata_fields_without_changing_table_name() {
        TableName tableName = TagDefinition.class.getAnnotation(TableName.class);
        assertThat(tableName.value()).isEqualTo("tag_definition");

        TagDefinition tag = new TagDefinition();
        tag.setValueType("BOOLEAN");
        tag.setManualEnabled(1);
        tag.setDefaultTtlDays(30);
        tag.setCategory("生命周期");
        tag.setOwner("growth");
        tag.setWritePolicy("UPSERT");

        assertThat(tag.getValueType()).isEqualTo("BOOLEAN");
        assertThat(tag.getManualEnabled()).isEqualTo(1);
        assertThat(tag.getDefaultTtlDays()).isEqualTo(30);
        assertThat(tag.getCategory()).isEqualTo("生命周期");
        assertThat(tag.getOwner()).isEqualTo("growth");
        assertThat(tag.getWritePolicy()).isEqualTo("UPSERT");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
cd backend/canvas-engine
mvn -Dtest=TagDefinitionCdpFieldsTest test
```

Expected: compilation fails because `TagDefinition` does not yet have `valueType`, `manualEnabled`, `defaultTtlDays`, `category`, `owner`, or `writePolicy`.

- [ ] **Step 3: Add CDP fields to `TagDefinition`**

Modify `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/TagDefinition.java` by adding these fields after `enabled`:

```java
    /** 标签值类型：STRING / NUMBER / BOOLEAN / JSON */
    private String valueType;

    /** 是否允许人工打标，1=允许，0=不允许 */
    private Integer manualEnabled;

    /** 默认有效期天数，null=长期有效 */
    private Integer defaultTtlDays;

    /** 标签分类 */
    private String category;

    /** 负责人 */
    private String owner;

    /** 第一批仅启用 UPSERT，APPEND 为后续多值标签预留 */
    private String writePolicy;
```

- [ ] **Step 4: Add Flyway migration**

Create `backend/canvas-engine/src/main/resources/db/migration/V49__cdp_core.sql`:

```sql
-- V49: CDP core user profile, tag instances, tag history, batch tagging, and tag write node

ALTER TABLE tag_definition
    ADD COLUMN value_type VARCHAR(20) NOT NULL DEFAULT 'STRING' COMMENT 'STRING/NUMBER/BOOLEAN/JSON',
    ADD COLUMN manual_enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否允许人工打标',
    ADD COLUMN default_ttl_days INT NULL COMMENT '默认有效期天数，null=长期有效',
    ADD COLUMN category VARCHAR(64) NULL COMMENT '标签分类',
    ADD COLUMN owner VARCHAR(64) NULL COMMENT '负责人',
    ADD COLUMN write_policy VARCHAR(20) NOT NULL DEFAULT 'UPSERT' COMMENT '第一批仅启用UPSERT，APPEND为后续多值标签预留';

CREATE TABLE cdp_user_profile (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id          VARCHAR(128) NOT NULL COMMENT '系统内统一用户ID',
    display_name     VARCHAR(128) NULL,
    phone            VARCHAR(128) NULL,
    email            VARCHAR(256) NULL,
    status           VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    properties_json  JSON NULL COMMENT '轻量扩展属性',
    first_seen_at    DATETIME NULL,
    last_seen_at     DATETIME NULL,
    created_by       VARCHAR(64) NULL,
    created_at       DATETIME NULL,
    updated_at       DATETIME NULL,
    UNIQUE KEY uk_cdp_user_id (user_id),
    INDEX idx_last_seen (last_seen_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CDP用户档案';

CREATE TABLE cdp_user_identity (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id          VARCHAR(128) NOT NULL,
    identity_type    VARCHAR(32) NOT NULL COMMENT 'USER_ID/PHONE/EMAIL/DEVICE_ID/OPEN_ID',
    identity_value   VARCHAR(256) NOT NULL,
    source_type      VARCHAR(32) NULL,
    source_ref_id    VARCHAR(128) NULL,
    verified         TINYINT NOT NULL DEFAULT 0,
    created_at       DATETIME NULL,
    updated_at       DATETIME NULL,
    UNIQUE KEY uk_identity (identity_type, identity_value),
    INDEX idx_user_identity (user_id, identity_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CDP用户身份映射';

CREATE TABLE cdp_user_tag (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id          VARCHAR(128) NOT NULL,
    tag_code         VARCHAR(64) NOT NULL,
    tag_value        VARCHAR(1000) NULL,
    value_type       VARCHAR(20) NOT NULL DEFAULT 'STRING',
    source_type      VARCHAR(32) NOT NULL COMMENT 'MANUAL/CANVAS/BATCH/RULE/API/IMPORT',
    source_ref_id    VARCHAR(128) NULL COMMENT 'executionId/nodeId/jobId等来源引用',
    status           VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    effective_at     DATETIME NULL,
    expires_at       DATETIME NULL,
    created_by       VARCHAR(64) NULL,
    created_at       DATETIME NULL,
    updated_at       DATETIME NULL,
    UNIQUE KEY uk_user_tag (user_id, tag_code),
    INDEX idx_tag_user (tag_code, user_id),
    INDEX idx_user_status (user_id, status),
    INDEX idx_expires (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CDP用户当前标签';

CREATE TABLE cdp_user_tag_history (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id          VARCHAR(128) NOT NULL,
    tag_code         VARCHAR(64) NOT NULL,
    old_value        VARCHAR(1000) NULL,
    new_value        VARCHAR(1000) NULL,
    operation        VARCHAR(20) NOT NULL COMMENT 'SET/REMOVE/EXPIRE',
    source_type      VARCHAR(32) NOT NULL,
    source_ref_id    VARCHAR(128) NULL,
    idempotency_key  VARCHAR(256) NULL COMMENT '幂等键，画布来源使用executionId:nodeId:userId:tagCode',
    reason           VARCHAR(500) NULL,
    operator         VARCHAR(64) NULL,
    operated_at      DATETIME NULL,
    UNIQUE KEY uk_tag_history_idempotency (idempotency_key),
    INDEX idx_user_tag_history (user_id, tag_code, operated_at),
    INDEX idx_source_history (source_type, source_ref_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CDP用户标签历史';

CREATE TABLE cdp_tag_operation (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    operation_type  VARCHAR(20) NOT NULL COMMENT 'BATCH_SET/BATCH_REMOVE',
    tag_code        VARCHAR(64) NOT NULL,
    tag_value       VARCHAR(1000) NULL,
    total_count     INT NOT NULL DEFAULT 0,
    success_count   INT NOT NULL DEFAULT 0,
    fail_count      INT NOT NULL DEFAULT 0,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    error_msg       VARCHAR(1000) NULL,
    created_by      VARCHAR(64) NULL,
    created_at      DATETIME NULL,
    updated_at      DATETIME NULL,
    INDEX idx_tag_operation_status (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CDP批量标签任务';

INSERT INTO node_type_registry
(type_key, type_name, category, handler_class, config_schema, output_schema, is_trigger, is_terminal, description, enabled)
VALUES
(
  'CDP_TAG_WRITE',
  '写用户标签',
  '行为策略',
  'org.chovy.canvas.engine.handlers.CdpTagWriteHandler',
  '[{"key":"tagCode","label":"标签","type":"select","dataSource":"/meta/tagger-tags","required":true},{"key":"valueMode","label":"标签值来源","type":"radio","required":true,"options":[{"label":"固定值","value":"fixed"},{"label":"上下文字段","value":"context"}]},{"key":"tagValue","label":"标签值","type":"text","showWhen":"valueMode==fixed"},{"key":"tagValueField","label":"上下文字段","type":"select","dataSource":"/meta/context-fields","showWhen":"valueMode==context"},{"key":"reason","label":"原因","type":"text"},{"key":"nextNodeId","label":"下一节点","type":"node-select"}]',
  '[{"fieldKey":"tagCode","fieldName":"标签编码","dataType":"STRING"},{"fieldKey":"tagValue","fieldName":"标签值","dataType":"STRING"},{"fieldKey":"tagWriteStatus","fieldName":"标签写入状态","dataType":"STRING"}]',
  0,
  0,
  '将当前执行用户写入CDP标签实例表',
  1
)
ON DUPLICATE KEY UPDATE
  type_name = VALUES(type_name),
  category = VALUES(category),
  handler_class = VALUES(handler_class),
  config_schema = VALUES(config_schema),
  output_schema = VALUES(output_schema),
  description = VALUES(description),
  enabled = VALUES(enabled);
```

- [ ] **Step 5: Run test to verify it passes**

Run:

```bash
cd backend/canvas-engine
mvn -Dtest=TagDefinitionCdpFieldsTest test
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add backend/canvas-engine/src/main/resources/db/migration/V49__cdp_core.sql \
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/TagDefinition.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/meta/TagDefinitionCdpFieldsTest.java
git commit -m "feat: add cdp schema foundation"
```

### Task 2: CDP Domain Entities and Mappers

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpUserProfile.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpUserIdentity.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpUserTag.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpUserTagHistory.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpTagOperation.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpUserProfileMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpUserIdentityMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpUserTagMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpUserTagHistoryMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpTagOperationMapper.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/CdpEntityMappingTest.java`

- [ ] **Step 1: Write the failing entity mapping test**

Create `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/CdpEntityMappingTest.java`:

```java
package org.chovy.canvas.domain.cdp;

import com.baomidou.mybatisplus.annotation.TableName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CdpEntityMappingTest {

    @Test
    void maps_entities_to_cdp_tables() {
        assertThat(CdpUserProfile.class.getAnnotation(TableName.class).value()).isEqualTo("cdp_user_profile");
        assertThat(CdpUserIdentity.class.getAnnotation(TableName.class).value()).isEqualTo("cdp_user_identity");
        assertThat(CdpUserTag.class.getAnnotation(TableName.class).value()).isEqualTo("cdp_user_tag");
        assertThat(CdpUserTagHistory.class.getAnnotation(TableName.class).value()).isEqualTo("cdp_user_tag_history");
        assertThat(CdpTagOperation.class.getAnnotation(TableName.class).value()).isEqualTo("cdp_tag_operation");
    }

    @Test
    void exposes_core_fields() {
        CdpUserTag tag = new CdpUserTag();
        tag.setUserId("u1");
        tag.setTagCode("vip");
        tag.setTagValue("true");
        tag.setStatus("ACTIVE");

        assertThat(tag.getUserId()).isEqualTo("u1");
        assertThat(tag.getTagCode()).isEqualTo("vip");
        assertThat(tag.getTagValue()).isEqualTo("true");
        assertThat(tag.getStatus()).isEqualTo("ACTIVE");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd backend/canvas-engine
mvn -Dtest=CdpEntityMappingTest test
```

Expected: compilation fails because the CDP entity classes do not exist.

- [ ] **Step 3: Add CDP entities**

Create the entity classes with the same Lombok/MyBatis style used by existing domain classes.

`backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpUserProfile.java`:

```java
package org.chovy.canvas.domain.cdp;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("cdp_user_profile")
public class CdpUserProfile {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String userId;
    private String displayName;
    private String phone;
    private String email;
    private String status;
    private String propertiesJson;
    private LocalDateTime firstSeenAt;
    private LocalDateTime lastSeenAt;
    private String createdBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
```

`backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpUserIdentity.java`:

```java
package org.chovy.canvas.domain.cdp;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("cdp_user_identity")
public class CdpUserIdentity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String userId;
    private String identityType;
    private String identityValue;
    private String sourceType;
    private String sourceRefId;
    private Integer verified;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
```

`backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpUserTag.java`:

```java
package org.chovy.canvas.domain.cdp;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("cdp_user_tag")
public class CdpUserTag {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String userId;
    private String tagCode;
    private String tagValue;
    private String valueType;
    private String sourceType;
    private String sourceRefId;
    private String status;
    private LocalDateTime effectiveAt;
    private LocalDateTime expiresAt;
    private String createdBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
```

`backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpUserTagHistory.java`:

```java
package org.chovy.canvas.domain.cdp;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("cdp_user_tag_history")
public class CdpUserTagHistory {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String userId;
    private String tagCode;
    private String oldValue;
    private String newValue;
    private String operation;
    private String sourceType;
    private String sourceRefId;
    private String idempotencyKey;
    private String reason;
    private String operator;
    private LocalDateTime operatedAt;
}
```

`backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpTagOperation.java`:

```java
package org.chovy.canvas.domain.cdp;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("cdp_tag_operation")
public class CdpTagOperation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String operationType;
    private String tagCode;
    private String tagValue;
    private Integer totalCount;
    private Integer successCount;
    private Integer failCount;
    private String status;
    private String errorMsg;
    private String createdBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 4: Add mappers**

Create one mapper per entity.

`backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpUserProfileMapper.java`:

```java
package org.chovy.canvas.domain.cdp;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CdpUserProfileMapper extends BaseMapper<CdpUserProfile> {}
```

`backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpUserIdentityMapper.java`:

```java
package org.chovy.canvas.domain.cdp;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CdpUserIdentityMapper extends BaseMapper<CdpUserIdentity> {}
```

`backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpUserTagMapper.java`:

```java
package org.chovy.canvas.domain.cdp;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CdpUserTagMapper extends BaseMapper<CdpUserTag> {}
```

`backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpUserTagHistoryMapper.java`:

```java
package org.chovy.canvas.domain.cdp;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CdpUserTagHistoryMapper extends BaseMapper<CdpUserTagHistory> {}
```

`backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpTagOperationMapper.java`:

```java
package org.chovy.canvas.domain.cdp;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CdpTagOperationMapper extends BaseMapper<CdpTagOperation> {}
```

- [ ] **Step 5: Run test to verify it passes**

```bash
cd backend/canvas-engine
mvn -Dtest=CdpEntityMappingTest test
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/CdpEntityMappingTest.java
git commit -m "feat: add cdp domain entities"
```

### Task 3: CDP User Service

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/cdp/CdpUserDetailDTO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpUserService.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/CdpUserServiceTest.java`

- [ ] **Step 1: Write failing service tests**

Create `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/CdpUserServiceTest.java`:

```java
package org.chovy.canvas.domain.cdp;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CdpUserServiceTest {

    private CdpUserProfileMapper profileMapper;
    private CdpUserIdentityMapper identityMapper;
    private CdpUserService service;

    @BeforeEach
    void setUp() {
        profileMapper = Mockito.mock(CdpUserProfileMapper.class);
        identityMapper = Mockito.mock(CdpUserIdentityMapper.class);
        service = new CdpUserService(profileMapper, identityMapper);
    }

    @Test
    void ensureUser_creates_profile_and_user_id_identity_when_missing() {
        when(profileMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        service.ensureUser("u1", "CANVAS_EXECUTION", "exec-1");

        ArgumentCaptor<CdpUserProfile> profileCaptor = ArgumentCaptor.forClass(CdpUserProfile.class);
        ArgumentCaptor<CdpUserIdentity> identityCaptor = ArgumentCaptor.forClass(CdpUserIdentity.class);
        verify(profileMapper).insert(profileCaptor.capture());
        verify(identityMapper).insert(identityCaptor.capture());

        assertThat(profileCaptor.getValue().getUserId()).isEqualTo("u1");
        assertThat(profileCaptor.getValue().getDisplayName()).isEqualTo("u1");
        assertThat(profileCaptor.getValue().getStatus()).isEqualTo("ACTIVE");
        assertThat(identityCaptor.getValue().getIdentityType()).isEqualTo("USER_ID");
        assertThat(identityCaptor.getValue().getIdentityValue()).isEqualTo("u1");
        assertThat(identityCaptor.getValue().getSourceType()).isEqualTo("CANVAS_EXECUTION");
        assertThat(identityCaptor.getValue().getSourceRefId()).isEqualTo("exec-1");
    }

    @Test
    void ensureUser_updates_last_seen_when_profile_exists() {
        CdpUserProfile existing = new CdpUserProfile();
        existing.setId(9L);
        existing.setUserId("u1");
        when(profileMapper.selectOne(any(Wrapper.class))).thenReturn(existing);

        service.ensureUser("u1", "MANUAL", "req-1");

        verify(profileMapper).updateById(existing);
        verify(identityMapper, never()).insert(any(CdpUserIdentity.class));
    }

    @Test
    void toDetail_masks_phone_and_email() {
        CdpUserProfile profile = new CdpUserProfile();
        profile.setUserId("u1");
        profile.setDisplayName("Alice");
        profile.setPhone("13812345678");
        profile.setEmail("alice@example.com");
        profile.setStatus("ACTIVE");

        var detail = service.toDetail(profile);

        assertThat(detail.phone()).isEqualTo("138****5678");
        assertThat(detail.email()).isEqualTo("a***e@example.com");
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
cd backend/canvas-engine
mvn -Dtest=CdpUserServiceTest test
```

Expected: compilation fails because `CdpUserService` and `CdpUserDetailDTO` do not exist.

- [ ] **Step 3: Add user detail DTO**

Create `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/cdp/CdpUserDetailDTO.java`:

```java
package org.chovy.canvas.dto.cdp;

import java.time.LocalDateTime;

public record CdpUserDetailDTO(
        String userId,
        String displayName,
        String phone,
        String email,
        String status,
        String propertiesJson,
        LocalDateTime firstSeenAt,
        LocalDateTime lastSeenAt
) {}
```

- [ ] **Step 4: Add `CdpUserService`**

Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpUserService.java`:

```java
package org.chovy.canvas.domain.cdp;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.chovy.canvas.common.DataMaskingUtil;
import org.chovy.canvas.dto.cdp.CdpUserDetailDTO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CdpUserService {

    private final CdpUserProfileMapper profileMapper;
    private final CdpUserIdentityMapper identityMapper;

    public CdpUserProfile ensureUser(String userId, String sourceType, String sourceRefId) {
        String normalized = requireUserId(userId);
        CdpUserProfile existing = profileMapper.selectOne(
                new LambdaQueryWrapper<CdpUserProfile>().eq(CdpUserProfile::getUserId, normalized));
        LocalDateTime now = LocalDateTime.now();
        if (existing != null) {
            if (existing.getFirstSeenAt() == null) existing.setFirstSeenAt(now);
            existing.setLastSeenAt(now);
            profileMapper.updateById(existing);
            return existing;
        }

        CdpUserProfile created = new CdpUserProfile();
        created.setUserId(normalized);
        created.setDisplayName(normalized);
        created.setStatus("ACTIVE");
        created.setFirstSeenAt(now);
        created.setLastSeenAt(now);
        profileMapper.insert(created);

        CdpUserIdentity identity = new CdpUserIdentity();
        identity.setUserId(normalized);
        identity.setIdentityType("USER_ID");
        identity.setIdentityValue(normalized);
        identity.setSourceType(sourceType);
        identity.setSourceRefId(sourceRefId);
        identity.setVerified(1);
        identityMapper.insert(identity);

        return created;
    }

    public CdpUserProfile getRequiredProfile(String userId) {
        CdpUserProfile profile = profileMapper.selectOne(
                new LambdaQueryWrapper<CdpUserProfile>().eq(CdpUserProfile::getUserId, requireUserId(userId)));
        if (profile == null) throw new IllegalArgumentException("CDP用户不存在: " + userId);
        return profile;
    }

    public CdpUserDetailDTO toDetail(CdpUserProfile profile) {
        return new CdpUserDetailDTO(
                profile.getUserId(),
                profile.getDisplayName(),
                DataMaskingUtil.maskPhone(profile.getPhone()),
                maskEmail(profile.getEmail()),
                profile.getStatus(),
                profile.getPropertiesJson(),
                profile.getFirstSeenAt(),
                profile.getLastSeenAt()
        );
    }

    private String requireUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId不能为空");
        }
        return userId.trim();
    }

    private String maskEmail(String email) {
        if (email == null || email.isBlank()) return email;
        int at = email.indexOf('@');
        if (at <= 1) return "***" + (at >= 0 ? email.substring(at) : "");
        String name = email.substring(0, at);
        return name.charAt(0) + "***" + name.charAt(name.length() - 1) + email.substring(at);
    }
}
```

- [ ] **Step 5: Run tests to verify they pass**

```bash
cd backend/canvas-engine
mvn -Dtest=CdpUserServiceTest test
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpUserService.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/dto/cdp/CdpUserDetailDTO.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/CdpUserServiceTest.java
git commit -m "feat: add cdp user service"
```

### Task 4: CDP Tag Service

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/cdp/CdpTagWriteReq.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/cdp/CdpUserTagDTO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/cdp/CdpUserTagHistoryDTO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpTagService.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/CdpTagServiceTest.java`

- [ ] **Step 1: Write failing tag service tests**

Create `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/CdpTagServiceTest.java`:

```java
package org.chovy.canvas.domain.cdp;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.chovy.canvas.domain.meta.TagDefinition;
import org.chovy.canvas.domain.meta.TagDefinitionMapper;
import org.chovy.canvas.dto.cdp.CdpTagWriteReq;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CdpTagServiceTest {

    private TagDefinitionMapper tagDefinitionMapper;
    private CdpUserTagMapper userTagMapper;
    private CdpUserTagHistoryMapper historyMapper;
    private CdpUserService userService;
    private CdpTagService service;

    @BeforeEach
    void setUp() {
        tagDefinitionMapper = Mockito.mock(TagDefinitionMapper.class);
        userTagMapper = Mockito.mock(CdpUserTagMapper.class);
        historyMapper = Mockito.mock(CdpUserTagHistoryMapper.class);
        userService = Mockito.mock(CdpUserService.class);
        service = new CdpTagService(tagDefinitionMapper, userTagMapper, historyMapper, userService);
    }

    @Test
    void setTag_upserts_current_tag_and_writes_history() {
        when(tagDefinitionMapper.selectOne(any(Wrapper.class))).thenReturn(tag("vip", "BOOLEAN", 1, 1));
        when(userTagMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        CdpTagWriteReq req = new CdpTagWriteReq("vip", "true", "manual mark",
                null, "MANUAL", "req-1", "admin", null);
        service.setTag("u1", req);

        ArgumentCaptor<CdpUserTag> tagCaptor = ArgumentCaptor.forClass(CdpUserTag.class);
        ArgumentCaptor<CdpUserTagHistory> historyCaptor = ArgumentCaptor.forClass(CdpUserTagHistory.class);
        verify(userTagMapper).insert(tagCaptor.capture());
        verify(historyMapper).insert(historyCaptor.capture());

        assertThat(tagCaptor.getValue().getUserId()).isEqualTo("u1");
        assertThat(tagCaptor.getValue().getTagCode()).isEqualTo("vip");
        assertThat(tagCaptor.getValue().getTagValue()).isEqualTo("true");
        assertThat(tagCaptor.getValue().getStatus()).isEqualTo("ACTIVE");
        assertThat(historyCaptor.getValue().getOperation()).isEqualTo("SET");
        assertThat(historyCaptor.getValue().getNewValue()).isEqualTo("true");
    }

    @Test
    void setTag_rejects_boolean_value_mismatch() {
        when(tagDefinitionMapper.selectOne(any(Wrapper.class))).thenReturn(tag("vip", "BOOLEAN", 1, 1));
        CdpTagWriteReq req = new CdpTagWriteReq("vip", "yes", "bad value",
                null, "MANUAL", "req-1", "admin", null);

        assertThatThrownBy(() -> service.setTag("u1", req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("BOOLEAN");
    }

    @Test
    void removeTag_marks_current_tag_removed_and_writes_history() {
        CdpUserTag existing = new CdpUserTag();
        existing.setUserId("u1");
        existing.setTagCode("vip");
        existing.setTagValue("true");
        existing.setStatus("ACTIVE");
        when(userTagMapper.selectOne(any(Wrapper.class))).thenReturn(existing);

        service.removeTag("u1", "vip", "cleanup", "admin");

        assertThat(existing.getStatus()).isEqualTo("REMOVED");
        verify(userTagMapper).updateById(existing);
        verify(historyMapper).insert(any(CdpUserTagHistory.class));
    }

    private TagDefinition tag(String code, String valueType, int enabled, int manualEnabled) {
        TagDefinition def = new TagDefinition();
        def.setTagCode(code);
        def.setName(code);
        def.setEnabled(enabled);
        def.setValueType(valueType);
        def.setManualEnabled(manualEnabled);
        def.setDefaultTtlDays(null);
        return def;
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
cd backend/canvas-engine
mvn -Dtest=CdpTagServiceTest test
```

Expected: compilation fails because `CdpTagService` and CDP tag DTOs do not exist.

- [ ] **Step 3: Add DTOs**

Create `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/cdp/CdpTagWriteReq.java`:

```java
package org.chovy.canvas.dto.cdp;

import java.time.LocalDateTime;

public record CdpTagWriteReq(
        String tagCode,
        String tagValue,
        String reason,
        LocalDateTime expiresAt,
        String sourceType,
        String sourceRefId,
        String operator,
        String idempotencyKey
) {}
```

Create `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/cdp/CdpUserTagDTO.java`:

```java
package org.chovy.canvas.dto.cdp;

import java.time.LocalDateTime;

public record CdpUserTagDTO(
        String tagCode,
        String tagName,
        String tagValue,
        String valueType,
        String sourceType,
        String status,
        LocalDateTime effectiveAt,
        LocalDateTime expiresAt,
        LocalDateTime updatedAt
) {}
```

Create `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/cdp/CdpUserTagHistoryDTO.java`:

```java
package org.chovy.canvas.dto.cdp;

import java.time.LocalDateTime;

public record CdpUserTagHistoryDTO(
        String tagCode,
        String oldValue,
        String newValue,
        String operation,
        String sourceType,
        String sourceRefId,
        String reason,
        String operator,
        LocalDateTime operatedAt
) {}
```

- [ ] **Step 4: Add `CdpTagService`**

Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpTagService.java`:

```java
package org.chovy.canvas.domain.cdp;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.chovy.canvas.domain.meta.TagDefinition;
import org.chovy.canvas.domain.meta.TagDefinitionMapper;
import org.chovy.canvas.dto.cdp.CdpTagWriteReq;
import org.chovy.canvas.dto.cdp.CdpUserTagDTO;
import org.chovy.canvas.dto.cdp.CdpUserTagHistoryDTO;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CdpTagService {

    private final TagDefinitionMapper tagDefinitionMapper;
    private final CdpUserTagMapper userTagMapper;
    private final CdpUserTagHistoryMapper historyMapper;
    private final CdpUserService userService;

    public CdpUserTag setTag(String userId, CdpTagWriteReq req) {
        String normalizedUserId = requireText(userId, "userId");
        String tagCode = requireText(req.tagCode(), "tagCode");
        TagDefinition def = getEnabledTag(tagCode);
        String sourceType = req.sourceType() == null || req.sourceType().isBlank() ? "MANUAL" : req.sourceType();
        if ("MANUAL".equals(sourceType) && Integer.valueOf(0).equals(def.getManualEnabled())) {
            throw new IllegalArgumentException("标签不允许人工打标: " + tagCode);
        }
        String value = normalizeValue(def.getValueType(), req.tagValue());
        userService.ensureUser(normalizedUserId, sourceType, req.sourceRefId());

        CdpUserTag existing = userTagMapper.selectOne(new LambdaQueryWrapper<CdpUserTag>()
                .eq(CdpUserTag::getUserId, normalizedUserId)
                .eq(CdpUserTag::getTagCode, tagCode));
        String oldValue = existing != null ? existing.getTagValue() : null;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = req.expiresAt();
        if (expiresAt == null && def.getDefaultTtlDays() != null) {
            expiresAt = now.plusDays(def.getDefaultTtlDays());
        }

        CdpUserTag tag = existing != null ? existing : new CdpUserTag();
        tag.setUserId(normalizedUserId);
        tag.setTagCode(tagCode);
        tag.setTagValue(value);
        tag.setValueType(def.getValueType() == null ? "STRING" : def.getValueType());
        tag.setSourceType(sourceType);
        tag.setSourceRefId(req.sourceRefId());
        tag.setStatus("ACTIVE");
        tag.setEffectiveAt(now);
        tag.setExpiresAt(expiresAt);
        tag.setCreatedBy(req.operator());
        if (existing == null) userTagMapper.insert(tag);
        else userTagMapper.updateById(tag);

        writeHistory(normalizedUserId, tagCode, oldValue, value, "SET", sourceType,
                req.sourceRefId(), req.idempotencyKey(), req.reason(), req.operator());
        return tag;
    }

    public void removeTag(String userId, String tagCode, String reason, String operator) {
        String normalizedUserId = requireText(userId, "userId");
        String normalizedTagCode = requireText(tagCode, "tagCode");
        CdpUserTag existing = userTagMapper.selectOne(new LambdaQueryWrapper<CdpUserTag>()
                .eq(CdpUserTag::getUserId, normalizedUserId)
                .eq(CdpUserTag::getTagCode, normalizedTagCode));
        if (existing == null) return;
        String oldValue = existing.getTagValue();
        existing.setStatus("REMOVED");
        userTagMapper.updateById(existing);
        writeHistory(normalizedUserId, normalizedTagCode, oldValue, null, "REMOVE", "MANUAL",
                null, null, reason, operator);
    }

    public List<CdpUserTagDTO> listCurrentTags(String userId) {
        return userTagMapper.selectList(new LambdaQueryWrapper<CdpUserTag>()
                        .eq(CdpUserTag::getUserId, requireText(userId, "userId"))
                        .eq(CdpUserTag::getStatus, "ACTIVE")
                        .orderByDesc(CdpUserTag::getUpdatedAt))
                .stream()
                .map(tag -> new CdpUserTagDTO(tag.getTagCode(), tag.getTagCode(), tag.getTagValue(),
                        tag.getValueType(), tag.getSourceType(), tag.getStatus(),
                        tag.getEffectiveAt(), tag.getExpiresAt(), tag.getUpdatedAt()))
                .toList();
    }

    public List<CdpUserTagHistoryDTO> listHistory(String userId) {
        return historyMapper.selectList(new LambdaQueryWrapper<CdpUserTagHistory>()
                        .eq(CdpUserTagHistory::getUserId, requireText(userId, "userId"))
                        .orderByDesc(CdpUserTagHistory::getOperatedAt))
                .stream()
                .map(item -> new CdpUserTagHistoryDTO(item.getTagCode(), item.getOldValue(), item.getNewValue(),
                        item.getOperation(), item.getSourceType(), item.getSourceRefId(),
                        item.getReason(), item.getOperator(), item.getOperatedAt()))
                .toList();
    }

    private TagDefinition getEnabledTag(String tagCode) {
        TagDefinition def = tagDefinitionMapper.selectOne(new LambdaQueryWrapper<TagDefinition>()
                .eq(TagDefinition::getTagCode, tagCode)
                .eq(TagDefinition::getEnabled, 1)
                .last("LIMIT 1"));
        if (def == null) throw new IllegalArgumentException("标签不存在或已禁用: " + tagCode);
        return def;
    }

    private void writeHistory(String userId, String tagCode, String oldValue, String newValue,
                              String operation, String sourceType, String sourceRefId,
                              String idempotencyKey, String reason, String operator) {
        CdpUserTagHistory history = new CdpUserTagHistory();
        history.setUserId(userId);
        history.setTagCode(tagCode);
        history.setOldValue(oldValue);
        history.setNewValue(newValue);
        history.setOperation(operation);
        history.setSourceType(sourceType);
        history.setSourceRefId(sourceRefId);
        history.setIdempotencyKey(idempotencyKey);
        history.setReason(reason);
        history.setOperator(operator);
        history.setOperatedAt(LocalDateTime.now());
        try {
            historyMapper.insert(history);
        } catch (DuplicateKeyException duplicate) {
            if (idempotencyKey == null || idempotencyKey.isBlank()) throw duplicate;
        }
    }

    private String normalizeValue(String valueType, String value) {
        String type = valueType == null || valueType.isBlank() ? "STRING" : valueType;
        if (value == null) return null;
        return switch (type) {
            case "BOOLEAN" -> {
                if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
                    throw new IllegalArgumentException("BOOLEAN 标签值只能是 true 或 false");
                }
                yield value.toLowerCase();
            }
            case "NUMBER" -> {
                try {
                    Double.parseDouble(value);
                    yield value;
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("NUMBER 标签值必须是数字");
                }
            }
            case "JSON", "STRING" -> value;
            default -> throw new IllegalArgumentException("不支持的标签值类型: " + type);
        };
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + "不能为空");
        }
        return value.trim();
    }
}
```

- [ ] **Step 5: Run tests to verify they pass**

```bash
cd backend/canvas-engine
mvn -Dtest=CdpTagServiceTest test
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpTagService.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/dto/cdp/CdpTagWriteReq.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/dto/cdp/CdpUserTagDTO.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/dto/cdp/CdpUserTagHistoryDTO.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/CdpTagServiceTest.java
git commit -m "feat: add cdp tag service"
```

### Task 5: CDP User and Batch Tag Controllers

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/cdp/CdpBatchTagReq.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpTagOperationService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/controller/CdpUserController.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/controller/CdpTagOperationController.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/CdpUserControllerTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/CdpTagOperationControllerTest.java`

- [ ] **Step 1: Write failing controller tests**

Create `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/CdpUserControllerTest.java`:

```java
package org.chovy.canvas.controller;

import org.chovy.canvas.domain.cdp.CdpTagService;
import org.chovy.canvas.domain.cdp.CdpUserProfile;
import org.chovy.canvas.domain.cdp.CdpUserService;
import org.chovy.canvas.dto.cdp.CdpTagWriteReq;
import org.chovy.canvas.dto.cdp.CdpUserDetailDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CdpUserControllerTest {

    private CdpUserService userService;
    private CdpTagService tagService;
    private CdpUserController controller;

    @BeforeEach
    void setUp() {
        userService = Mockito.mock(CdpUserService.class);
        tagService = Mockito.mock(CdpTagService.class);
        controller = new CdpUserController(userService, tagService);
    }

    @Test
    void get_returns_user_detail() {
        CdpUserProfile profile = new CdpUserProfile();
        profile.setUserId("u1");
        when(userService.getRequiredProfile("u1")).thenReturn(profile);
        when(userService.toDetail(profile)).thenReturn(new CdpUserDetailDTO("u1", "u1", null, null,
                "ACTIVE", null, null, null));

        assertThat(controller.get("u1").block().getData().userId()).isEqualTo("u1");
    }

    @Test
    void addTag_delegates_to_tag_service() {
        CdpTagWriteReq req = new CdpTagWriteReq("vip", "true", "reason", null,
                null, null, null, null);

        controller.addTag("u1", req).block();

        verify(tagService).setTag("u1", req);
    }

    @Test
    void listTags_returns_current_tags() {
        when(tagService.listCurrentTags("u1")).thenReturn(List.of());

        assertThat(controller.listTags("u1").block().getData()).isEmpty();
    }
}
```

Create `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/CdpTagOperationControllerTest.java`:

```java
package org.chovy.canvas.controller;

import org.chovy.canvas.domain.cdp.CdpTagOperation;
import org.chovy.canvas.domain.cdp.CdpTagOperationService;
import org.chovy.canvas.dto.cdp.CdpBatchTagReq;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class CdpTagOperationControllerTest {

    @Test
    void create_returns_created_operation() {
        CdpTagOperationService service = Mockito.mock(CdpTagOperationService.class);
        CdpTagOperationController controller = new CdpTagOperationController(service);
        CdpTagOperation op = new CdpTagOperation();
        op.setId(7L);
        when(service.create(new CdpBatchTagReq("BATCH_SET", "vip", "true", List.of("u1"), "reason", "admin")))
                .thenReturn(op);

        CdpBatchTagReq req = new CdpBatchTagReq("BATCH_SET", "vip", "true", List.of("u1"), "reason", "admin");
        assertThat(controller.create(req).block().getData().getId()).isEqualTo(7L);
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
cd backend/canvas-engine
mvn -Dtest=CdpUserControllerTest,CdpTagOperationControllerTest test
```

Expected: compilation fails because controllers and batch DTO/service do not exist.

- [ ] **Step 3: Add batch DTO and service**

Create `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/cdp/CdpBatchTagReq.java`:

```java
package org.chovy.canvas.dto.cdp;

import java.util.List;

public record CdpBatchTagReq(
        String operationType,
        String tagCode,
        String tagValue,
        List<String> userIds,
        String reason,
        String operator
) {}
```

Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpTagOperationService.java`:

```java
package org.chovy.canvas.domain.cdp;

import lombok.RequiredArgsConstructor;
import org.chovy.canvas.dto.cdp.CdpBatchTagReq;
import org.chovy.canvas.dto.cdp.CdpTagWriteReq;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CdpTagOperationService {

    private final CdpTagOperationMapper operationMapper;
    private final CdpTagService tagService;

    public CdpTagOperation create(CdpBatchTagReq req) {
        List<String> userIds = req.userIds() == null ? List.of() : req.userIds();
        if (userIds.isEmpty()) throw new IllegalArgumentException("批量打标用户不能为空");
        CdpTagOperation op = new CdpTagOperation();
        op.setOperationType(req.operationType());
        op.setTagCode(req.tagCode());
        op.setTagValue(req.tagValue());
        op.setTotalCount(userIds.size());
        op.setSuccessCount(0);
        op.setFailCount(0);
        op.setStatus("RUNNING");
        op.setCreatedBy(req.operator());
        operationMapper.insert(op);

        Thread.ofVirtual().start(() -> run(op, userIds, req));
        return op;
    }

    public CdpTagOperation get(Long id) {
        CdpTagOperation op = operationMapper.selectById(id);
        if (op == null) throw new IllegalArgumentException("批量标签任务不存在: " + id);
        return op;
    }

    private void run(CdpTagOperation op, List<String> userIds, CdpBatchTagReq req) {
        int success = 0;
        int fail = 0;
        StringBuilder errors = new StringBuilder();
        for (String userId : userIds) {
            try {
                if ("BATCH_REMOVE".equals(req.operationType())) {
                    tagService.removeTag(userId, req.tagCode(), req.reason(), req.operator());
                } else {
                    tagService.setTag(userId, new CdpTagWriteReq(req.tagCode(), req.tagValue(),
                            req.reason(), null, "BATCH", String.valueOf(op.getId()),
                            req.operator(), op.getId() + ":" + userId + ":" + req.tagCode()));
                }
                success++;
            } catch (RuntimeException e) {
                fail++;
                if (errors.length() < 900) errors.append(userId).append(": ").append(e.getMessage()).append("; ");
            }
        }
        op.setSuccessCount(success);
        op.setFailCount(fail);
        op.setStatus(fail == 0 ? "SUCCESS" : "PARTIAL_FAILED");
        op.setErrorMsg(errors.isEmpty() ? null : errors.toString());
        operationMapper.updateById(op);
    }
}
```

- [ ] **Step 4: Add controllers**

Create `backend/canvas-engine/src/main/java/org/chovy/canvas/controller/CdpUserController.java`:

```java
package org.chovy.canvas.controller;

import lombok.RequiredArgsConstructor;
import org.chovy.canvas.common.R;
import org.chovy.canvas.domain.cdp.CdpTagService;
import org.chovy.canvas.domain.cdp.CdpUserService;
import org.chovy.canvas.dto.cdp.CdpTagWriteReq;
import org.chovy.canvas.dto.cdp.CdpUserDetailDTO;
import org.chovy.canvas.dto.cdp.CdpUserTagDTO;
import org.chovy.canvas.dto.cdp.CdpUserTagHistoryDTO;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@RestController
@RequestMapping("/cdp/users")
@RequiredArgsConstructor
public class CdpUserController {

    private final CdpUserService userService;
    private final CdpTagService tagService;

    @GetMapping("/{userId}")
    public Mono<R<CdpUserDetailDTO>> get(@PathVariable String userId) {
        return Mono.fromCallable(() -> R.ok(userService.toDetail(userService.getRequiredProfile(userId))))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/{userId}/tags")
    public Mono<R<List<CdpUserTagDTO>>> listTags(@PathVariable String userId) {
        return Mono.fromCallable(() -> R.ok(tagService.listCurrentTags(userId)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/{userId}/tag-history")
    public Mono<R<List<CdpUserTagHistoryDTO>>> listTagHistory(@PathVariable String userId) {
        return Mono.fromCallable(() -> R.ok(tagService.listHistory(userId)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/{userId}/tags")
    public Mono<R<Void>> addTag(@PathVariable String userId, @RequestBody CdpTagWriteReq req) {
        return Mono.fromCallable(() -> {
            tagService.setTag(userId, req);
            return R.<Void>ok();
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @DeleteMapping("/{userId}/tags/{tagCode}")
    public Mono<R<Void>> removeTag(@PathVariable String userId, @PathVariable String tagCode) {
        return Mono.fromCallable(() -> {
            tagService.removeTag(userId, tagCode, "用户详情移除标签", null);
            return R.<Void>ok();
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
```

Create `backend/canvas-engine/src/main/java/org/chovy/canvas/controller/CdpTagOperationController.java`:

```java
package org.chovy.canvas.controller;

import lombok.RequiredArgsConstructor;
import org.chovy.canvas.common.R;
import org.chovy.canvas.domain.cdp.CdpTagOperation;
import org.chovy.canvas.domain.cdp.CdpTagOperationService;
import org.chovy.canvas.dto.cdp.CdpBatchTagReq;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/cdp/tag-operations")
@RequiredArgsConstructor
public class CdpTagOperationController {

    private final CdpTagOperationService service;

    @PostMapping
    public Mono<R<CdpTagOperation>> create(@RequestBody CdpBatchTagReq req) {
        return Mono.fromCallable(() -> R.ok(service.create(req)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/{id}")
    public Mono<R<CdpTagOperation>> get(@PathVariable Long id) {
        return Mono.fromCallable(() -> R.ok(service.get(id)))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
```

- [ ] **Step 5: Run tests to verify they pass**

```bash
cd backend/canvas-engine
mvn -Dtest=CdpUserControllerTest,CdpTagOperationControllerTest test
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/controller/CdpUserController.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/controller/CdpTagOperationController.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpTagOperationService.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/dto/cdp/CdpBatchTagReq.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/controller/CdpUserControllerTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/controller/CdpTagOperationControllerTest.java
git commit -m "feat: add cdp user tag APIs"
```

### Task 6: Canvas User Query API

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/cdp/CanvasUserRowDTO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/cdp/CanvasUserDetailDTO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CanvasUserQueryService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/controller/CanvasUserController.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/CanvasUserQueryServiceTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/CanvasUserControllerTest.java`

- [ ] **Step 1: Write failing service test**

Create `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/CanvasUserQueryServiceTest.java`:

```java
package org.chovy.canvas.domain.cdp;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.chovy.canvas.domain.execution.CanvasExecution;
import org.chovy.canvas.domain.execution.CanvasExecutionMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class CanvasUserQueryServiceTest {

    @Test
    void listUsers_aggregates_executions_by_user() {
        CanvasExecutionMapper executionMapper = Mockito.mock(CanvasExecutionMapper.class);
        CdpTagService tagService = Mockito.mock(CdpTagService.class);
        CdpUserService userService = Mockito.mock(CdpUserService.class);
        CanvasUserQueryService service = new CanvasUserQueryService(executionMapper, tagService, userService);

        CanvasExecution success = exec("e1", "u1", 2, LocalDateTime.parse("2026-05-23T10:00:00"));
        CanvasExecution failed = exec("e2", "u1", 3, LocalDateTime.parse("2026-05-23T11:00:00"));
        when(executionMapper.selectList(any(Wrapper.class))).thenReturn(List.of(success, failed));
        when(tagService.listCurrentTags("u1")).thenReturn(List.of());

        var rows = service.listUsers(7L);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).userId()).isEqualTo("u1");
        assertThat(rows.get(0).executionCount()).isEqualTo(2);
        assertThat(rows.get(0).successCount()).isEqualTo(1);
        assertThat(rows.get(0).failedCount()).isEqualTo(1);
        assertThat(rows.get(0).latestStatus()).isEqualTo("FAILED");
    }

    private CanvasExecution exec(String id, String userId, int status, LocalDateTime createdAt) {
        CanvasExecution exec = new CanvasExecution();
        exec.setId(id);
        exec.setCanvasId(7L);
        exec.setUserId(userId);
        exec.setStatus(status);
        exec.setCreatedAt(createdAt);
        return exec;
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd backend/canvas-engine
mvn -Dtest=CanvasUserQueryServiceTest test
```

Expected: compilation fails because `CanvasUserQueryService` and DTOs do not exist.

- [ ] **Step 3: Add DTOs**

Create `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/cdp/CanvasUserRowDTO.java`:

```java
package org.chovy.canvas.dto.cdp;

import java.time.LocalDateTime;
import java.util.List;

public record CanvasUserRowDTO(
        String userId,
        String displayName,
        long executionCount,
        long successCount,
        long failedCount,
        String latestStatus,
        LocalDateTime firstEnteredAt,
        LocalDateTime lastEnteredAt,
        List<CdpUserTagDTO> tags
) {}
```

Create `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/cdp/CanvasUserDetailDTO.java`:

```java
package org.chovy.canvas.dto.cdp;

import java.util.List;

public record CanvasUserDetailDTO(
        String userId,
        CdpUserDetailDTO profile,
        List<CdpUserTagDTO> tags,
        List<CanvasUserRowDTO> canvasRows
) {}
```

- [ ] **Step 4: Add `CanvasUserQueryService`**

Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CanvasUserQueryService.java`:

```java
package org.chovy.canvas.domain.cdp;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.chovy.canvas.domain.constant.ExecutionStatus;
import org.chovy.canvas.domain.execution.CanvasExecution;
import org.chovy.canvas.domain.execution.CanvasExecutionMapper;
import org.chovy.canvas.dto.cdp.CanvasUserRowDTO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CanvasUserQueryService {

    private final CanvasExecutionMapper executionMapper;
    private final CdpTagService tagService;
    private final CdpUserService userService;

    public List<CanvasUserRowDTO> listUsers(Long canvasId) {
        List<CanvasExecution> executions = executionMapper.selectList(
                new LambdaQueryWrapper<CanvasExecution>()
                        .eq(CanvasExecution::getCanvasId, canvasId)
                        .isNotNull(CanvasExecution::getUserId)
                        .orderByDesc(CanvasExecution::getCreatedAt));
        Map<String, List<CanvasExecution>> byUser = new LinkedHashMap<>();
        for (CanvasExecution execution : executions) {
            byUser.computeIfAbsent(execution.getUserId(), ignored -> new java.util.ArrayList<>()).add(execution);
        }
        return byUser.entrySet().stream()
                .map(entry -> toRow(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(CanvasUserRowDTO::lastEnteredAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    public CanvasUserRowDTO getUserInCanvas(Long canvasId, String userId) {
        List<CanvasExecution> executions = executionMapper.selectList(
                new LambdaQueryWrapper<CanvasExecution>()
                        .eq(CanvasExecution::getCanvasId, canvasId)
                        .eq(CanvasExecution::getUserId, userId)
                        .orderByDesc(CanvasExecution::getCreatedAt));
        if (executions.isEmpty()) throw new IllegalArgumentException("该用户没有进入过画布: " + userId);
        return toRow(userId, executions);
    }

    public List<CanvasExecution> listExecutions(Long canvasId, String userId) {
        return executionMapper.selectList(new LambdaQueryWrapper<CanvasExecution>()
                .eq(CanvasExecution::getCanvasId, canvasId)
                .eq(CanvasExecution::getUserId, userId)
                .orderByDesc(CanvasExecution::getCreatedAt)
                .last("LIMIT 100"));
    }

    private CanvasUserRowDTO toRow(String userId, List<CanvasExecution> executions) {
        executions.forEach(e -> userService.ensureUser(userId, "CANVAS_EXECUTION", e.getId()));
        LocalDateTime first = executions.stream().map(CanvasExecution::getCreatedAt)
                .min(Comparator.naturalOrder()).orElse(null);
        LocalDateTime last = executions.stream().map(CanvasExecution::getCreatedAt)
                .max(Comparator.naturalOrder()).orElse(null);
        CanvasExecution latest = executions.stream()
                .max(Comparator.comparing(CanvasExecution::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);
        long success = executions.stream().filter(e -> ExecutionStatus.SUCCESS.getCode().equals(e.getStatus())).count();
        long failed = executions.stream().filter(e -> ExecutionStatus.FAILED.getCode().equals(e.getStatus())).count();
        return new CanvasUserRowDTO(
                userId,
                userId,
                executions.size(),
                success,
                failed,
                latest == null ? "-" : statusLabel(latest.getStatus()),
                first,
                last,
                tagService.listCurrentTags(userId)
        );
    }

    private String statusLabel(Integer status) {
        if (ExecutionStatus.SUCCESS.getCode().equals(status)) return "SUCCESS";
        if (ExecutionStatus.FAILED.getCode().equals(status)) return "FAILED";
        if (ExecutionStatus.PAUSED.getCode().equals(status)) return "PAUSED";
        if (ExecutionStatus.RUNNING.getCode().equals(status)) return "RUNNING";
        return String.valueOf(status);
    }
}
```

- [ ] **Step 5: Add controller and controller test**

Create `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/CanvasUserControllerTest.java`:

```java
package org.chovy.canvas.controller;

import org.chovy.canvas.domain.cdp.CanvasUserQueryService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class CanvasUserControllerTest {

    @Test
    void list_returns_canvas_users() {
        CanvasUserQueryService service = Mockito.mock(CanvasUserQueryService.class);
        CanvasUserController controller = new CanvasUserController(service);
        when(service.listUsers(7L)).thenReturn(List.of());

        assertThat(controller.list(7L).block().getData()).isEmpty();
    }
}
```

Create `backend/canvas-engine/src/main/java/org/chovy/canvas/controller/CanvasUserController.java`:

```java
package org.chovy.canvas.controller;

import lombok.RequiredArgsConstructor;
import org.chovy.canvas.common.R;
import org.chovy.canvas.domain.cdp.CanvasUserQueryService;
import org.chovy.canvas.domain.execution.CanvasExecution;
import org.chovy.canvas.dto.cdp.CanvasUserRowDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@RestController
@RequestMapping("/canvas/{id}/users")
@RequiredArgsConstructor
public class CanvasUserController {

    private final CanvasUserQueryService service;

    @GetMapping
    public Mono<R<List<CanvasUserRowDTO>>> list(@PathVariable Long id) {
        return Mono.fromCallable(() -> R.ok(service.listUsers(id)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/{userId}")
    public Mono<R<CanvasUserRowDTO>> get(@PathVariable Long id, @PathVariable String userId) {
        return Mono.fromCallable(() -> R.ok(service.getUserInCanvas(id, userId)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/{userId}/executions")
    public Mono<R<List<CanvasExecution>>> executions(@PathVariable Long id, @PathVariable String userId) {
        return Mono.fromCallable(() -> R.ok(service.listExecutions(id, userId)))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
```

- [ ] **Step 6: Run tests to verify they pass**

```bash
cd backend/canvas-engine
mvn -Dtest=CanvasUserQueryServiceTest,CanvasUserControllerTest test
```

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CanvasUserQueryService.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/controller/CanvasUserController.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/dto/cdp/CanvasUserRowDTO.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/dto/cdp/CanvasUserDetailDTO.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/CanvasUserQueryServiceTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/controller/CanvasUserControllerTest.java
git commit -m "feat: add canvas user data API"
```

### Task 7: Canvas Execution CDP User Upsert

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/CanvasExecutionService.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/trigger/CanvasExecutionServiceCdpTest.java`

- [ ] **Step 1: Write failing constructor/behavior test**

Create `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/trigger/CanvasExecutionServiceCdpTest.java`:

```java
package org.chovy.canvas.engine.trigger;

import org.chovy.canvas.domain.cdp.CdpUserService;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class CanvasExecutionServiceCdpTest {

    @Test
    void service_declares_cdp_user_service_dependency() {
        boolean hasDependency = java.util.Arrays.stream(CanvasExecutionService.class.getDeclaredFields())
                .anyMatch(field -> field.getType().equals(CdpUserService.class));

        assertThat(hasDependency).isTrue();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd backend/canvas-engine
mvn -Dtest=CanvasExecutionServiceCdpTest test
```

Expected: FAIL because `CanvasExecutionService` has no `CdpUserService` field.

- [ ] **Step 3: Inject `CdpUserService` and call it before execution insert**

Modify `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/CanvasExecutionService.java`.

Add import:

```java
import org.chovy.canvas.domain.cdp.CdpUserService;
```

Add constructor-injected field near the other `final` fields:

```java
    private final CdpUserService cdpUserService;
```

In both places where `finalExec` or `exec` is created before `insertExecution(...)`, add:

```java
                    ensureCdpUser(ctx);
```

For the normal trigger path, the block should read:

```java
                    ensureCdpUser(ctx);
                    final CanvasExecution finalExec = createExecution(ctx);
                    Mono<Map<String, Object>> executionMono = dagEngine.execute(graph, triggerNodeId, ctx)
                            .timeout(Duration.ofSeconds(globalTimeoutSec));
```

For the dry-run path, add the same call before `CanvasExecution exec = createExecution(ctx);`.

Add private helper:

```java
    private void ensureCdpUser(ExecutionContext ctx) {
        if (ctx.getUserId() != null && !ctx.getUserId().isBlank()) {
            cdpUserService.ensureUser(ctx.getUserId(), "CANVAS_EXECUTION", ctx.getExecutionId());
        }
    }
```

- [ ] **Step 4: Run test to verify it passes**

```bash
cd backend/canvas-engine
mvn -Dtest=CanvasExecutionServiceCdpTest test
```

Expected: PASS.

- [ ] **Step 5: Run existing trigger tests**

```bash
cd backend/canvas-engine
mvn -Dtest=CanvasExecutionServiceTest,CanvasExecutionServiceTriggerNodeTest test
```

Expected: PASS. If existing `new CanvasExecutionService(...)` calls fail to compile, edit each failing test constructor call and append `Mockito.mock(CdpUserService.class)` in the constructor slot that matches the new `private final CdpUserService cdpUserService` field.

- [ ] **Step 6: Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/CanvasExecutionService.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/trigger/CanvasExecutionServiceCdpTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/trigger/CanvasExecutionServiceTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/trigger/CanvasExecutionServiceTriggerNodeTest.java
git commit -m "feat: upsert cdp users during canvas execution"
```

### Task 8: CDP Tag Write Node Handler

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/CdpTagWriteHandler.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/CdpTagWriteHandlerTest.java`

- [ ] **Step 1: Write failing handler tests**

Create `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/CdpTagWriteHandlerTest.java`:

```java
package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.domain.cdp.CdpTagService;
import org.chovy.canvas.dto.cdp.CdpTagWriteReq;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeResult;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

class CdpTagWriteHandlerTest {

    @Test
    void writes_fixed_tag_value_and_routes_to_next_node() {
        CdpTagService tagService = Mockito.mock(CdpTagService.class);
        CdpTagWriteHandler handler = new CdpTagWriteHandler(tagService);
        ExecutionContext ctx = new ExecutionContext();
        ctx.setExecutionId("exec-1");
        ctx.setUserId("u1");

        NodeResult result = handler.executeAsync(Map.of(
                "tagCode", "vip",
                "valueMode", "fixed",
                "tagValue", "true",
                "reason", "hit branch",
                "nextNodeId", "next"
        ), ctx).block();

        ArgumentCaptor<CdpTagWriteReq> reqCaptor = ArgumentCaptor.forClass(CdpTagWriteReq.class);
        verify(tagService).setTag(Mockito.eq("u1"), reqCaptor.capture());
        assertThat(reqCaptor.getValue().tagCode()).isEqualTo("vip");
        assertThat(reqCaptor.getValue().tagValue()).isEqualTo("true");
        assertThat(reqCaptor.getValue().sourceType()).isEqualTo("CANVAS");
        assertThat(reqCaptor.getValue().idempotencyKey()).contains("exec-1");
        assertThat(result.nextNodeId()).isEqualTo("next");
        assertThat(result.output()).containsEntry("tagWriteStatus", "SUCCESS");
    }

    @Test
    void writes_context_tag_value() {
        CdpTagService tagService = Mockito.mock(CdpTagService.class);
        CdpTagWriteHandler handler = new CdpTagWriteHandler(tagService);
        ExecutionContext ctx = new ExecutionContext();
        ctx.setExecutionId("exec-1");
        ctx.setUserId("u1");
        ctx.getTriggerPayload().put("vipFlag", "true");

        handler.executeAsync(Map.of(
                "tagCode", "vip",
                "valueMode", "context",
                "tagValueField", "vipFlag"
        ), ctx).block();

        ArgumentCaptor<CdpTagWriteReq> reqCaptor = ArgumentCaptor.forClass(CdpTagWriteReq.class);
        verify(tagService).setTag(Mockito.eq("u1"), reqCaptor.capture());
        assertThat(reqCaptor.getValue().tagValue()).isEqualTo("true");
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
cd backend/canvas-engine
mvn -Dtest=CdpTagWriteHandlerTest test
```

Expected: compilation fails because `CdpTagWriteHandler` does not exist.

- [ ] **Step 3: Add handler**

Create `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/CdpTagWriteHandler.java`:

```java
package org.chovy.canvas.engine.handlers;

import lombok.RequiredArgsConstructor;
import org.chovy.canvas.domain.cdp.CdpTagService;
import org.chovy.canvas.dto.cdp.CdpTagWriteReq;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.chovy.canvas.engine.handler.NodeResult;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Map;

@Component
@NodeHandlerType("CDP_TAG_WRITE")
@RequiredArgsConstructor
public class CdpTagWriteHandler implements NodeHandler {

    private final CdpTagService tagService;

    @Override
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        return Mono.fromCallable(() -> {
            String userId = requireText(ctx.getUserId(), "userId");
            String tagCode = requireText(asString(config.get("tagCode")), "tagCode");
            String valueMode = asString(config.getOrDefault("valueMode", "fixed"));
            String tagValue = "context".equals(valueMode)
                    ? asString(ctx.getContextValue(requireText(asString(config.get("tagValueField")), "tagValueField")))
                    : asString(config.get("tagValue"));
            String nodeRef = ctx.getExecutionId() + ":CDP_TAG_WRITE";
            String idempotencyKey = ctx.getExecutionId() + ":" + userId + ":" + tagCode;

            tagService.setTag(userId, new CdpTagWriteReq(
                    tagCode,
                    tagValue,
                    asString(config.get("reason")),
                    null,
                    "CANVAS",
                    nodeRef,
                    "canvas",
                    idempotencyKey
            ));

            return NodeResult.ok(asString(config.get("nextNodeId")), Map.of(
                    "tagCode", tagCode,
                    "tagValue", tagValue == null ? "" : tagValue,
                    "tagWriteStatus", "SUCCESS"
            ));
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("CDP_TAG_WRITE: " + fieldName + "不能为空");
        }
        return value.trim();
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
cd backend/canvas-engine
mvn -Dtest=CdpTagWriteHandlerTest test
```

Expected: PASS.

- [ ] **Step 5: Run handler registry smoke test**

```bash
cd backend/canvas-engine
mvn -Dtest=TaggerHandlerTest,CdpTagWriteHandlerTest test
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/CdpTagWriteHandler.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/CdpTagWriteHandlerTest.java
git commit -m "feat: add cdp tag write node handler"
```

### Task 9: Frontend CDP API and Presentation Helpers

**Files:**
- Create: `frontend/src/services/cdpApi.ts`
- Create: `frontend/src/pages/cdp-users/cdpPresentation.ts`
- Create: `frontend/src/pages/cdp-users/cdpPresentation.test.ts`

- [ ] **Step 1: Write failing frontend helper tests**

Create `frontend/src/pages/cdp-users/cdpPresentation.test.ts`:

```ts
import { describe, expect, it } from 'vitest'
import { buildTagWritePayload, formatExecutionStatus, tagColor } from './cdpPresentation'

describe('cdpPresentation', () => {
  it('builds tag write payload with string value', () => {
    expect(buildTagWritePayload({
      tagCode: 'vip',
      tagValue: true,
      reason: 'manual',
    })).toEqual({
      tagCode: 'vip',
      tagValue: 'true',
      reason: 'manual',
    })
  })

  it('formats execution status labels', () => {
    expect(formatExecutionStatus('SUCCESS')).toEqual({ label: '成功', color: 'success' })
    expect(formatExecutionStatus('FAILED')).toEqual({ label: '失败', color: 'error' })
    expect(formatExecutionStatus('RUNNING')).toEqual({ label: '执行中', color: 'processing' })
  })

  it('returns stable tag colors', () => {
    expect(tagColor('high_value')).toBe(tagColor('high_value'))
  })
})
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd frontend
npm test -- cdpPresentation.test.ts
```

Expected: FAIL because `cdpPresentation.ts` does not exist.

- [ ] **Step 3: Add CDP API client**

Create `frontend/src/services/cdpApi.ts`:

```ts
import type { R } from '../types'
import http from './api'

export interface CdpUserTag {
  tagCode: string
  tagName: string
  tagValue?: string
  valueType?: string
  sourceType?: string
  status?: string
  effectiveAt?: string
  expiresAt?: string
  updatedAt?: string
}

export interface CdpUserDetail {
  userId: string
  displayName?: string
  phone?: string
  email?: string
  status: string
  propertiesJson?: string
  firstSeenAt?: string
  lastSeenAt?: string
}

export interface CdpUserTagHistory {
  tagCode: string
  oldValue?: string
  newValue?: string
  operation: string
  sourceType: string
  sourceRefId?: string
  reason?: string
  operator?: string
  operatedAt?: string
}

export interface CanvasUserRow {
  userId: string
  displayName?: string
  executionCount: number
  successCount: number
  failedCount: number
  latestStatus: string
  firstEnteredAt?: string
  lastEnteredAt?: string
  tags: CdpUserTag[]
}

export interface TagWritePayload {
  tagCode: string
  tagValue?: string
  reason?: string
  expiresAt?: string
}

export const cdpApi = {
  getUser: (userId: string) =>
    http.get<R<CdpUserDetail>, R<CdpUserDetail>>(`/cdp/users/${encodeURIComponent(userId)}`),
  listUserTags: (userId: string) =>
    http.get<R<CdpUserTag[]>, R<CdpUserTag[]>>(`/cdp/users/${encodeURIComponent(userId)}/tags`),
  listUserTagHistory: (userId: string) =>
    http.get<R<CdpUserTagHistory[]>, R<CdpUserTagHistory[]>>(`/cdp/users/${encodeURIComponent(userId)}/tag-history`),
  addUserTag: (userId: string, body: TagWritePayload) =>
    http.post<R<void>, R<void>>(`/cdp/users/${encodeURIComponent(userId)}/tags`, body),
  removeUserTag: (userId: string, tagCode: string) =>
    http.delete<R<void>, R<void>>(`/cdp/users/${encodeURIComponent(userId)}/tags/${encodeURIComponent(tagCode)}`),
  createBatchTag: (body: { operationType: string; tagCode: string; tagValue?: string; userIds: string[]; reason?: string }) =>
    http.post<R<any>, R<any>>('/cdp/tag-operations', body),
  listCanvasUsers: (canvasId: number) =>
    http.get<R<CanvasUserRow[]>, R<CanvasUserRow[]>>(`/canvas/${canvasId}/users`),
  getCanvasUser: (canvasId: number, userId: string) =>
    http.get<R<CanvasUserRow>, R<CanvasUserRow>>(`/canvas/${canvasId}/users/${encodeURIComponent(userId)}`),
  listCanvasUserExecutions: (canvasId: number, userId: string) =>
    http.get<R<any[]>, R<any[]>>(`/canvas/${canvasId}/users/${encodeURIComponent(userId)}/executions`),
}
```

- [ ] **Step 4: Add presentation helpers**

Create `frontend/src/pages/cdp-users/cdpPresentation.ts`:

```ts
export function buildTagWritePayload(values: { tagCode: string; tagValue?: unknown; reason?: string; expiresAt?: string }) {
  return {
    tagCode: values.tagCode,
    tagValue: values.tagValue == null ? undefined : String(values.tagValue),
    reason: values.reason,
    ...(values.expiresAt ? { expiresAt: values.expiresAt } : {}),
  }
}

export function formatExecutionStatus(status?: string): { label: string; color: string } {
  if (status === 'SUCCESS') return { label: '成功', color: 'success' }
  if (status === 'FAILED') return { label: '失败', color: 'error' }
  if (status === 'PAUSED') return { label: '挂起', color: 'warning' }
  if (status === 'RUNNING') return { label: '执行中', color: 'processing' }
  return { label: status || '-', color: 'default' }
}

export function tagColor(tagCode: string): string {
  const colors = ['blue', 'green', 'purple', 'cyan', 'geekblue', 'gold']
  let hash = 0
  for (const ch of tagCode) hash = (hash * 31 + ch.charCodeAt(0)) >>> 0
  return colors[hash % colors.length]
}

export function formatDateTime(value?: string): string {
  return value ? value.replace('T', ' ').slice(0, 19) : '-'
}
```

- [ ] **Step 5: Run test to verify it passes**

```bash
cd frontend
npm test -- cdpPresentation.test.ts
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add frontend/src/services/cdpApi.ts \
  frontend/src/pages/cdp-users/cdpPresentation.ts \
  frontend/src/pages/cdp-users/cdpPresentation.test.ts
git commit -m "feat: add cdp frontend api helpers"
```

### Task 10: Frontend CDP Pages and Routes

**Files:**
- Create: `frontend/src/pages/cdp-users/index.tsx`
- Create: `frontend/src/pages/cdp-user-detail/index.tsx`
- Create: `frontend/src/pages/canvas-users/index.tsx`
- Modify: `frontend/src/App.tsx`
- Modify: `frontend/src/components/layout/AppLayout.tsx`
- Modify: `frontend/src/pages/canvas-list/index.tsx`
- Modify: `frontend/src/pages/canvas-editor/index.tsx`
- Modify: `frontend/src/pages/canvas-stats/index.tsx`

- [ ] **Step 1: Add CDP users page**

Create `frontend/src/pages/cdp-users/index.tsx`:

```tsx
import { useEffect, useState } from 'react'
import { Button, Input, Space, Table, Tag, Typography, message } from 'antd'
import { TagsOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import { useNavigate } from 'react-router-dom'
import { cdpApi, type CanvasUserRow } from '../../services/cdpApi'
import { formatDateTime, formatExecutionStatus, tagColor } from './cdpPresentation'

const { Title } = Typography

export default function CdpUsersPage() {
  const navigate = useNavigate()
  const [keyword, setKeyword] = useState('')
  const [rows, setRows] = useState<CanvasUserRow[]>([])
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    setRows([])
  }, [])

  const filtered = rows.filter(row => !keyword || row.userId.includes(keyword))

  const columns: ColumnsType<CanvasUserRow> = [
    { title: '用户 ID', dataIndex: 'userId', render: v => <Button type="link" onClick={() => navigate(`/cdp/users/${encodeURIComponent(v)}`)}>{v}</Button> },
    { title: '执行次数', dataIndex: 'executionCount', width: 100, align: 'right' },
    { title: '最近状态', dataIndex: 'latestStatus', width: 100, render: v => { const s = formatExecutionStatus(v); return <Tag color={s.color}>{s.label}</Tag> } },
    { title: '当前标签', dataIndex: 'tags', render: tags => tags?.map((tag: any) => <Tag key={tag.tagCode} color={tagColor(tag.tagCode)}>{tag.tagName || tag.tagCode}</Tag>) },
    { title: '最近进入', dataIndex: 'lastEnteredAt', width: 180, render: formatDateTime },
  ]

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 20 }}>
        <Title level={4} style={{ margin: 0 }}>CDP 用户中心</Title>
        <Space>
          <Input.Search placeholder="搜索 userId" value={keyword} onChange={e => setKeyword(e.target.value)} style={{ width: 240 }} />
          <Button icon={<TagsOutlined />} onClick={() => message.info('批量打标入口在画布用户页和用户详情页可用')}>批量打标</Button>
        </Space>
      </div>
      <Table rowKey="userId" columns={columns} dataSource={filtered} loading={loading} />
    </div>
  )
}
```

- [ ] **Step 2: Add user detail page**

Create `frontend/src/pages/cdp-user-detail/index.tsx`:

```tsx
import { useEffect, useState } from 'react'
import { Button, Card, Form, Input, Modal, Popconfirm, Space, Table, Tag, Typography, message } from 'antd'
import { ArrowLeftOutlined, PlusOutlined } from '@ant-design/icons'
import { useNavigate, useParams } from 'react-router-dom'
import { cdpApi, type CdpUserDetail, type CdpUserTag, type CdpUserTagHistory } from '../../services/cdpApi'
import { buildTagWritePayload, formatDateTime, tagColor } from '../cdp-users/cdpPresentation'

const { Title, Text } = Typography

export default function CdpUserDetailPage() {
  const { userId = '' } = useParams()
  const navigate = useNavigate()
  const [detail, setDetail] = useState<CdpUserDetail | null>(null)
  const [tags, setTags] = useState<CdpUserTag[]>([])
  const [history, setHistory] = useState<CdpUserTagHistory[]>([])
  const [modalOpen, setModalOpen] = useState(false)
  const [form] = Form.useForm()

  const load = async () => {
    const [u, t, h] = await Promise.all([
      cdpApi.getUser(userId),
      cdpApi.listUserTags(userId),
      cdpApi.listUserTagHistory(userId),
    ])
    setDetail(u.data)
    setTags(t.data ?? [])
    setHistory(h.data ?? [])
  }

  useEffect(() => { if (userId) load() }, [userId])

  const saveTag = async () => {
    const values = await form.validateFields()
    await cdpApi.addUserTag(userId, buildTagWritePayload(values))
    message.success('标签已写入')
    setModalOpen(false)
    form.resetFields()
    load()
  }

  const removeTag = async (tagCode: string) => {
    await cdpApi.removeUserTag(userId, tagCode)
    message.success('标签已移除')
    load()
  }

  return (
    <div>
      <Space style={{ marginBottom: 16 }}>
        <Button type="text" icon={<ArrowLeftOutlined />} onClick={() => navigate(-1)} />
        <Title level={4} style={{ margin: 0 }}>用户详情</Title>
      </Space>

      <Card style={{ marginBottom: 16 }}>
        <Space direction="vertical" size={4}>
          <Text strong>{detail?.displayName || userId}</Text>
          <Text type="secondary">User ID: {userId}</Text>
          <Text type="secondary">最近活跃: {formatDateTime(detail?.lastSeenAt)}</Text>
        </Space>
      </Card>

      <Card title="当前标签" extra={<Button size="small" icon={<PlusOutlined />} onClick={() => setModalOpen(true)}>打标签</Button>} style={{ marginBottom: 16 }}>
        <Space wrap>
          {tags.map(tag => (
            <Popconfirm key={tag.tagCode} title="移除该标签？" onConfirm={() => removeTag(tag.tagCode)}>
              <Tag color={tagColor(tag.tagCode)} style={{ cursor: 'pointer' }}>{tag.tagName || tag.tagCode}: {tag.tagValue || '-'}</Tag>
            </Popconfirm>
          ))}
          {tags.length === 0 && <Text type="secondary">暂无标签</Text>}
        </Space>
      </Card>

      <Card title="标签历史">
        <Table rowKey={(_, index) => String(index)} dataSource={history} pagination={false} size="small"
          columns={[
            { title: '标签', dataIndex: 'tagCode' },
            { title: '操作', dataIndex: 'operation' },
            { title: '旧值', dataIndex: 'oldValue' },
            { title: '新值', dataIndex: 'newValue' },
            { title: '来源', dataIndex: 'sourceType' },
            { title: '时间', dataIndex: 'operatedAt', render: formatDateTime },
          ]} />
      </Card>

      <Modal title="打标签" open={modalOpen} onOk={saveTag} onCancel={() => setModalOpen(false)} okText="保存" cancelText="取消">
        <Form form={form} layout="vertical">
          <Form.Item name="tagCode" label="标签编码" rules={[{ required: true, message: '请输入标签编码' }]}>
            <Input placeholder="high_value" />
          </Form.Item>
          <Form.Item name="tagValue" label="标签值">
            <Input placeholder="true / 100 / 字符串" />
          </Form.Item>
          <Form.Item name="reason" label="原因">
            <Input.TextArea rows={2} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
```

- [ ] **Step 3: Add canvas users page**

Create `frontend/src/pages/canvas-users/index.tsx`:

```tsx
import { useEffect, useState } from 'react'
import { Button, Drawer, Space, Table, Tag, Typography } from 'antd'
import { ArrowLeftOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import { useNavigate, useParams } from 'react-router-dom'
import { cdpApi, type CanvasUserRow } from '../../services/cdpApi'
import { formatDateTime, formatExecutionStatus, tagColor } from '../cdp-users/cdpPresentation'

const { Title, Text } = Typography

export default function CanvasUsersPage() {
  const { id = '' } = useParams()
  const canvasId = Number(id)
  const navigate = useNavigate()
  const [rows, setRows] = useState<CanvasUserRow[]>([])
  const [selected, setSelected] = useState<CanvasUserRow | null>(null)
  const [executions, setExecutions] = useState<any[]>([])
  const [loading, setLoading] = useState(false)

  const load = async () => {
    setLoading(true)
    try {
      const res = await cdpApi.listCanvasUsers(canvasId)
      setRows(res.data ?? [])
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { if (canvasId) load() }, [canvasId])

  const openUser = async (row: CanvasUserRow) => {
    setSelected(row)
    const res = await cdpApi.listCanvasUserExecutions(canvasId, row.userId)
    setExecutions(res.data ?? [])
  }

  const columns: ColumnsType<CanvasUserRow> = [
    { title: '用户 ID', dataIndex: 'userId', render: (_, row) => <Button type="link" onClick={() => openUser(row)}>{row.userId}</Button> },
    { title: '执行次数', dataIndex: 'executionCount', width: 100, align: 'right' },
    { title: '成功', dataIndex: 'successCount', width: 80, align: 'right' },
    { title: '失败', dataIndex: 'failedCount', width: 80, align: 'right' },
    { title: '最近状态', dataIndex: 'latestStatus', width: 100, render: v => { const s = formatExecutionStatus(v); return <Tag color={s.color}>{s.label}</Tag> } },
    { title: '标签', dataIndex: 'tags', render: tags => tags?.map((tag: any) => <Tag key={tag.tagCode} color={tagColor(tag.tagCode)}>{tag.tagName || tag.tagCode}</Tag>) },
    { title: '最近进入', dataIndex: 'lastEnteredAt', width: 180, render: formatDateTime },
  ]

  return (
    <div>
      <Space style={{ marginBottom: 16 }}>
        <Button type="text" icon={<ArrowLeftOutlined />} onClick={() => navigate(-1)} />
        <Title level={4} style={{ margin: 0 }}>画布用户数据</Title>
      </Space>
      <Table rowKey="userId" columns={columns} dataSource={rows} loading={loading} />

      <Drawer title={selected?.userId} open={!!selected} width={640} onClose={() => setSelected(null)}>
        <Space direction="vertical" style={{ width: '100%' }}>
          <Button type="link" onClick={() => selected && navigate(`/cdp/users/${encodeURIComponent(selected.userId)}`)}>打开用户详情</Button>
          <Text strong>执行记录</Text>
          <Table rowKey="id" dataSource={executions} pagination={false} size="small"
            columns={[
              { title: '执行 ID', dataIndex: 'id', ellipsis: true },
              { title: '状态', dataIndex: 'status' },
              { title: '触发', dataIndex: 'triggerType' },
              { title: '时间', dataIndex: 'createdAt', render: formatDateTime },
            ]} />
        </Space>
      </Drawer>
    </div>
  )
}
```

- [ ] **Step 4: Add routes and menu**

Modify `frontend/src/App.tsx`:

```tsx
import CdpUsersPage from './pages/cdp-users'
import CdpUserDetailPage from './pages/cdp-user-detail'
import CanvasUsersPage from './pages/canvas-users'
```

Add routes inside authenticated/admin layout:

```tsx
<Route path="/cdp/users" element={<CdpUsersPage />} />
<Route path="/cdp/users/:userId" element={<CdpUserDetailPage />} />
<Route path="/canvas/:id/users" element={<CanvasUsersPage />} />
```

Modify `frontend/src/components/layout/AppLayout.tsx`:

- Import `IdcardOutlined` from `@ant-design/icons`.
- In `selectedKey`, add:

```tsx
if (location.pathname.startsWith('/cdp/users')) return 'cdp-users'
```

- Add this item under the `marketing` group after `旅程管理`:

```tsx
{
  key: 'cdp-users',
  icon: <IdcardOutlined />,
  label: 'CDP 用户中心',
  onClick: () => navigate('/cdp/users'),
},
```

- [ ] **Step 5: Add canvas entry buttons**

Modify `frontend/src/pages/canvas-list/index.tsx` in the action column by adding a button beside edit/stat buttons:

```tsx
<Button size="small" onClick={() => navigate(`/canvas/${record.id}/users`)}>用户数据</Button>
```

Modify `frontend/src/pages/canvas-editor/index.tsx` in the top toolbar beside the existing stats/trace controls:

```tsx
<Button size="small" onClick={() => navigate(`/canvas/${canvasId}/users`)}>
  用户
</Button>
```

Modify `frontend/src/pages/canvas-stats/index.tsx` in the page header controls:

```tsx
<Button onClick={() => navigate(`/canvas/${id}/users`)}>用户明细</Button>
```

- [ ] **Step 6: Run frontend tests and build**

```bash
cd frontend
npm test -- cdpPresentation.test.ts
npm run build
```

Expected: tests PASS, TypeScript build PASS.

- [ ] **Step 7: Commit**

```bash
git add frontend/src/pages/cdp-users \
  frontend/src/pages/cdp-user-detail \
  frontend/src/pages/canvas-users \
  frontend/src/App.tsx \
  frontend/src/components/layout/AppLayout.tsx \
  frontend/src/pages/canvas-list/index.tsx \
  frontend/src/pages/canvas-editor/index.tsx \
  frontend/src/pages/canvas-stats/index.tsx
git commit -m "feat: add cdp user frontend"
```

### Task 11: Tag Configuration Frontend Fields

**Files:**
- Modify: `frontend/src/pages/tag-config/index.tsx`
- Test: `frontend/src/pages/tag-config/tagConfigPayload.test.ts`
- Create: `frontend/src/pages/tag-config/tagConfigPayload.ts`

- [ ] **Step 1: Extract payload helper and write failing test**

Create `frontend/src/pages/tag-config/tagConfigPayload.test.ts`:

```ts
import { describe, expect, it } from 'vitest'
import { normalizeTagDefinitionPayload } from './tagConfigPayload'

describe('tagConfigPayload', () => {
  it('normalizes cdp tag definition fields', () => {
    expect(normalizeTagDefinitionPayload({
      name: '高价值',
      tagCode: 'high_value',
      tagType: 'offline',
      enabled: true,
      valueType: 'BOOLEAN',
      manualEnabled: true,
      defaultTtlDays: 30,
      category: '生命周期',
      owner: 'growth',
      writePolicy: 'UPSERT',
    })).toEqual({
      name: '高价值',
      tagCode: 'high_value',
      tagType: 'offline',
      enabled: 1,
      valueType: 'BOOLEAN',
      manualEnabled: 1,
      defaultTtlDays: 30,
      category: '生命周期',
      owner: 'growth',
      writePolicy: 'UPSERT',
    })
  })
})
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd frontend
npm test -- tagConfigPayload.test.ts
```

Expected: FAIL because `tagConfigPayload.ts` does not exist.

- [ ] **Step 3: Add helper**

Create `frontend/src/pages/tag-config/tagConfigPayload.ts`:

```ts
export function normalizeTagDefinitionPayload(values: any) {
  return {
    ...values,
    enabled: values.enabled ? 1 : 0,
    manualEnabled: values.manualEnabled ? 1 : 0,
    defaultTtlDays: values.defaultTtlDays ?? null,
    writePolicy: values.writePolicy || 'UPSERT',
  }
}
```

- [ ] **Step 4: Modify tag config page**

Modify `frontend/src/pages/tag-config/index.tsx`:

- Import helper:

```tsx
import { normalizeTagDefinitionPayload } from './tagConfigPayload'
```

- Extend `TagDef`:

```tsx
interface TagDef {
  id: number
  name: string
  tagCode: string
  tagType: string
  description?: string
  enabled: number
  valueType?: string
  manualEnabled?: number
  defaultTtlDays?: number
  category?: string
  owner?: string
  writePolicy?: string
}
```

- In `openCreate`, set defaults:

```tsx
form.setFieldsValue({
  tagType: 'offline',
  enabled: true,
  valueType: 'STRING',
  manualEnabled: true,
  writePolicy: 'UPSERT',
})
```

- In `openEdit`, map `manualEnabled`:

```tsx
form.setFieldsValue({ ...r, enabled: r.enabled === 1, manualEnabled: r.manualEnabled !== 0 })
```

- Replace payload body in `handleOk`:

```tsx
const body = normalizeTagDefinitionPayload(values)
```

- Add columns before status:

```tsx
{ title: '值类型', dataIndex: 'valueType', width: 90, render: v => v || 'STRING' },
{ title: '人工打标', dataIndex: 'manualEnabled', width: 90,
  render: v => <Tag color={v === 0 ? 'default' : 'green'}>{v === 0 ? '关闭' : '允许'}</Tag> },
```

- Add form fields after tag type:

```tsx
<Form.Item name="valueType" label="标签值类型" rules={[{ required: true }]}>
  <Select options={[
    { value: 'STRING', label: '字符串' },
    { value: 'NUMBER', label: '数字' },
    { value: 'BOOLEAN', label: '布尔' },
    { value: 'JSON', label: 'JSON' },
  ]} />
</Form.Item>
<Form.Item name="manualEnabled" label="允许人工打标" valuePropName="checked">
  <Switch checkedChildren="允许" unCheckedChildren="关闭" />
</Form.Item>
<Form.Item name="defaultTtlDays" label="默认有效期（天）">
  <Input type="number" min={1} placeholder="留空表示长期有效" />
</Form.Item>
<Form.Item name="category" label="分类">
  <Input placeholder="如：生命周期" />
</Form.Item>
<Form.Item name="owner" label="负责人">
  <Input placeholder="如：growth" />
</Form.Item>
<Form.Item name="writePolicy" label="写入策略">
  <Select disabled options={[{ value: 'UPSERT', label: '覆盖当前值' }]} />
</Form.Item>
```

- [ ] **Step 5: Run frontend tests and build**

```bash
cd frontend
npm test -- tagConfigPayload.test.ts
npm run build
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add frontend/src/pages/tag-config/index.tsx \
  frontend/src/pages/tag-config/tagConfigPayload.ts \
  frontend/src/pages/tag-config/tagConfigPayload.test.ts
git commit -m "feat: extend tag config for cdp"
```

### Task 12: Final Verification

**Files:**
- Verify all backend and frontend changes.

- [ ] **Step 1: Run backend unit tests for CDP slice**

```bash
cd backend/canvas-engine
mvn -Dtest='Cdp*Test,CanvasUser*Test,TagDefinitionCdpFieldsTest,CanvasExecutionServiceCdpTest' test
```

Expected: PASS.

- [ ] **Step 2: Run broader backend regression**

```bash
cd backend/canvas-engine
mvn test
```

Expected: PASS. If unrelated pre-existing tests fail, capture the failing test names and confirm whether they are tied to current work before changing anything.

- [ ] **Step 3: Run frontend unit tests**

```bash
cd frontend
npm test
```

Expected: PASS.

- [ ] **Step 4: Run frontend production build**

```bash
cd frontend
npm run build
```

Expected: PASS.

- [ ] **Step 5: Inspect git diff for accidental unrelated edits**

```bash
git status --short
git diff --stat
```

Expected: only CDP-related backend/frontend files are modified beyond pre-existing unrelated worktree changes.

- [ ] **Step 6: Confirm final commit state**

Run:

```bash
git log --oneline -12
git status --short
```

Expected: the recent log contains the CDP task commits from this plan. Any remaining `git status --short` output is either empty or clearly attributable to pre-existing unrelated worktree changes that were present before this plan execution. Do not create an empty commit.

## Self-Review

- Spec coverage: The plan covers the first-batch CDP loop from the spec: schema, user profile, current tags, tag history, manual tagging, batch tagging, canvas users, canvas write-tag node, frontend pages, and tag configuration extensions.
- Deferred spec items: Event ingestion, rule tags, advanced user 360 insight charts, field-level privacy permissions, tag approvals, and governance are explicitly out of scope for this first implementation plan.
- Placeholder scan: The plan avoids placeholder instructions and gives concrete file paths, commands, expected results, and code snippets for the main implementation changes.
- Type consistency: Backend uses `Cdp*` entity/DTO names consistently; frontend uses `CdpUserTag`, `CdpUserDetail`, `CanvasUserRow`, and `TagWritePayload` consistently.
