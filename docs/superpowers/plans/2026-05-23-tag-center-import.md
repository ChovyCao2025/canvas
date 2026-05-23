# Tag Center Import Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a tag center with configurable tag values, standalone identity type configuration, and persisted historical tag imports from API push, Excel upload, and external API pull.

**Architecture:** Add focused domain models under `domain/meta`, keep validation and upsert rules in services, and expose thin WebFlux controllers matching existing management APIs. Frontend adds three admin pages and extends the config panel data-source loader so `TAGGER` can load tag values from the selected tag.

**Tech Stack:** Spring Boot WebFlux, MyBatis-Plus, Flyway, Hutool Excel, JUnit 5 + Mockito, React, TypeScript, Ant Design, Vitest.

---

## Scope Check

The feature spans backend metadata, import processing, Excel parsing, external pull configuration, frontend admin pages, and node configuration. These parts are not independent products: each depends on the same `idType + idValue + tagCode + tagValue` contract and the same tag value dictionary. Implement as one plan with small commits so each task remains testable.

## File Structure

Backend files to create:

- `backend/canvas-engine/src/main/resources/db/migration/V60__tag_center_import.sql`  
  Creates `identity_type`, reshapes `tag_definition`, and adds tag value/import tables.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/IdentityType.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/IdentityTypeMapper.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/TagValueDefinition.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/TagValueDefinitionMapper.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/UserTagCurrent.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/UserTagCurrentMapper.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/TagImportBatch.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/TagImportBatchMapper.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/TagImportError.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/TagImportErrorMapper.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/TagImportSource.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/TagImportSourceMapper.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/IdentityTypeService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/TagDefinitionService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/TagImportService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/TagImportSourceService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/TagImportPushReq.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/TagImportRow.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/TagImportResult.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/controller/IdentityTypeController.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/controller/TagImportController.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/controller/TagImportSourceController.java`

Backend files to modify:

- `backend/canvas-engine/pom.xml`  
  Ensure Hutool Excel transitive classes are available through existing `hutool-all`.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/TagDefinition.java`  
  Add `valueType`, widen comments, make `tagCode` globally unique by behavior.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/TagDefinitionMapper.java`  
  Add custom upsert/select methods only if MyBatis-Plus wrappers are not enough.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/controller/TagDefinitionController.java`  
  Delegate validation to `TagDefinitionService` and add tag value endpoints.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/controller/MetaController.java`  
  Add `/meta/identity-types` and `/meta/tagger-tag-values`; keep `/meta/tagger-tags` DB-backed.

Backend tests to create:

- `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/meta/IdentityTypeServiceTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/meta/TagDefinitionServiceTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/meta/TagImportServiceTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/meta/TagImportSourceServiceTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/TagImportControllerTest.java`

Frontend files to create:

- `frontend/src/pages/identity-types/index.tsx`
- `frontend/src/pages/tag-import/index.tsx`
- `frontend/src/pages/tag-config/tagTypes.ts`
- `frontend/src/pages/tag-config/tagValueEditor.tsx`
- `frontend/src/pages/tag-import/tagImportTypes.ts`
- `frontend/src/pages/tag-import/tagImportApiDoc.tsx`
- `frontend/src/pages/tag-import/tagImportBatchList.tsx`
- `frontend/src/pages/tag-import/tagImportSourcePanel.tsx`
- `frontend/src/pages/tag-import/tagImportExcelPanel.tsx`
- `frontend/src/pages/tag-config/tagValueEditor.test.ts`
- `frontend/src/components/config-panel/dataSource.test.ts`

Frontend files to modify:

- `frontend/src/services/api.ts`  
  Add identity type, tag value, and tag import API clients.
- `frontend/src/types/index.ts`  
  Add typed metadata models used by the new pages.
- `frontend/src/App.tsx`  
  Add admin routes for ID type configuration and tag import.
- `frontend/src/components/layout/AppLayout.tsx`  
  Add menu entries and selected-key handling.
- `frontend/src/pages/tag-config/index.tsx`  
  Upgrade tag config to tag center with value management.
- `frontend/src/components/config-panel/index.tsx`  
  Support `{fieldKey}` placeholders in `dataSource` and reload dependent selects.

---

### Task 1: Database Migration And Metadata Models

**Files:**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V60__tag_center_import.sql`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/TagDefinition.java`
- Create domain and mapper files listed in the File Structure backend creation list.

- [ ] **Step 1: Add the Flyway migration**

Create `backend/canvas-engine/src/main/resources/db/migration/V60__tag_center_import.sql` with:

```sql
ALTER TABLE tag_definition
  DROP INDEX uk_tag_code_type,
  ADD COLUMN value_type VARCHAR(16) NOT NULL DEFAULT 'STRING' COMMENT 'STRING / NUMBER / BOOLEAN' AFTER tag_type,
  MODIFY COLUMN name VARCHAR(100) NOT NULL,
  MODIFY COLUMN description VARCHAR(500) NULL;

ALTER TABLE tag_definition
  ADD UNIQUE KEY uk_tag_code (tag_code);

CREATE TABLE identity_type (
  id BIGINT NOT NULL AUTO_INCREMENT,
  code VARCHAR(64) NOT NULL,
  name VARCHAR(100) NOT NULL,
  description VARCHAR(500) NULL,
  enabled TINYINT NOT NULL DEFAULT 1,
  allow_import TINYINT NOT NULL DEFAULT 1,
  multi_value TINYINT NOT NULL DEFAULT 0,
  priority INT NOT NULL DEFAULT 100,
  participate_mapping TINYINT NOT NULL DEFAULT 0,
  created_by VARCHAR(64) NULL,
  created_at DATETIME NULL,
  updated_at DATETIME NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_identity_type_code (code),
  INDEX idx_identity_type_enabled (enabled, allow_import)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='身份类型配置';

CREATE TABLE tag_value_definition (
  id BIGINT NOT NULL AUTO_INCREMENT,
  tag_code VARCHAR(64) NOT NULL,
  value VARCHAR(255) NOT NULL,
  label VARCHAR(255) NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  enabled TINYINT NOT NULL DEFAULT 1,
  source VARCHAR(32) NOT NULL DEFAULT 'MANUAL',
  description VARCHAR(500) NULL,
  created_at DATETIME NULL,
  updated_at DATETIME NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_tag_value (tag_code, value),
  INDEX idx_tag_value_enabled (tag_code, enabled, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标签值字典';

CREATE TABLE user_tag_current (
  id BIGINT NOT NULL AUTO_INCREMENT,
  id_type VARCHAR(64) NOT NULL,
  id_value VARCHAR(255) NOT NULL,
  tag_code VARCHAR(64) NOT NULL,
  tag_value VARCHAR(255) NOT NULL,
  tag_time DATETIME NULL,
  source_type VARCHAR(32) NOT NULL,
  source_batch_id BIGINT NULL,
  updated_at DATETIME NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_tag_current (id_type, id_value, tag_code),
  INDEX idx_user_tag_tag (tag_code, tag_value),
  INDEX idx_user_tag_identity (id_type, id_value)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户当前标签';

CREATE TABLE tag_import_batch (
  id BIGINT NOT NULL AUTO_INCREMENT,
  source_type VARCHAR(32) NOT NULL,
  status VARCHAR(32) NOT NULL,
  file_name VARCHAR(255) NULL,
  external_url VARCHAR(1000) NULL,
  total_rows INT NOT NULL DEFAULT 0,
  success_rows INT NOT NULL DEFAULT 0,
  failed_rows INT NOT NULL DEFAULT 0,
  created_by VARCHAR(64) NULL,
  started_at DATETIME NULL,
  finished_at DATETIME NULL,
  error_message VARCHAR(1000) NULL,
  created_at DATETIME NULL,
  updated_at DATETIME NULL,
  PRIMARY KEY (id),
  INDEX idx_import_batch_status (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标签导入批次';

CREATE TABLE tag_import_error (
  id BIGINT NOT NULL AUTO_INCREMENT,
  batch_id BIGINT NOT NULL,
  row_no INT NOT NULL,
  raw_payload TEXT NULL,
  error_code VARCHAR(64) NOT NULL,
  error_msg VARCHAR(1000) NOT NULL,
  created_at DATETIME NULL,
  PRIMARY KEY (id),
  INDEX idx_import_error_batch (batch_id, row_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标签导入错误行';

CREATE TABLE tag_import_source (
  id BIGINT NOT NULL AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL,
  url VARCHAR(1000) NOT NULL,
  method VARCHAR(16) NOT NULL DEFAULT 'GET',
  headers_json TEXT NULL,
  body_template TEXT NULL,
  page_param VARCHAR(64) NULL,
  page_size_param VARCHAR(64) NULL,
  page_size INT NOT NULL DEFAULT 500,
  records_path VARCHAR(255) NOT NULL DEFAULT '$',
  field_mapping TEXT NOT NULL,
  enabled TINYINT NOT NULL DEFAULT 1,
  created_by VARCHAR(64) NULL,
  created_at DATETIME NULL,
  updated_at DATETIME NULL,
  PRIMARY KEY (id),
  INDEX idx_tag_import_source_enabled (enabled, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标签外部拉取源';

INSERT INTO identity_type
  (code, name, description, enabled, allow_import, multi_value, priority, participate_mapping, created_by, created_at, updated_at)
VALUES
  ('user_id', '用户ID', '系统内部用户ID', 1, 1, 0, 10, 0, 'system', NOW(), NOW()),
  ('mobile', '手机号', '用户手机号', 1, 1, 0, 20, 0, 'system', NOW(), NOW()),
  ('open_id', 'OpenID', '渠道OpenID', 1, 1, 1, 30, 0, 'system', NOW(), NOW()),
  ('email', '邮箱', '用户邮箱', 1, 1, 0, 40, 0, 'system', NOW(), NOW()),
  ('member_no', '会员号', '业务会员号', 1, 1, 0, 50, 0, 'system', NOW(), NOW());

INSERT INTO tag_value_definition (tag_code, value, label, sort_order, enabled, source, created_at, updated_at)
VALUES
  ('new_user', '1', '是', 10, 1, 'MANUAL', NOW(), NOW()),
  ('new_user', '0', '否', 20, 1, 'MANUAL', NOW(), NOW()),
  ('high_value', 'VIP', 'VIP', 10, 1, 'MANUAL', NOW(), NOW()),
  ('high_value', 'NORMAL', '普通', 20, 1, 'MANUAL', NOW(), NOW()),
  ('churn_risk', 'HIGH', '高风险', 10, 1, 'MANUAL', NOW(), NOW()),
  ('churn_risk', 'LOW', '低风险', 20, 1, 'MANUAL', NOW(), NOW());
```

- [ ] **Step 2: Update `TagDefinition`**

Modify `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/TagDefinition.java` so it contains these fields:

```java
package org.chovy.canvas.domain.meta;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tag_definition")
public class TagDefinition {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String tagCode;
    private String tagType;
    private String valueType;
    private String description;
    private Integer enabled;
    private String createdBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 3: Add metadata entities**

Create entity files matching the migration column names. Use `@TableName`, `@TableId(type = IdType.AUTO)`, Lombok `@Data`, and `FieldFill` on timestamp fields. `TagImportError.rawPayload`, `TagImportSource.headersJson`, `TagImportSource.bodyTemplate`, and `TagImportSource.fieldMapping` are `String` fields.

Example for `IdentityType.java`:

```java
package org.chovy.canvas.domain.meta;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("identity_type")
public class IdentityType {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private String name;
    private String description;
    private Integer enabled;
    private Integer allowImport;
    private Integer multiValue;
    private Integer priority;
    private Integer participateMapping;
    private String createdBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 4: Add mapper interfaces**

Create one mapper per entity:

```java
package org.chovy.canvas.domain.meta;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface IdentityTypeMapper extends BaseMapper<IdentityType> {}
```

Repeat the same pattern for `TagValueDefinitionMapper`, `UserTagCurrentMapper`, `TagImportBatchMapper`, `TagImportErrorMapper`, and `TagImportSourceMapper`.

- [ ] **Step 5: Compile backend**

Run:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -f backend/pom.xml -pl canvas-engine -am -DskipTests compile
```

Expected: build succeeds with no Java compilation errors.

- [ ] **Step 6: Commit**

```bash
git add backend/canvas-engine/src/main/resources/db/migration/V60__tag_center_import.sql \
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta
git commit -m "feat: add tag center data model"
```

---

### Task 2: Identity Type Service And API

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/IdentityTypeService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/controller/IdentityTypeController.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/controller/MetaController.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/meta/IdentityTypeServiceTest.java`

- [ ] **Step 1: Write service tests**

Create `IdentityTypeServiceTest.java`:

```java
package org.chovy.canvas.domain.meta;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IdentityTypeServiceTest {
    private IdentityTypeMapper mapper;
    private UserTagCurrentMapper userTagCurrentMapper;
    private IdentityTypeService service;

    @BeforeEach
    void setUp() {
        mapper = mock(IdentityTypeMapper.class);
        userTagCurrentMapper = mock(UserTagCurrentMapper.class);
        service = new IdentityTypeService(mapper, userTagCurrentMapper);
    }

    @Test
    void create_setsDefaultsAndNormalizesCode() {
        IdentityType body = new IdentityType();
        body.setCode(" Mobile ");
        body.setName("手机号");

        service.create(body);

        ArgumentCaptor<IdentityType> captor = ArgumentCaptor.forClass(IdentityType.class);
        verify(mapper).insert(captor.capture());
        assertThat(captor.getValue().getCode()).isEqualTo("mobile");
        assertThat(captor.getValue().getEnabled()).isEqualTo(1);
        assertThat(captor.getValue().getAllowImport()).isEqualTo(1);
        assertThat(captor.getValue().getMultiValue()).isEqualTo(0);
        assertThat(captor.getValue().getPriority()).isEqualTo(100);
        assertThat(captor.getValue().getParticipateMapping()).isEqualTo(0);
    }

    @Test
    void requireImportable_rejectsDisabledType() {
        IdentityType disabled = new IdentityType();
        disabled.setCode("mobile");
        disabled.setEnabled(0);
        disabled.setAllowImport(1);
        when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(disabled);

        assertThatThrownBy(() -> service.requireImportable("mobile"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不可用于导入");
    }

    @Test
    void listImportable_returnsEnabledAndImportableTypes() {
        IdentityType type = new IdentityType();
        type.setCode("mobile");
        type.setName("手机号");
        when(mapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(type));

        assertThat(service.listImportable()).containsExactly(type);
    }
}
```

- [ ] **Step 2: Run service tests and verify failure**

Run:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -f backend/pom.xml -pl canvas-engine -am -Dtest=IdentityTypeServiceTest test
```

Expected: compilation fails because `IdentityTypeService` does not exist.

- [ ] **Step 3: Implement `IdentityTypeService`**

Create:

```java
package org.chovy.canvas.domain.meta;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class IdentityTypeService {
    private final IdentityTypeMapper mapper;
    private final UserTagCurrentMapper userTagCurrentMapper;

    public List<IdentityType> list(Integer enabled, Integer allowImport) {
        return mapper.selectList(new LambdaQueryWrapper<IdentityType>()
                .eq(enabled != null, IdentityType::getEnabled, enabled)
                .eq(allowImport != null, IdentityType::getAllowImport, allowImport)
                .orderByAsc(IdentityType::getPriority)
                .orderByAsc(IdentityType::getId));
    }

    public List<IdentityType> listImportable() {
        return list(1, 1);
    }

    public IdentityType create(IdentityType body) {
        normalize(body);
        mapper.insert(body);
        return body;
    }

    public void update(Long id, IdentityType body) {
        body.setId(id);
        normalize(body);
        mapper.updateById(body);
    }

    public void delete(Long id) {
        IdentityType existing = mapper.selectById(id);
        if (existing == null) {
            return;
        }
        Long used = userTagCurrentMapper.selectCount(new LambdaQueryWrapper<UserTagCurrent>()
                .eq(UserTagCurrent::getIdType, existing.getCode()));
        if (used != null && used > 0) {
            throw new IllegalArgumentException("ID 类型已被历史标签使用，只能停用");
        }
        mapper.deleteById(id);
    }

    public IdentityType requireImportable(String code) {
        String normalized = normalizeCode(code);
        IdentityType type = mapper.selectOne(new LambdaQueryWrapper<IdentityType>()
                .eq(IdentityType::getCode, normalized)
                .last("LIMIT 1"));
        if (type == null) {
            throw new IllegalArgumentException("ID 类型不存在: " + normalized);
        }
        if (!Integer.valueOf(1).equals(type.getEnabled()) || !Integer.valueOf(1).equals(type.getAllowImport())) {
            throw new IllegalArgumentException("ID 类型不可用于导入: " + normalized);
        }
        return type;
    }

    private static void normalize(IdentityType body) {
        body.setCode(normalizeCode(body.getCode()));
        requireText(body.getName(), "ID 类型名称");
        if (body.getEnabled() == null) body.setEnabled(1);
        if (body.getAllowImport() == null) body.setAllowImport(1);
        if (body.getMultiValue() == null) body.setMultiValue(0);
        if (body.getPriority() == null) body.setPriority(100);
        if (body.getParticipateMapping() == null) body.setParticipateMapping(0);
    }

    private static String normalizeCode(String code) {
        String value = requireText(code, "ID 类型编码").toLowerCase(Locale.ROOT);
        if (!value.matches("[a-z][a-z0-9_]{1,63}")) {
            throw new IllegalArgumentException("ID 类型编码只能包含小写字母、数字、下划线，且以字母开头");
        }
        return value;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + "不能为空");
        }
        return value.trim();
    }
}
```

- [ ] **Step 4: Add controller and meta endpoint**

Create `IdentityTypeController.java`:

```java
package org.chovy.canvas.controller;

import org.chovy.canvas.common.PageResult;
import org.chovy.canvas.common.R;
import org.chovy.canvas.domain.meta.IdentityType;
import org.chovy.canvas.domain.meta.IdentityTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/canvas/identity-types")
@RequiredArgsConstructor
public class IdentityTypeController {
    private final IdentityTypeService service;

    @GetMapping
    public Mono<R<PageResult<IdentityType>>> list(
            @RequestParam(required = false) Integer enabled,
            @RequestParam(required = false) Integer allowImport) {
        return Mono.fromCallable(() -> {
            var rows = service.list(enabled, allowImport);
            return R.ok(PageResult.of(rows.size(), rows));
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping
    public Mono<R<IdentityType>> create(@RequestBody IdentityType body) {
        return Mono.fromCallable(() -> R.ok(service.create(body)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PutMapping("/{id}")
    public Mono<R<Void>> update(@PathVariable Long id, @RequestBody IdentityType body) {
        return Mono.fromRunnable(() -> service.update(id, body))
                .subscribeOn(Schedulers.boundedElastic())
                .thenReturn(R.ok());
    }

    @DeleteMapping("/{id}")
    public Mono<R<Void>> delete(@PathVariable Long id) {
        return Mono.fromRunnable(() -> service.delete(id))
                .subscribeOn(Schedulers.boundedElastic())
                .thenReturn(R.ok());
    }
}
```

In `MetaController`, inject `IdentityTypeService` and add:

```java
@GetMapping("/identity-types")
public Mono<R<List<StubOption>>> getIdentityTypes(
        @RequestParam(required = false, defaultValue = "1") Integer allowImport) {
    return Mono.fromCallable(() -> identityTypeService.list(1, allowImport).stream()
            .map(t -> new StubOption(t.getCode(), t.getName()))
            .collect(Collectors.toList()))
            .subscribeOn(Schedulers.boundedElastic())
            .map(R::ok);
}
```

- [ ] **Step 5: Run tests**

Run:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -f backend/pom.xml -pl canvas-engine -am -Dtest=IdentityTypeServiceTest test
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/IdentityTypeService.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/controller/IdentityTypeController.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/controller/MetaController.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/meta/IdentityTypeServiceTest.java
git commit -m "feat: add identity type configuration api"
```

---

### Task 3: Tag Values Service, Controller, And Meta Options

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/TagDefinitionService.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/controller/TagDefinitionController.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/controller/MetaController.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/meta/TagDefinitionServiceTest.java`

- [ ] **Step 1: Write service tests**

Create `TagDefinitionServiceTest.java`:

```java
package org.chovy.canvas.domain.meta;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TagDefinitionServiceTest {
    private TagDefinitionMapper tagMapper;
    private TagValueDefinitionMapper valueMapper;
    private UserTagCurrentMapper userTagMapper;
    private TagDefinitionService service;

    @BeforeEach
    void setUp() {
        tagMapper = mock(TagDefinitionMapper.class);
        valueMapper = mock(TagValueDefinitionMapper.class);
        userTagMapper = mock(UserTagCurrentMapper.class);
        service = new TagDefinitionService(tagMapper, valueMapper, userTagMapper);
    }

    @Test
    void create_defaultsEnabledAndStringValueType() {
        TagDefinition tag = new TagDefinition();
        tag.setName("用户等级");
        tag.setTagCode(" user_level ");
        tag.setTagType("offline");

        service.create(tag);

        ArgumentCaptor<TagDefinition> captor = ArgumentCaptor.forClass(TagDefinition.class);
        verify(tagMapper).insert(captor.capture());
        assertThat(captor.getValue().getTagCode()).isEqualTo("user_level");
        assertThat(captor.getValue().getValueType()).isEqualTo("STRING");
        assertThat(captor.getValue().getEnabled()).isEqualTo(1);
    }

    @Test
    void ensureValue_createsMissingValue() {
        when(valueMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        service.ensureValue("user_level", "VIP", "API_PUSH");

        ArgumentCaptor<TagValueDefinition> captor = ArgumentCaptor.forClass(TagValueDefinition.class);
        verify(valueMapper).insert(captor.capture());
        assertThat(captor.getValue().getTagCode()).isEqualTo("user_level");
        assertThat(captor.getValue().getValue()).isEqualTo("VIP");
        assertThat(captor.getValue().getLabel()).isEqualTo("VIP");
        assertThat(captor.getValue().getSource()).isEqualTo("API_PUSH");
    }

    @Test
    void validateTagValue_rejectsInvalidNumber() {
        TagDefinition tag = new TagDefinition();
        tag.setTagCode("amount_bucket");
        tag.setValueType("NUMBER");
        tag.setEnabled(1);
        when(tagMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(tag);

        assertThatThrownBy(() -> service.requireEnabledTagAndValidateValue("amount_bucket", "abc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("必须是数字");
    }

    @Test
    void listValues_returnsEnabledSortedValues() {
        TagValueDefinition value = new TagValueDefinition();
        value.setValue("VIP");
        value.setLabel("VIP");
        when(valueMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(value));

        assertThat(service.listValues("user_level", 1)).containsExactly(value);
    }
}
```

- [ ] **Step 2: Run service tests and verify failure**

Run:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -f backend/pom.xml -pl canvas-engine -am -Dtest=TagDefinitionServiceTest test
```

Expected: compilation fails because `TagDefinitionService` does not exist.

- [ ] **Step 3: Implement `TagDefinitionService`**

Create `TagDefinitionService.java`:

```java
package org.chovy.canvas.domain.meta;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TagDefinitionService {
    private static final Set<String> TAG_TYPES = Set.of("offline", "realtime");
    private static final Set<String> VALUE_TYPES = Set.of("STRING", "NUMBER", "BOOLEAN");

    private final TagDefinitionMapper tagMapper;
    private final TagValueDefinitionMapper valueMapper;
    private final UserTagCurrentMapper userTagCurrentMapper;

    public List<TagDefinition> list(String tagType, Integer enabled) {
        return tagMapper.selectList(new LambdaQueryWrapper<TagDefinition>()
                .eq(tagType != null && !tagType.isBlank(), TagDefinition::getTagType, tagType)
                .eq(enabled != null, TagDefinition::getEnabled, enabled)
                .orderByAsc(TagDefinition::getId));
    }

    public TagDefinition create(TagDefinition body) {
        normalize(body);
        tagMapper.insert(body);
        return body;
    }

    public void update(Long id, TagDefinition body) {
        body.setId(id);
        normalize(body);
        tagMapper.updateById(body);
    }

    public void delete(Long id) {
        TagDefinition tag = tagMapper.selectById(id);
        if (tag == null) {
            return;
        }
        Long used = userTagCurrentMapper.selectCount(new LambdaQueryWrapper<UserTagCurrent>()
                .eq(UserTagCurrent::getTagCode, tag.getTagCode()));
        if (used != null && used > 0) {
            throw new IllegalArgumentException("标签已被历史标签使用，只能停用");
        }
        tagMapper.deleteById(id);
    }

    public List<TagValueDefinition> listValues(String tagCode, Integer enabled) {
        return valueMapper.selectList(new LambdaQueryWrapper<TagValueDefinition>()
                .eq(TagValueDefinition::getTagCode, requireText(tagCode, "标签编码"))
                .eq(enabled != null, TagValueDefinition::getEnabled, enabled)
                .orderByAsc(TagValueDefinition::getSortOrder)
                .orderByAsc(TagValueDefinition::getId));
    }

    public TagValueDefinition createValue(String tagCode, TagValueDefinition body) {
        body.setTagCode(requireText(tagCode, "标签编码"));
        normalizeValue(body);
        valueMapper.insert(body);
        return body;
    }

    public void updateValue(Long id, TagValueDefinition body) {
        body.setId(id);
        normalizeValue(body);
        valueMapper.updateById(body);
    }

    public void deleteValue(Long id) {
        TagValueDefinition value = valueMapper.selectById(id);
        if (value == null) {
            return;
        }
        Long used = userTagCurrentMapper.selectCount(new LambdaQueryWrapper<UserTagCurrent>()
                .eq(UserTagCurrent::getTagCode, value.getTagCode())
                .eq(UserTagCurrent::getTagValue, value.getValue()));
        if (used != null && used > 0) {
            throw new IllegalArgumentException("标签值已被历史标签使用，只能停用");
        }
        valueMapper.deleteById(id);
    }

    public TagDefinition requireEnabledTagAndValidateValue(String tagCode, String tagValue) {
        TagDefinition tag = tagMapper.selectOne(new LambdaQueryWrapper<TagDefinition>()
                .eq(TagDefinition::getTagCode, requireText(tagCode, "标签编码"))
                .last("LIMIT 1"));
        if (tag == null) {
            throw new IllegalArgumentException("标签不存在: " + tagCode);
        }
        if (!Integer.valueOf(1).equals(tag.getEnabled())) {
            throw new IllegalArgumentException("标签已停用: " + tagCode);
        }
        validateValueType(tag.getValueType(), tagValue);
        return tag;
    }

    public void ensureValue(String tagCode, String tagValue, String source) {
        String normalizedValue = requireText(tagValue, "标签值");
        TagValueDefinition existing = valueMapper.selectOne(new LambdaQueryWrapper<TagValueDefinition>()
                .eq(TagValueDefinition::getTagCode, tagCode)
                .eq(TagValueDefinition::getValue, normalizedValue)
                .last("LIMIT 1"));
        if (existing != null) {
            return;
        }
        TagValueDefinition value = new TagValueDefinition();
        value.setTagCode(tagCode);
        value.setValue(normalizedValue);
        value.setLabel(normalizedValue);
        value.setSortOrder(0);
        value.setEnabled(1);
        value.setSource(source);
        valueMapper.insert(value);
    }

    private static void normalize(TagDefinition body) {
        body.setName(requireText(body.getName(), "标签名称"));
        body.setTagCode(requireCode(body.getTagCode(), "标签编码"));
        String tagType = body.getTagType() == null ? "offline" : body.getTagType().trim();
        if (!TAG_TYPES.contains(tagType)) {
            throw new IllegalArgumentException("标签类型只能是 offline 或 realtime");
        }
        body.setTagType(tagType);
        String valueType = body.getValueType() == null ? "STRING" : body.getValueType().trim().toUpperCase(Locale.ROOT);
        if (!VALUE_TYPES.contains(valueType)) {
            throw new IllegalArgumentException("标签值类型只能是 STRING、NUMBER、BOOLEAN");
        }
        body.setValueType(valueType);
        if (body.getEnabled() == null) body.setEnabled(1);
    }

    private static void normalizeValue(TagValueDefinition body) {
        body.setValue(requireText(body.getValue(), "标签值"));
        body.setLabel(requireText(body.getLabel() == null ? body.getValue() : body.getLabel(), "标签值显示名"));
        if (body.getSortOrder() == null) body.setSortOrder(0);
        if (body.getEnabled() == null) body.setEnabled(1);
        if (body.getSource() == null || body.getSource().isBlank()) body.setSource("MANUAL");
    }

    private static void validateValueType(String valueType, String tagValue) {
        String value = requireText(tagValue, "标签值");
        if ("NUMBER".equals(valueType)) {
            try {
                Double.parseDouble(value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("标签值必须是数字");
            }
        }
        if ("BOOLEAN".equals(valueType) && !Set.of("true", "false", "1", "0").contains(value.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("标签值必须是布尔值");
        }
    }

    private static String requireCode(String code, String fieldName) {
        String value = requireText(code, fieldName);
        if (!value.matches("[A-Za-z][A-Za-z0-9_]{1,63}")) {
            throw new IllegalArgumentException(fieldName + "只能包含字母、数字、下划线，且以字母开头");
        }
        return value;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + "不能为空");
        }
        return value.trim();
    }
}
```

- [ ] **Step 4: Modify `TagDefinitionController`**

Replace mapper usage with `TagDefinitionService`. Add endpoints:

```java
@GetMapping("/{tagCode}/values")
public Mono<R<List<TagValueDefinition>>> listValues(
        @PathVariable String tagCode,
        @RequestParam(required = false) Integer enabled) {
    return Mono.fromCallable(() -> R.ok(service.listValues(tagCode, enabled)))
            .subscribeOn(Schedulers.boundedElastic());
}

@PostMapping("/{tagCode}/values")
public Mono<R<TagValueDefinition>> createValue(
        @PathVariable String tagCode,
        @RequestBody TagValueDefinition body) {
    return Mono.fromCallable(() -> R.ok(service.createValue(tagCode, body)))
            .subscribeOn(Schedulers.boundedElastic());
}

@PutMapping("/values/{id}")
public Mono<R<Void>> updateValue(@PathVariable Long id, @RequestBody TagValueDefinition body) {
    return Mono.fromRunnable(() -> service.updateValue(id, body))
            .subscribeOn(Schedulers.boundedElastic())
            .thenReturn(R.ok());
}

@DeleteMapping("/values/{id}")
public Mono<R<Void>> deleteValue(@PathVariable Long id) {
    return Mono.fromRunnable(() -> service.deleteValue(id))
            .subscribeOn(Schedulers.boundedElastic())
            .thenReturn(R.ok());
}
```

- [ ] **Step 5: Add tag value meta endpoint**

In `MetaController`, inject `TagDefinitionService` and add:

```java
@GetMapping("/tagger-tag-values")
public Mono<R<List<StubOption>>> getTaggerTagValues(@RequestParam String tagCode) {
    return Mono.fromCallable(() -> tagDefinitionService.listValues(tagCode, 1).stream()
            .map(v -> new StubOption(v.getValue(), v.getLabel()))
            .collect(Collectors.toList()))
            .subscribeOn(Schedulers.boundedElastic())
            .map(R::ok);
}
```

Update existing `/meta/tagger-tags` DB path to call `tagDefinitionService.list(type, 1)` instead of directly querying the mapper.

- [ ] **Step 6: Run tests**

Run:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -f backend/pom.xml -pl canvas-engine -am -Dtest=TagDefinitionServiceTest test
```

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/TagDefinitionService.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/controller/TagDefinitionController.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/controller/MetaController.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/meta/TagDefinitionServiceTest.java
git commit -m "feat: add tag value management api"
```

---

### Task 4: Unified Import Service And API Push

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/TagImportRow.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/TagImportPushReq.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/TagImportResult.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/TagImportService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/controller/TagImportController.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/meta/TagImportServiceTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/TagImportControllerTest.java`

- [ ] **Step 1: Create DTOs**

`TagImportRow.java`:

```java
package org.chovy.canvas.dto;

import lombok.Data;

@Data
public class TagImportRow {
    private Integer rowNo;
    private String idType;
    private String idValue;
    private String tagCode;
    private String tagValue;
    private String tagTime;
}
```

`TagImportPushReq.java`:

```java
package org.chovy.canvas.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TagImportPushReq {
    private List<TagImportRow> rows = new ArrayList<>();
}
```

`TagImportResult.java`:

```java
package org.chovy.canvas.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TagImportResult {
    private Long batchId;
    private String status;
    private int totalRows;
    private int successRows;
    private int failedRows;
}
```

- [ ] **Step 2: Write import service tests**

Create `TagImportServiceTest.java`:

```java
package org.chovy.canvas.domain.meta;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.chovy.canvas.dto.TagImportResult;
import org.chovy.canvas.dto.TagImportRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TagImportServiceTest {
    private IdentityTypeService identityTypeService;
    private TagDefinitionService tagDefinitionService;
    private UserTagCurrentMapper userTagMapper;
    private TagImportBatchMapper batchMapper;
    private TagImportErrorMapper errorMapper;
    private TagImportService service;

    @BeforeEach
    void setUp() {
        identityTypeService = mock(IdentityTypeService.class);
        tagDefinitionService = mock(TagDefinitionService.class);
        userTagMapper = mock(UserTagCurrentMapper.class);
        batchMapper = mock(TagImportBatchMapper.class);
        errorMapper = mock(TagImportErrorMapper.class);
        service = new TagImportService(identityTypeService, tagDefinitionService, userTagMapper, batchMapper, errorMapper);
        doAnswer(invocation -> {
            TagImportBatch batch = invocation.getArgument(0);
            batch.setId(1001L);
            return 1;
        }).when(batchMapper).insert(any(TagImportBatch.class));
    }

    @Test
    void importRows_upsertsValidRows() {
        TagDefinition tag = new TagDefinition();
        tag.setTagCode("user_level");
        tag.setValueType("STRING");
        tag.setEnabled(1);
        when(tagDefinitionService.requireEnabledTagAndValidateValue("user_level", "VIP")).thenReturn(tag);

        TagImportRow row = new TagImportRow();
        row.setRowNo(1);
        row.setIdType("mobile");
        row.setIdValue("13800000000");
        row.setTagCode("user_level");
        row.setTagValue("VIP");

        TagImportResult result = service.importRows("API_PUSH", null, null, List.of(row));

        assertThat(result.getStatus()).isEqualTo("SUCCESS");
        assertThat(result.getSuccessRows()).isEqualTo(1);
        verify(identityTypeService).requireImportable("mobile");
        verify(tagDefinitionService).ensureValue("user_level", "VIP", "API_PUSH");
        verify(userTagMapper).selectOne(any(LambdaQueryWrapper.class));
        verify(userTagMapper).insert(any(UserTagCurrent.class));
    }

    @Test
    void importRows_writesErrorForDuplicateRowInSameBatch() {
        TagDefinition tag = new TagDefinition();
        tag.setTagCode("user_level");
        tag.setValueType("STRING");
        tag.setEnabled(1);
        when(tagDefinitionService.requireEnabledTagAndValidateValue("user_level", "VIP")).thenReturn(tag);

        TagImportRow first = row(1, "mobile", "13800000000", "user_level", "VIP");
        TagImportRow second = row(2, "mobile", "13800000000", "user_level", "VIP");

        TagImportResult result = service.importRows("API_PUSH", null, null, List.of(first, second));

        assertThat(result.getStatus()).isEqualTo("PARTIAL_SUCCESS");
        assertThat(result.getSuccessRows()).isEqualTo(1);
        assertThat(result.getFailedRows()).isEqualTo(1);
        ArgumentCaptor<TagImportError> captor = ArgumentCaptor.forClass(TagImportError.class);
        verify(errorMapper).insert(captor.capture());
        assertThat(captor.getValue().getErrorCode()).isEqualTo("DUPLICATE_ROW");
    }

    @Test
    void importRows_updatesExistingCurrentValue() {
        TagDefinition tag = new TagDefinition();
        tag.setTagCode("user_level");
        tag.setValueType("STRING");
        tag.setEnabled(1);
        when(tagDefinitionService.requireEnabledTagAndValidateValue("user_level", "VIP")).thenReturn(tag);
        UserTagCurrent existing = new UserTagCurrent();
        existing.setId(10L);
        when(userTagMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

        service.importRows("API_PUSH", null, null, List.of(row(1, "mobile", "13800000000", "user_level", "VIP")));

        verify(userTagMapper).update(any(UserTagCurrent.class), any(LambdaUpdateWrapper.class));
    }

    private static TagImportRow row(int rowNo, String idType, String idValue, String tagCode, String tagValue) {
        TagImportRow row = new TagImportRow();
        row.setRowNo(rowNo);
        row.setIdType(idType);
        row.setIdValue(idValue);
        row.setTagCode(tagCode);
        row.setTagValue(tagValue);
        return row;
    }
}
```

- [ ] **Step 3: Run tests and verify failure**

Run:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -f backend/pom.xml -pl canvas-engine -am -Dtest=TagImportServiceTest test
```

Expected: compilation fails because `TagImportService` does not exist.

- [ ] **Step 4: Implement `TagImportService`**

Create a service with these public methods:

```java
public TagImportResult importRows(String sourceType, String fileName, String externalUrl, List<TagImportRow> rows)
public List<TagImportBatch> listBatches()
public List<TagImportError> listErrors(Long batchId)
```

Core rules:

```java
String dedupeKey = row.getIdType() + "\u0001" + row.getIdValue() + "\u0001" + row.getTagCode();
if (!seen.add(dedupeKey)) {
    writeError(batchId, row, "DUPLICATE_ROW", "同一批次内重复的身份和标签");
    failedRows++;
    continue;
}
identityTypeService.requireImportable(row.getIdType());
tagDefinitionService.requireEnabledTagAndValidateValue(row.getTagCode(), row.getTagValue());
tagDefinitionService.ensureValue(row.getTagCode(), row.getTagValue(), sourceType);
upsertCurrentTag(batchId, sourceType, row);
```

Implement `upsertCurrentTag` with select-then-insert/update:

```java
UserTagCurrent existing = userTagCurrentMapper.selectOne(new LambdaQueryWrapper<UserTagCurrent>()
        .eq(UserTagCurrent::getIdType, row.getIdType())
        .eq(UserTagCurrent::getIdValue, row.getIdValue())
        .eq(UserTagCurrent::getTagCode, row.getTagCode())
        .last("LIMIT 1"));
if (existing == null) {
    UserTagCurrent current = new UserTagCurrent();
    current.setIdType(row.getIdType());
    current.setIdValue(row.getIdValue());
    current.setTagCode(row.getTagCode());
    current.setTagValue(row.getTagValue());
    current.setTagTime(parseTagTime(row.getTagTime()));
    current.setSourceType(sourceType);
    current.setSourceBatchId(batchId);
    current.setUpdatedAt(LocalDateTime.now());
    userTagCurrentMapper.insert(current);
} else {
    UserTagCurrent patch = new UserTagCurrent();
    patch.setTagValue(row.getTagValue());
    patch.setTagTime(parseTagTime(row.getTagTime()));
    patch.setSourceType(sourceType);
    patch.setSourceBatchId(batchId);
    patch.setUpdatedAt(LocalDateTime.now());
    userTagCurrentMapper.update(patch, new LambdaUpdateWrapper<UserTagCurrent>()
            .eq(UserTagCurrent::getId, existing.getId()));
}
```

Set batch final status:

```java
String status = failedRows == 0 ? "SUCCESS" : successRows == 0 ? "FAILED" : "PARTIAL_SUCCESS";
```

- [ ] **Step 5: Add API push controller**

Create `TagImportController.java` with:

```java
@RestController
@RequestMapping("/canvas/tag-imports")
@RequiredArgsConstructor
public class TagImportController {
    private final TagImportService tagImportService;

    @PostMapping("/api-push")
    public Mono<R<TagImportResult>> apiPush(@RequestBody TagImportPushReq req) {
        return Mono.fromCallable(() -> R.ok(tagImportService.importRows("API_PUSH", null, null, req.getRows())))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/batches")
    public Mono<R<List<TagImportBatch>>> batches() {
        return Mono.fromCallable(() -> R.ok(tagImportService.listBatches()))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/batches/{id}/errors")
    public Mono<R<List<TagImportError>>> errors(@PathVariable Long id) {
        return Mono.fromCallable(() -> R.ok(tagImportService.listErrors(id)))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
```

- [ ] **Step 6: Run tests**

Run:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -f backend/pom.xml -pl canvas-engine -am -Dtest=TagImportServiceTest test
```

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/dto/TagImport*.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/TagImportService.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/controller/TagImportController.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/meta/TagImportServiceTest.java
git commit -m "feat: add unified tag import service"
```

---

### Task 5: Excel Import And Template

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/controller/TagImportController.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/TagImportService.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/TagImportControllerTest.java`

- [ ] **Step 1: Keep security routing unchanged**

Do not modify `backend/canvas-engine/src/main/java/org/chovy/canvas/config/SecurityConfig.java` for this task. Current routing already makes `/canvas/tag-imports/**` authenticated through `.anyExchange().authenticated()`, and the frontend admin route controls page access.

- [ ] **Step 2: Write controller tests for row conversion**

Create `TagImportControllerTest.java` with a unit test for CSV-like map conversion by extracting parsing into a package-private method:

```java
package org.chovy.canvas.controller;

import org.chovy.canvas.dto.TagImportRow;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TagImportControllerTest {
    @Test
    void toImportRow_mapsTemplateColumns() {
        TagImportRow row = TagImportController.toImportRow(3, Map.of(
                "idType", "mobile",
                "idValue", "13800000000",
                "tagCode", "user_level",
                "tagValue", "VIP",
                "tagTime", "2026-05-23 10:30:00"
        ));

        assertThat(row.getRowNo()).isEqualTo(3);
        assertThat(row.getIdType()).isEqualTo("mobile");
        assertThat(row.getIdValue()).isEqualTo("13800000000");
        assertThat(row.getTagCode()).isEqualTo("user_level");
        assertThat(row.getTagValue()).isEqualTo("VIP");
        assertThat(row.getTagTime()).isEqualTo("2026-05-23 10:30:00");
    }
}
```

- [ ] **Step 3: Add Excel endpoints**

In `TagImportController`, add imports from Hutool:

```java
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelWriter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
```

Add:

```java
@GetMapping("/excel-template")
public Mono<ResponseEntity<byte[]>> excelTemplate() {
    return Mono.fromCallable(() -> {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ExcelWriter writer = ExcelUtil.getWriter(true)) {
            writer.writeHeadRow(List.of("idType", "idValue", "tagCode", "tagValue", "tagTime"));
            writer.writeRow(List.of("mobile", "13800000000", "user_level", "VIP", "2026-05-23 10:30:00"));
            writer.flush(out, true);
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header("Content-Disposition", "attachment; filename=tag-import-template.xlsx")
                .body(out.toByteArray());
    }).subscribeOn(Schedulers.boundedElastic());
}

@PostMapping(value = "/excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public Mono<R<TagImportResult>> excel(@RequestPart("file") Mono<FilePart> filePartMono) {
    return filePartMono.flatMap(filePart -> DataBufferUtils.join(filePart.content())
            .map(buffer -> {
                byte[] bytes = new byte[buffer.readableByteCount()];
                buffer.read(bytes);
                DataBufferUtils.release(buffer);
                List<TagImportRow> rows = readRows(bytes);
                return tagImportService.importRows("EXCEL_IMPORT", filePart.filename(), null, rows);
            })
            .subscribeOn(Schedulers.boundedElastic())
            .map(R::ok));
}
```

Add parsing helpers:

```java
static List<TagImportRow> readRows(byte[] bytes) {
    try (ExcelReader reader = ExcelUtil.getReader(new ByteArrayInputStream(bytes))) {
        List<Map<String, Object>> maps = reader.readAll();
        if (maps.size() > 20000) {
            throw new IllegalArgumentException("Excel 单次最多导入 20000 行");
        }
        List<TagImportRow> rows = new ArrayList<>();
        for (int i = 0; i < maps.size(); i++) {
            rows.add(toImportRow(i + 2, maps.get(i)));
        }
        return rows;
    }
}

static TagImportRow toImportRow(int rowNo, Map<String, ?> map) {
    TagImportRow row = new TagImportRow();
    row.setRowNo(rowNo);
    row.setIdType(value(map, "idType"));
    row.setIdValue(value(map, "idValue"));
    row.setTagCode(value(map, "tagCode"));
    row.setTagValue(value(map, "tagValue"));
    row.setTagTime(value(map, "tagTime"));
    return row;
}

private static String value(Map<String, ?> map, String key) {
    Object value = map.get(key);
    return value == null ? null : String.valueOf(value).trim();
}
```

- [ ] **Step 4: Run tests**

Run:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -f backend/pom.xml -pl canvas-engine -am -Dtest=TagImportControllerTest,TagImportServiceTest test
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/controller/TagImportController.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/controller/TagImportControllerTest.java
git commit -m "feat: add excel tag import"
```

---

### Task 6: External API Pull Source

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/TagImportSourceService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/controller/TagImportSourceController.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/meta/TagImportSourceServiceTest.java`

- [ ] **Step 1: Write service tests**

Create `TagImportSourceServiceTest.java`:

```java
package org.chovy.canvas.domain.meta;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dto.TagImportRow;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TagImportSourceServiceTest {
    @Test
    void mapRows_usesConfiguredFieldMapping() throws Exception {
        TagImportSource source = new TagImportSource();
        source.setFieldMapping("""
                {"idType":"identity_type","idValue":"identity_value","tagCode":"tag_code","tagValue":"tag_value","tagTime":"tag_time"}
                """);
        TagImportSourceService service = new TagImportSourceService(null, null, new ObjectMapper(), null);

        List<TagImportRow> rows = service.mapRows(source, List.of(Map.of(
                "identity_type", "mobile",
                "identity_value", "13800000000",
                "tag_code", "user_level",
                "tag_value", "VIP",
                "tag_time", "2026-05-23 10:30:00"
        )));

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getIdType()).isEqualTo("mobile");
        assertThat(rows.get(0).getIdValue()).isEqualTo("13800000000");
        assertThat(rows.get(0).getTagCode()).isEqualTo("user_level");
        assertThat(rows.get(0).getTagValue()).isEqualTo("VIP");
    }
}
```

- [ ] **Step 2: Implement source service**

Create `TagImportSourceService` with CRUD methods plus:

```java
public TagImportResult run(Long id) {
    TagImportSource source = mapper.selectById(id);
    if (source == null || !Integer.valueOf(1).equals(source.getEnabled())) {
        throw new IllegalArgumentException("标签拉取源不存在或已停用");
    }
    List<Map<String, Object>> records = fetchRecords(source);
    return tagImportService.importRows("API_PULL", null, source.getUrl(), mapRows(source, records));
}
```

Use Spring `WebClient` for `GET` and `POST`. For this first implementation, support `recordsPath = "$"` and `recordsPath = "$.data"`:

```java
JsonNode root = objectMapper.readTree(responseBody);
JsonNode recordsNode = "$.data".equals(source.getRecordsPath()) ? root.get("data") : root;
if (recordsNode == null || !recordsNode.isArray()) {
    throw new IllegalArgumentException("拉取响应 recordsPath 未指向数组");
}
```

Keep `mapRows` package-visible so the unit test can call it.

- [ ] **Step 3: Add controller**

Create `TagImportSourceController.java`:

```java
@RestController
@RequestMapping("/canvas/tag-import-sources")
@RequiredArgsConstructor
public class TagImportSourceController {
    private final TagImportSourceService service;

    @GetMapping
    public Mono<R<PageResult<TagImportSource>>> list(@RequestParam(required = false) Integer enabled) {
        return Mono.fromCallable(() -> {
            var rows = service.list(enabled);
            return R.ok(PageResult.of(rows.size(), rows));
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping
    public Mono<R<TagImportSource>> create(@RequestBody TagImportSource body) {
        return Mono.fromCallable(() -> R.ok(service.create(body))).subscribeOn(Schedulers.boundedElastic());
    }

    @PutMapping("/{id}")
    public Mono<R<Void>> update(@PathVariable Long id, @RequestBody TagImportSource body) {
        return Mono.fromRunnable(() -> service.update(id, body)).subscribeOn(Schedulers.boundedElastic()).thenReturn(R.ok());
    }

    @DeleteMapping("/{id}")
    public Mono<R<Void>> delete(@PathVariable Long id) {
        return Mono.fromRunnable(() -> service.delete(id)).subscribeOn(Schedulers.boundedElastic()).thenReturn(R.ok());
    }

    @PostMapping("/{id}/run")
    public Mono<R<TagImportResult>> run(@PathVariable Long id) {
        return Mono.fromCallable(() -> R.ok(service.run(id))).subscribeOn(Schedulers.boundedElastic());
    }
}
```

- [ ] **Step 4: Run tests**

Run:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -f backend/pom.xml -pl canvas-engine -am -Dtest=TagImportSourceServiceTest test
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/TagImportSourceService.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/controller/TagImportSourceController.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/meta/TagImportSourceServiceTest.java
git commit -m "feat: add tag import api pull sources"
```

---

### Task 7: TAGGER Node Schema And Dependent Data Sources

**Files:**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V61__tagger_tag_value_schema.sql`
- Modify: `frontend/src/components/config-panel/index.tsx`
- Test: `frontend/src/components/config-panel/dataSource.test.ts`

- [ ] **Step 1: Add node schema migration**

Create `V61__tagger_tag_value_schema.sql`:

```sql
UPDATE node_type_registry
SET config_schema = '[{"key":"mode","label":"标签模式","type":"radio","required":true,"options":[{"label":"实时触发（监听 MQ 事件）","value":"realtime"},{"label":"离线打标（流程内执行）","value":"offline"},{"label":"人群圈选","value":"audience"}]},{"key":"tagCodeKey","label":"标签","type":"select","dataSource":"/meta/tagger-tags","required":true,"showWhen":"mode!=audience"},{"key":"tagValue","label":"标签值","type":"select","dataSource":"/meta/tagger-tag-values?tagCode={tagCodeKey}","required":false,"showWhen":"mode==offline"},{"key":"audienceId","label":"人群","type":"select","dataSource":"/canvas/audiences/ready","required":true,"showWhen":"mode==audience"},{"key":"hitNextNodeId","label":"命中分支","type":"edge-hint","showWhen":"mode==audience","icon":"check"},{"key":"missNextNodeId","label":"未命中分支","type":"edge-hint","showWhen":"mode==audience","icon":"close"}]'
WHERE type_key = 'TAGGER';
```

- [ ] **Step 2: Extract data source resolver for tests**

In `frontend/src/components/config-panel/index.tsx`, export helpers:

```ts
export function resolveDataSourceTemplate(src: string, values: Record<string, unknown>): string | null {
  const missing = src.match(/\{(\w+)\}/g)?.find(token => {
    const key = token.slice(1, -1)
    return values[key] == null || values[key] === ''
  })
  if (missing) return null
  return src.replace(/\{(\w+)\}/g, (_, key) => encodeURIComponent(String(values[key])))
}

export function getDataSourceDependencies(src?: string): string[] {
  if (!src) return []
  return Array.from(src.matchAll(/\{(\w+)\}/g)).map(match => match[1])
}
```

- [ ] **Step 3: Write Vitest coverage**

Create `frontend/src/components/config-panel/dataSource.test.ts`:

```ts
import { describe, expect, it } from 'vitest'
import { getDataSourceDependencies, resolveDataSourceTemplate } from './index'

describe('config panel data source templates', () => {
  it('resolves field placeholders from form values', () => {
    expect(resolveDataSourceTemplate('/meta/tagger-tag-values?tagCode={tagCodeKey}', {
      tagCodeKey: 'user_level',
    })).toBe('/meta/tagger-tag-values?tagCode=user_level')
  })

  it('returns null when dependency value is missing', () => {
    expect(resolveDataSourceTemplate('/meta/tagger-tag-values?tagCode={tagCodeKey}', {})).toBeNull()
  })

  it('extracts dependency field keys', () => {
    expect(getDataSourceDependencies('/meta/tagger-tag-values?tagCode={tagCodeKey}')).toEqual(['tagCodeKey'])
  })
})
```

- [ ] **Step 4: Update option loading effect**

In the select option loading effect, replace direct `const src = f.dataSource!` with:

```ts
const resolvedSrc = resolveDataSourceTemplate(f.dataSource!, formValues)
if (!resolvedSrc) {
  setOptions(prev => ({ ...prev, [f.key]: [] }))
  return
}
if (rawCache.has(resolvedSrc)) {
  setOptions(prev => ({ ...prev, [f.key]: toSelectOptions(rawCache.get(resolvedSrc)!) }))
  return
}
loadDataSource(resolvedSrc).then(data =>
  setOptions(prev => ({ ...prev, [f.key]: toSelectOptions(data) }))
)
```

Make the effect depend on `schema` and `formValues`.

- [ ] **Step 5: Clear dependent field when parent changes**

Inside `handleValuesChange`, after `setFormValues(all)`, add:

```ts
const fields = parseSchema(schema?.configSchema)
for (const field of fields) {
  const deps = getDataSourceDependencies(field.dataSource)
  if (deps.some(dep => Object.prototype.hasOwnProperty.call(_changed, dep))) {
    form.setFieldValue(field.key, undefined)
  }
}
```

Keep this before calling `onChange` so the saved config does not keep stale `tagValue`.

- [ ] **Step 6: Run frontend tests**

Run:

```bash
npm --prefix frontend test -- dataSource.test.ts
```

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add backend/canvas-engine/src/main/resources/db/migration/V61__tagger_tag_value_schema.sql \
  frontend/src/components/config-panel/index.tsx \
  frontend/src/components/config-panel/dataSource.test.ts
git commit -m "feat: support tag value data source dependencies"
```

---

### Task 8: Frontend API Types And Routes

**Files:**
- Modify: `frontend/src/types/index.ts`
- Modify: `frontend/src/services/api.ts`
- Modify: `frontend/src/App.tsx`
- Modify: `frontend/src/components/layout/AppLayout.tsx`

- [ ] **Step 1: Add frontend types**

Append to `frontend/src/types/index.ts`:

```ts
export interface IdentityType {
  id: number
  code: string
  name: string
  description?: string
  enabled: 0 | 1
  allowImport: 0 | 1
  multiValue: 0 | 1
  priority: number
  participateMapping: 0 | 1
  createdAt?: string
  updatedAt?: string
}

export interface TagDefinition {
  id: number
  name: string
  tagCode: string
  tagType: 'offline' | 'realtime'
  valueType: 'STRING' | 'NUMBER' | 'BOOLEAN'
  description?: string
  enabled: 0 | 1
}

export interface TagValueDefinition {
  id: number
  tagCode: string
  value: string
  label: string
  sortOrder: number
  enabled: 0 | 1
  source: string
  description?: string
}

export interface TagImportRow {
  rowNo?: number
  idType: string
  idValue: string
  tagCode: string
  tagValue: string
  tagTime?: string
}

export interface TagImportResult {
  batchId: number
  status: string
  totalRows: number
  successRows: number
  failedRows: number
}
```

- [ ] **Step 2: Add API clients**

In `frontend/src/services/api.ts`, import the new types and add:

```ts
export const identityTypeApi = {
  list: (params?: { enabled?: number; allowImport?: number }) =>
    http.get<R<PageResult<IdentityType>>, R<PageResult<IdentityType>>>('/canvas/identity-types', { params }),
  create: (body: Partial<IdentityType>) =>
    http.post<R<IdentityType>, R<IdentityType>>('/canvas/identity-types', body),
  update: (id: number, body: Partial<IdentityType>) =>
    http.put<R<void>, R<void>>(`/canvas/identity-types/${id}`, body),
  delete: (id: number) =>
    http.delete<R<void>, R<void>>(`/canvas/identity-types/${id}`),
}

export const tagValueApi = {
  list: (tagCode: string, params?: { enabled?: number }) =>
    http.get<R<TagValueDefinition[]>, R<TagValueDefinition[]>>(`/canvas/tag-definitions/${tagCode}/values`, { params }),
  create: (tagCode: string, body: Partial<TagValueDefinition>) =>
    http.post<R<TagValueDefinition>, R<TagValueDefinition>>(`/canvas/tag-definitions/${tagCode}/values`, body),
  update: (id: number, body: Partial<TagValueDefinition>) =>
    http.put<R<void>, R<void>>(`/canvas/tag-definitions/values/${id}`, body),
  delete: (id: number) =>
    http.delete<R<void>, R<void>>(`/canvas/tag-definitions/values/${id}`),
}

export const tagImportApi = {
  apiPush: (rows: TagImportRow[]) =>
    http.post<R<TagImportResult>, R<TagImportResult>>('/canvas/tag-imports/api-push', { rows }),
  batches: () =>
    http.get<R<any[]>, R<any[]>>('/canvas/tag-imports/batches'),
  errors: (batchId: number) =>
    http.get<R<any[]>, R<any[]>>(`/canvas/tag-imports/batches/${batchId}/errors`),
  excelTemplateUrl: '/canvas/tag-imports/excel-template',
  uploadExcel: (file: File) => {
    const form = new FormData()
    form.append('file', file)
    return http.post<R<TagImportResult>, R<TagImportResult>>('/canvas/tag-imports/excel', form)
  },
}
```

- [ ] **Step 3: Add routes**

In `frontend/src/App.tsx`, import:

```ts
import IdentityTypesPage from './pages/identity-types'
import TagImportPage from './pages/tag-import'
```

Add admin routes:

```tsx
<Route path="/identity-types" element={<IdentityTypesPage />} />
<Route path="/tag-import" element={<TagImportPage />} />
```

- [ ] **Step 4: Add navigation**

In `AppLayout.tsx`, add selected keys:

```ts
if (location.pathname.startsWith('/identity-types')) return 'identity-types'
if (location.pathname.startsWith('/tag-import')) return 'tag-import'
```

Add menu items under settings:

```tsx
{
  key: 'identity-types',
  icon: <UserOutlined />,
  label: 'ID 类型配置',
  onClick: () => navigate('/identity-types'),
},
{
  key: 'tag-import',
  icon: <TagsOutlined />,
  label: '标签导入',
  onClick: () => navigate('/tag-import'),
},
```

- [ ] **Step 5: Type check**

Run:

```bash
npm --prefix frontend run build
```

Expected: TypeScript compile succeeds.

- [ ] **Step 6: Commit**

```bash
git add frontend/src/types/index.ts frontend/src/services/api.ts frontend/src/App.tsx frontend/src/components/layout/AppLayout.tsx
git commit -m "feat: add tag center frontend routes"
```

---

### Task 9: ID Type Configuration Page

**Files:**
- Create: `frontend/src/pages/identity-types/index.tsx`

- [ ] **Step 1: Create page**

Create `frontend/src/pages/identity-types/index.tsx` with an Ant Design table and modal following `frontend/src/pages/tag-config/index.tsx` style. Required behavior:

```tsx
const columns: ColumnsType<IdentityType> = [
  { title: '编码', dataIndex: 'code' },
  { title: '名称', dataIndex: 'name' },
  { title: '优先级', dataIndex: 'priority', width: 90 },
  { title: '状态', dataIndex: 'enabled', render: v => <Tag color={v === 1 ? 'green' : 'default'}>{v === 1 ? '启用' : '禁用'}</Tag> },
  { title: '允许导入', dataIndex: 'allowImport', render: v => <Tag color={v === 1 ? 'blue' : 'default'}>{v === 1 ? '允许' : '禁止'}</Tag> },
  { title: '取值', dataIndex: 'multiValue', render: v => v === 1 ? '多值' : '单值' },
  { title: '参与映射', dataIndex: 'participateMapping', render: v => v === 1 ? '是' : '否' },
]
```

Form fields:

```tsx
<Form.Item name="code" label="编码" rules={[{ required: true }]}>
  <Input placeholder="如：mobile" disabled={!!editing} />
</Form.Item>
<Form.Item name="name" label="名称" rules={[{ required: true }]}>
  <Input placeholder="如：手机号" />
</Form.Item>
<Form.Item name="priority" label="优先级" initialValue={100}>
  <InputNumber style={{ width: '100%' }} min={1} max={9999} />
</Form.Item>
<Form.Item name="description" label="说明">
  <Input.TextArea rows={2} />
</Form.Item>
<Form.Item name="enabled" label="启用" valuePropName="checked"><Switch /></Form.Item>
<Form.Item name="allowImport" label="允许导入" valuePropName="checked"><Switch /></Form.Item>
<Form.Item name="multiValue" label="多值 ID" valuePropName="checked"><Switch /></Form.Item>
<Form.Item name="participateMapping" label="参与映射" valuePropName="checked"><Switch /></Form.Item>
```

Map switches to `1/0` before saving.

- [ ] **Step 2: Build**

Run:

```bash
npm --prefix frontend run build
```

Expected: build succeeds.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/pages/identity-types/index.tsx
git commit -m "feat: add identity type configuration page"
```

---

### Task 10: Tag Center Page With Value Editor

**Files:**
- Modify: `frontend/src/pages/tag-config/index.tsx`
- Create: `frontend/src/pages/tag-config/tagTypes.ts`
- Create: `frontend/src/pages/tag-config/tagValueEditor.tsx`
- Test: `frontend/src/pages/tag-config/tagValueEditor.test.ts`

- [ ] **Step 1: Add local types**

Create `tagTypes.ts`:

```ts
import type { TagDefinition, TagValueDefinition } from '../../types'

export type TagFormValues = Omit<TagDefinition, 'id' | 'enabled'> & { enabled: boolean }
export type TagValueFormValues = Omit<TagValueDefinition, 'id' | 'enabled'> & { enabled: boolean }
```

- [ ] **Step 2: Create value editor component**

Create `tagValueEditor.tsx`. Props:

```ts
interface Props {
  tagCode: string | null
}
```

Behavior:
- Load values with `tagValueApi.list(tagCode)` when `tagCode` is set.
- Show table columns: value, label, sortOrder, source, enabled, operation.
- Modal supports create/update value.
- Delete action calls `tagValueApi.delete(id)`.

Use stable form conversion:

```ts
const toBody = (values: any) => ({
  ...values,
  enabled: values.enabled ? 1 : 0,
  sortOrder: values.sortOrder ?? 0,
  source: values.source || 'MANUAL',
})
```

- [ ] **Step 3: Add component test**

Create `tagValueEditor.test.ts` for pure conversion by exporting `toTagValueBody` from the component:

```ts
import { describe, expect, it } from 'vitest'
import { toTagValueBody } from './tagValueEditor'

describe('toTagValueBody', () => {
  it('normalizes switch and defaults', () => {
    expect(toTagValueBody({ value: 'VIP', label: 'VIP', enabled: true })).toMatchObject({
      value: 'VIP',
      label: 'VIP',
      enabled: 1,
      sortOrder: 0,
      source: 'MANUAL',
    })
  })
})
```

- [ ] **Step 4: Upgrade tag page**

In `index.tsx`, add `valueType` to the `TagDef` interface, add value type column, and change modal title to “标签中心”. In the edit modal or drawer, render:

```tsx
{editing?.tagCode && (
  <>
    <Divider />
    <TagValueEditor tagCode={editing.tagCode} />
  </>
)}
```

Add `valueType` form:

```tsx
<Form.Item name="valueType" label="标签值类型" rules={[{ required: true }]}>
  <Select options={[
    { value: 'STRING', label: '文本' },
    { value: 'NUMBER', label: '数字' },
    { value: 'BOOLEAN', label: '布尔' },
  ]} />
</Form.Item>
```

- [ ] **Step 5: Run tests and build**

Run:

```bash
npm --prefix frontend test -- tagValueEditor.test.ts
npm --prefix frontend run build
```

Expected: both pass.

- [ ] **Step 6: Commit**

```bash
git add frontend/src/pages/tag-config
git commit -m "feat: add tag value editor"
```

---

### Task 11: Tag Import Page

**Files:**
- Create: `frontend/src/pages/tag-import/index.tsx`
- Create: `frontend/src/pages/tag-import/tagImportTypes.ts`
- Create: `frontend/src/pages/tag-import/tagImportApiDoc.tsx`
- Create: `frontend/src/pages/tag-import/tagImportBatchList.tsx`
- Create: `frontend/src/pages/tag-import/tagImportSourcePanel.tsx`
- Create: `frontend/src/pages/tag-import/tagImportExcelPanel.tsx`

- [ ] **Step 1: Create API doc tab**

`tagImportApiDoc.tsx` displays:

```tsx
const sample = `{
  "rows": [
    {
      "idType": "mobile",
      "idValue": "13800000000",
      "tagCode": "user_level",
      "tagValue": "VIP",
      "tagTime": "2026-05-23 10:30:00"
    }
  ]
}`
```

Show field table:

```tsx
[
  ['idType', '身份类型编码，来自 ID 类型配置'],
  ['idValue', '身份值，如手机号、OpenID、会员号'],
  ['tagCode', '标签编码，来自标签中心'],
  ['tagValue', '标签值，会自动补齐到标签值字典'],
  ['tagTime', '标签时间，可选，格式 YYYY-MM-DD HH:mm:ss'],
]
```

- [ ] **Step 2: Create Excel panel**

`tagImportExcelPanel.tsx` uses `Upload` with `beforeUpload` to call `tagImportApi.uploadExcel(file)` and returns `false`. Add template button:

```tsx
<Button onClick={() => window.open(tagImportApi.excelTemplateUrl)}>下载模板</Button>
```

After upload, show `TagImportResult` with `Alert`.

- [ ] **Step 3: Create batch list**

`tagImportBatchList.tsx` loads `tagImportApi.batches()` and shows columns:
- ID
- sourceType
- status
- totalRows
- successRows
- failedRows
- createdAt

Add expandable row to load errors with `tagImportApi.errors(batch.id)`.

- [ ] **Step 4: Create source panel**

`tagImportSourcePanel.tsx` initially supports list/create/edit/run through a simple table and modal. Add fields:

```tsx
name, url, method, recordsPath, fieldMapping, enabled
```

Set default `fieldMapping`:

```json
{"idType":"identity_type","idValue":"identity_value","tagCode":"tag_code","tagValue":"tag_value","tagTime":"tag_time"}
```

Add missing service methods in `tagImportApi`:

```ts
sources: () => http.get<R<PageResult<any>>, R<PageResult<any>>>('/canvas/tag-import-sources'),
createSource: (body: any) => http.post<R<any>, R<any>>('/canvas/tag-import-sources', body),
updateSource: (id: number, body: any) => http.put<R<void>, R<void>>(`/canvas/tag-import-sources/${id}`, body),
deleteSource: (id: number) => http.delete<R<void>, R<void>>(`/canvas/tag-import-sources/${id}`),
runSource: (id: number) => http.post<R<TagImportResult>, R<TagImportResult>>(`/canvas/tag-import-sources/${id}/run`),
```

- [ ] **Step 5: Compose page**

`index.tsx`:

```tsx
export default function TagImportPage() {
  return (
    <div>
      <Typography.Title level={4} style={{ marginTop: 0 }}>标签导入</Typography.Title>
      <Tabs items={[
        { key: 'api', label: 'API 推送', children: <TagImportApiDoc /> },
        { key: 'excel', label: 'Excel 导入', children: <TagImportExcelPanel /> },
        { key: 'pull', label: 'API 拉取', children: <TagImportSourcePanel /> },
        { key: 'batches', label: '导入批次', children: <TagImportBatchList /> },
      ]} />
    </div>
  )
}
```

- [ ] **Step 6: Build**

Run:

```bash
npm --prefix frontend run build
```

Expected: build succeeds.

- [ ] **Step 7: Commit**

```bash
git add frontend/src/pages/tag-import frontend/src/services/api.ts
git commit -m "feat: add tag import management page"
```

---

### Task 12: Final Verification

**Files:**
- All files changed by previous tasks.

- [ ] **Step 1: Run backend unit tests**

Run:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -f backend/pom.xml -pl canvas-engine -am test
```

Expected: all backend tests pass.

- [ ] **Step 2: Run frontend tests**

Run:

```bash
npm --prefix frontend test
```

Expected: all frontend tests pass.

- [ ] **Step 3: Run frontend build**

Run:

```bash
npm --prefix frontend run build
```

Expected: TypeScript and Vite build pass.

- [ ] **Step 4: Run full compile**

Run:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -f backend/pom.xml -pl canvas-engine -am -DskipTests compile
```

Expected: backend modules compile.

- [ ] **Step 5: Inspect git diff**

Run:

```bash
git status --short
git diff --stat HEAD
```

Expected: only intended files are modified or untracked.

- [ ] **Step 6: Commit verification fixes**

If verification required fixes, commit them:

```bash
git add backend frontend
git commit -m "test: verify tag center import flow"
```

If no fixes were needed, do not create an empty commit.

## Self-Review Notes

Spec coverage:
- ID 类型独立配置: Task 1, Task 2, Task 8, Task 9.
- 标签值字典: Task 1, Task 3, Task 10.
- API 推送导入: Task 4, Task 11.
- Excel 导入: Task 5, Task 11.
- 外部 API 拉取: Task 6, Task 11.
- 画布节点标签值联动: Task 7.
- 批次与错误行: Task 4, Task 5, Task 6, Task 11.

Type consistency:
- Backend DTOs use `idType`, `idValue`, `tagCode`, `tagValue`, `tagTime`.
- Frontend API clients use the same field names.
- `TAGGER` node schema uses existing `tagCodeKey` and new `tagValue`.

Execution risk:
- `MetaController` constructor injection will need a careful edit because it already has multiple dependencies.
- `ConfigPanel` option-loading effect currently caches by raw URL; Task 7 changes cache keys to resolved URLs.
- Excel upload on WebFlux needs `FilePart` and `DataBufferUtils`; keep parsing inside `boundedElastic`.
