# Canvas Example Library Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a switchable official canvas example library that stores templates in `canvas_template`, imports them into normal draft canvases by default, and documents component usage and marketing scenarios.

**Architecture:** Reuse the existing `canvas_template` table as the canonical template store, extend `canvas` with example source metadata, and add a startup seeder that materializes enabled official templates into `canvas` plus draft `canvas_version` records. The frontend keeps rendering normal canvases through the existing editor; light UI changes only add example labels and fix the IF negative branch handle mismatch.

**Tech Stack:** Java 21, Spring Boot WebFlux, MyBatis-Plus, Flyway SQL migrations, Jackson, JUnit 5, Mockito, AssertJ, React, TypeScript, Ant Design, Vitest.

---

## Preflight

The current working tree contains unresolved merge conflicts and unrelated edits. Before executing this plan, create or switch to a clean isolated worktree, or resolve the existing conflicts first. Do not mix this implementation with the current conflicted files.

Recommended execution command once a clean worktree exists:

```bash
git status --short
```

Expected: no `UU` or `AA` entries before starting task work.

## File Structure

Backend schema and models:
- Create: `backend/canvas-engine/src/main/resources/db/migration/V54__canvas_example_library_schema.sql`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/Canvas.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasTemplate.java`

Backend example import:
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasExamplesProperties.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasExampleSeeder.java`
- Modify: `backend/canvas-engine/src/main/resources/application.yml`

Backend service behavior:
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasService.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasOpsService.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/controller/OpsController.java`

Backend template data:
- Create: `backend/canvas-engine/scripts/generate-canvas-example-sql.mjs`
- Create: `backend/canvas-engine/src/main/resources/db/migration/V55__canvas_example_templates.sql`

Backend tests:
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasExampleLibrarySchemaTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasExampleSeederTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasServiceExampleFilterTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasOpsServiceExampleCloneTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/OpsControllerTemplateTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasExampleTemplateMigrationTest.java`
- Modify existing constructor setup in `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasServicePublishTest.java`
- Modify existing `@InjectMocks` test setup in `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasServiceArchiveTest.java`

Frontend:
- Modify: `frontend/src/components/canvas/branchHandles.ts`
- Modify: `frontend/src/components/canvas/branchHandles.test.ts`
- Modify: `frontend/src/types/index.ts`
- Modify: `frontend/src/pages/canvas-list/index.tsx`

Docs:
- Create: `docs/canvas-examples/README.md`
- Create: `docs/canvas-examples/components.md`
- Create: `docs/canvas-examples/combinations.md`
- Create: `docs/canvas-examples/scenarios.md`

---

### Task 1: Fix IF Branch Handle Compatibility

**Files:**
- Modify: `frontend/src/components/canvas/branchHandles.ts`
- Modify: `frontend/src/components/canvas/branchHandles.test.ts`

- [ ] **Step 1: Write the failing Vitest expectation**

Change the IF test in `frontend/src/components/canvas/branchHandles.test.ts` to expect `fail`, matching `deriveEdges`, `patchBizConfig`, and publish validation:

```ts
it('IF_CONDITION returns fixed success+fail handles', () => {
  const handles = getBranchHandles('IF_CONDITION', {})
  expect(handles).toHaveLength(2)
  expect(handles[0]).toEqual({ id: 'success', label: '条件成立', color: '#52c41a' })
  expect(handles[1]).toEqual({ id: 'fail', label: '否则', color: '#8c8c8c' })
})
```

- [ ] **Step 2: Run the frontend test to verify it fails**

```bash
cd frontend
npm test -- branchHandles.test.ts
```

Expected: FAIL because the current negative IF handle id is `else`.

- [ ] **Step 3: Implement the handle change**

In `frontend/src/components/canvas/branchHandles.ts`, change the `IF_CONDITION` branch:

```ts
case 'IF_CONDITION':
  return [
    { id: 'success', label: '条件成立', color: '#52c41a' },
    { id: 'fail',    label: '否则',     color: '#8c8c8c' },
  ]
```

- [ ] **Step 4: Run the frontend test to verify it passes**

```bash
cd frontend
npm test -- branchHandles.test.ts
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/components/canvas/branchHandles.ts frontend/src/components/canvas/branchHandles.test.ts
git commit -m "fix: align if branch handle with fail route"
```

---

### Task 2: Add Example Schema Migration and Entity Fields

**Files:**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V54__canvas_example_library_schema.sql`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/Canvas.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasTemplate.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasExampleLibrarySchemaTest.java`

- [ ] **Step 1: Write the schema migration test**

Create `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasExampleLibrarySchemaTest.java`:

```java
package org.chovy.canvas.domain.canvas;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class CanvasExampleLibrarySchemaTest {

    @Test
    void migrationAddsTemplateAndCanvasExampleColumns() throws Exception {
        ClassPathResource migration =
                new ClassPathResource("db/migration/V54__canvas_example_library_schema.sql");

        String sql = migration.getContentAsString(StandardCharsets.UTF_8);

        assertThat(sql)
                .contains("ALTER TABLE canvas_template")
                .contains("template_key")
                .contains("company_type")
                .contains("marketing_scenario")
                .contains("covered_node_types")
                .contains("uk_canvas_template_key")
                .contains("ALTER TABLE canvas")
                .contains("is_example")
                .contains("source_template_key")
                .contains("idx_example_template");
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

```bash
cd backend/canvas-engine
mvn -Dtest=CanvasExampleLibrarySchemaTest test
```

Expected: FAIL because `V54__canvas_example_library_schema.sql` does not exist.

- [ ] **Step 3: Add the migration**

Create `backend/canvas-engine/src/main/resources/db/migration/V54__canvas_example_library_schema.sql`:

```sql
-- V54: official canvas example library metadata.

ALTER TABLE canvas_template
  ADD COLUMN template_key VARCHAR(100) NULL COMMENT '官方模板稳定唯一键',
  ADD COLUMN company_type VARCHAR(50) NULL COMMENT '公司类型',
  ADD COLUMN marketing_scenario VARCHAR(50) NULL COMMENT '营销场景',
  ADD COLUMN difficulty VARCHAR(20) NULL COMMENT '入门/进阶/复杂',
  ADD COLUMN covered_node_types VARCHAR(1000) NULL COMMENT '覆盖的节点类型，逗号分隔',
  ADD COLUMN sort_order INT NOT NULL DEFAULT 0 COMMENT '官方模板排序',
  ADD COLUMN enabled TINYINT NOT NULL DEFAULT 1 COMMENT '1=模板可用',
  ADD UNIQUE KEY uk_canvas_template_key (template_key);

ALTER TABLE canvas
  ADD COLUMN is_example TINYINT NOT NULL DEFAULT 0 COMMENT '1=官方示例画布',
  ADD COLUMN source_template_key VARCHAR(100) NULL COMMENT '来源官方模板 key',
  ADD INDEX idx_example_template (is_example, source_template_key);

UPDATE canvas
SET is_example = 1
WHERE name LIKE '示例：%'
  AND created_by = 'system';
```

- [ ] **Step 4: Add entity fields**

In `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasTemplate.java`, add fields after `category`:

```java
private String templateKey;
private String companyType;
private String marketingScenario;
private String difficulty;
private String coveredNodeTypes;
private Integer sortOrder;
private Integer enabled;
```

In `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/Canvas.java`, add fields after `createdBy`:

```java
/** 1=官方示例画布，受 canvas.examples.enabled 控制展示 */
private Integer isExample;

/** 来源官方模板 key，用于启动导入幂等判断 */
private String sourceTemplateKey;
```

- [ ] **Step 5: Run the schema test**

```bash
cd backend/canvas-engine
mvn -Dtest=CanvasExampleLibrarySchemaTest test
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add backend/canvas-engine/src/main/resources/db/migration/V54__canvas_example_library_schema.sql \
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/Canvas.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasTemplate.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasExampleLibrarySchemaTest.java
git commit -m "feat: add canvas example metadata schema"
```

---

### Task 3: Add Example Configuration Properties

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasExamplesProperties.java`
- Modify: `backend/canvas-engine/src/main/resources/application.yml`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasExamplesPropertiesTest.java`

- [ ] **Step 1: Write the property default test**

Create `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasExamplesPropertiesTest.java`:

```java
package org.chovy.canvas.domain.canvas;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CanvasExamplesPropertiesTest {

    @Test
    void examplesAreEnabledByDefault() {
        CanvasExamplesProperties properties = new CanvasExamplesProperties();

        assertThat(properties.isEnabled()).isTrue();
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

```bash
cd backend/canvas-engine
mvn -Dtest=CanvasExamplesPropertiesTest test
```

Expected: FAIL because `CanvasExamplesProperties` does not exist.

- [ ] **Step 3: Add the properties class**

Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasExamplesProperties.java`:

```java
package org.chovy.canvas.domain.canvas;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "canvas.examples")
public class CanvasExamplesProperties {

    /**
     * true: import and show official example canvases.
     * false: skip import and hide example canvases from normal list results.
     */
    private boolean enabled = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
```

- [ ] **Step 4: Register the configuration and default value**

In `backend/canvas-engine/src/main/java/org/chovy/canvas/CanvasEngineApplication.java`, add configuration properties registration:

```java
import org.chovy.canvas.domain.canvas.CanvasExamplesProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@EnableConfigurationProperties(CanvasExamplesProperties.class)
```

In `backend/canvas-engine/src/main/resources/application.yml`, add under `canvas:`:

```yaml
  examples:
    enabled: ${CANVAS_EXAMPLES_ENABLED:true}
```

- [ ] **Step 5: Run the property test**

```bash
cd backend/canvas-engine
mvn -Dtest=CanvasExamplesPropertiesTest test
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasExamplesProperties.java \
  backend/canvas-engine/src/main/resources/application.yml \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasExamplesPropertiesTest.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/CanvasEngineApplication.java
git commit -m "feat: add canvas example toggle"
```

---

### Task 4: Implement Startup Importer

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasExampleSeeder.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasExampleSeederTest.java`

- [ ] **Step 1: Write seeder tests**

Create `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasExampleSeederTest.java`:

```java
package org.chovy.canvas.domain.canvas;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.domain.constant.CanvasStatusEnum;
import org.chovy.canvas.domain.constant.VersionStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CanvasExampleSeederTest {

    @Mock CanvasTemplateMapper templateMapper;
    @Mock CanvasMapper canvasMapper;
    @Mock CanvasVersionMapper canvasVersionMapper;

    @Test
    void disabledToggleSkipsImport() throws Exception {
        CanvasExamplesProperties properties = new CanvasExamplesProperties();
        properties.setEnabled(false);
        CanvasExampleSeeder seeder = new CanvasExampleSeeder(
                templateMapper, canvasMapper, canvasVersionMapper, new ObjectMapper(), properties);

        seeder.run(null);

        verifyNoInteractions(templateMapper, canvasMapper, canvasVersionMapper);
    }

    @Test
    void importsMissingOfficialTemplateAsDraftCanvas() throws Exception {
        CanvasExamplesProperties properties = new CanvasExamplesProperties();
        CanvasTemplate template = template("component_event_if_coupon", "{\"nodes\":[]}");
        when(templateMapper.selectList(any())).thenReturn(List.of(template));
        when(canvasMapper.selectOne(any())).thenReturn(null);
        doAnswer(invocation -> {
            Canvas canvas = invocation.getArgument(0);
            canvas.setId(99L);
            return 1;
        }).when(canvasMapper).insert(any(Canvas.class));

        CanvasExampleSeeder seeder = new CanvasExampleSeeder(
                templateMapper, canvasMapper, canvasVersionMapper, new ObjectMapper(), properties);

        seeder.run(null);

        ArgumentCaptor<Canvas> canvasCaptor = ArgumentCaptor.forClass(Canvas.class);
        verify(canvasMapper).insert(canvasCaptor.capture());
        Canvas inserted = canvasCaptor.getValue();
        assertThat(inserted.getName()).isEqualTo("示例：事件触发新客领券");
        assertThat(inserted.getStatus()).isEqualTo(CanvasStatusEnum.DRAFT.getCode());
        assertThat(inserted.getCreatedBy()).isEqualTo("example-seed");
        assertThat(inserted.getIsExample()).isEqualTo(1);
        assertThat(inserted.getSourceTemplateKey()).isEqualTo("component_event_if_coupon");

        ArgumentCaptor<CanvasVersion> versionCaptor = ArgumentCaptor.forClass(CanvasVersion.class);
        verify(canvasVersionMapper).insert(versionCaptor.capture());
        CanvasVersion version = versionCaptor.getValue();
        assertThat(version.getCanvasId()).isEqualTo(99L);
        assertThat(version.getVersion()).isEqualTo(1);
        assertThat(version.getStatus()).isEqualTo(VersionStatus.DRAFT.getCode());
        assertThat(version.getGraphJson()).isEqualTo("{\"nodes\":[]}");
        assertThat(version.getCreatedBy()).isEqualTo("example-seed");
    }

    @Test
    void existingImportedCanvasIsNotDuplicated() throws Exception {
        CanvasExamplesProperties properties = new CanvasExamplesProperties();
        when(templateMapper.selectList(any())).thenReturn(List.of(template("component_event_if_coupon", "{\"nodes\":[]}")));
        when(canvasMapper.selectOne(any())).thenReturn(new Canvas());
        CanvasExampleSeeder seeder = new CanvasExampleSeeder(
                templateMapper, canvasMapper, canvasVersionMapper, new ObjectMapper(), properties);

        seeder.run(null);

        verify(canvasMapper, never()).insert(any(Canvas.class));
        verify(canvasVersionMapper, never()).insert(any(CanvasVersion.class));
    }

    @Test
    void invalidTemplateGraphIsSkipped() throws Exception {
        CanvasExamplesProperties properties = new CanvasExamplesProperties();
        when(templateMapper.selectList(any())).thenReturn(List.of(template("bad_graph", "{")));
        CanvasExampleSeeder seeder = new CanvasExampleSeeder(
                templateMapper, canvasMapper, canvasVersionMapper, new ObjectMapper(), properties);

        seeder.run(null);

        verify(canvasMapper, never()).insert(any(Canvas.class));
        verify(canvasVersionMapper, never()).insert(any(CanvasVersion.class));
    }

    private static CanvasTemplate template(String key, String graphJson) {
        CanvasTemplate template = new CanvasTemplate();
        template.setTemplateKey(key);
        template.setName("示例：事件触发新客领券");
        template.setDescription("事件触发后判断新客并发券");
        template.setGraphJson(graphJson);
        template.setIsOfficial(1);
        template.setEnabled(1);
        return template;
    }
}
```

- [ ] **Step 2: Run the tests to verify they fail**

```bash
cd backend/canvas-engine
mvn -Dtest=CanvasExampleSeederTest test
```

Expected: FAIL because `CanvasExampleSeeder` does not exist.

- [ ] **Step 3: Add the seeder**

Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasExampleSeeder.java`:

```java
package org.chovy.canvas.domain.canvas;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.domain.constant.CanvasStatusEnum;
import org.chovy.canvas.domain.constant.VersionStatus;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CanvasExampleSeeder implements ApplicationRunner {

    static final String CREATED_BY = "example-seed";

    private final CanvasTemplateMapper templateMapper;
    private final CanvasMapper canvasMapper;
    private final CanvasVersionMapper canvasVersionMapper;
    private final ObjectMapper objectMapper;
    private final CanvasExamplesProperties properties;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        if (!properties.isEnabled()) {
            log.info("Official canvas examples are disabled; skip import.");
            return;
        }

        List<CanvasTemplate> templates = templateMapper.selectList(
                new LambdaQueryWrapper<CanvasTemplate>()
                        .eq(CanvasTemplate::getIsOfficial, 1)
                        .eq(CanvasTemplate::getEnabled, 1)
                        .isNotNull(CanvasTemplate::getTemplateKey)
                        .orderByAsc(CanvasTemplate::getSortOrder)
        );

        for (CanvasTemplate template : templates) {
            importTemplate(template);
        }
    }

    private void importTemplate(CanvasTemplate template) {
        if (!isValidGraph(template)) {
            log.warn("Skip official canvas example template {} because graph_json is invalid.",
                    template.getTemplateKey());
            return;
        }

        Canvas existing = canvasMapper.selectOne(
                new LambdaQueryWrapper<Canvas>()
                        .eq(Canvas::getSourceTemplateKey, template.getTemplateKey())
                        .last("LIMIT 1")
        );
        if (existing != null) {
            return;
        }

        Canvas canvas = new Canvas();
        canvas.setName(template.getName());
        canvas.setDescription(template.getDescription());
        canvas.setStatus(CanvasStatusEnum.DRAFT.getCode());
        canvas.setCreatedBy(CREATED_BY);
        canvas.setIsExample(1);
        canvas.setSourceTemplateKey(template.getTemplateKey());
        canvasMapper.insert(canvas);

        CanvasVersion version = new CanvasVersion();
        version.setCanvasId(canvas.getId());
        version.setVersion(1);
        version.setGraphJson(template.getGraphJson());
        version.setStatus(VersionStatus.DRAFT.getCode());
        version.setCreatedBy(CREATED_BY);
        canvasVersionMapper.insert(version);
    }

    private boolean isValidGraph(CanvasTemplate template) {
        try {
            objectMapper.readTree(template.getGraphJson());
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
```

- [ ] **Step 4: Run the seeder tests**

```bash
cd backend/canvas-engine
mvn -Dtest=CanvasExampleSeederTest test
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasExampleSeeder.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasExampleSeederTest.java
git commit -m "feat: import official canvas examples"
```

---

### Task 5: Filter Example Canvases From List When Disabled

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasService.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasServicePublishTest.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasServiceArchiveTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasServiceExampleFilterTest.java`

- [ ] **Step 1: Write list filtering tests**

Create `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasServiceExampleFilterTest.java`:

```java
package org.chovy.canvas.domain.canvas;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.chovy.canvas.dto.CanvasListQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CanvasServiceExampleFilterTest {

    @Mock CanvasMapper canvasMapper;
    @Mock CanvasVersionMapper canvasVersionMapper;
    @Mock org.chovy.canvas.engine.dag.DagParser dagParser;
    @Mock org.chovy.canvas.infra.redis.TriggerRouteService triggerRouteService;
    @Mock org.chovy.canvas.engine.trigger.CanvasSchedulerService schedulerService;
    @Mock org.chovy.canvas.infra.cache.CanvasConfigCache configCache;
    @Mock org.chovy.canvas.engine.trigger.CanvasExecutionService canvasExecutionService;
    @Mock org.chovy.canvas.engine.handlers.GroovyHandler groovyHandler;
    @Mock org.chovy.canvas.engine.handlers.MqTriggerHandler mqTriggerHandler;
    @Mock org.springframework.data.redis.core.StringRedisTemplate redis;
    @Mock CanvasTransactionService canvasTransactionService;

    CanvasExamplesProperties properties;
    CanvasService service;

    @BeforeEach
    void setUp() {
        properties = new CanvasExamplesProperties();
        service = new CanvasService(
                canvasMapper, canvasVersionMapper, dagParser, triggerRouteService,
                schedulerService, configCache, canvasExecutionService, groovyHandler,
                mqTriggerHandler, redis, canvasTransactionService, properties);
        when(canvasMapper.selectPage(any(Page.class), any())).thenReturn(new Page<>());
    }

    @Test
    void listIncludesExamplesWhenToggleEnabled() {
        properties.setEnabled(true);

        service.list(new CanvasListQuery());

        ArgumentCaptor<LambdaQueryWrapper<Canvas>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(canvasMapper).selectPage(any(Page.class), captor.capture());
        assertThat(captor.getValue().getSqlSegment()).doesNotContain("is_example");
    }

    @Test
    void listHidesExamplesWhenToggleDisabled() {
        properties.setEnabled(false);

        service.list(new CanvasListQuery());

        ArgumentCaptor<LambdaQueryWrapper<Canvas>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(canvasMapper).selectPage(any(Page.class), captor.capture());
        assertThat(captor.getValue().getSqlSegment()).contains("is_example");
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

```bash
cd backend/canvas-engine
mvn -Dtest=CanvasServiceExampleFilterTest test
```

Expected: FAIL because `CanvasService` does not accept `CanvasExamplesProperties`.

- [ ] **Step 3: Inject properties and filter list**

In `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasService.java`, add constructor field:

```java
private final CanvasExamplesProperties examplesProperties;
```

In `list(CanvasListQuery q)`, add this condition before `orderByDesc`:

```java
.eq(!examplesProperties.isEnabled(), Canvas::getIsExample, 0)
```

The wrapper block becomes:

```java
LambdaQueryWrapper<Canvas> wrapper = new LambdaQueryWrapper<Canvas>()
        .eq(q.getStatus() != null, Canvas::getStatus, q.getStatus())
        .ne(q.getStatus() == null, Canvas::getStatus, CanvasStatusEnum.ARCHIVED.getCode())
        .eq(!examplesProperties.isEnabled(), Canvas::getIsExample, 0)
        .like(q.getName() != null && !q.getName().isBlank(), Canvas::getName, q.getName())
        .orderByDesc(Canvas::getCreatedAt);
```

- [ ] **Step 4: Update existing CanvasService test constructors**

In `CanvasServicePublishTest#setUp`, append `new CanvasExamplesProperties()` to the constructor call:

```java
canvasService = new CanvasService(
        canvasMapper,
        canvasVersionMapper,
        dagParser,
        triggerRouteService,
        schedulerService,
        configCache,
        canvasExecutionService,
        groovyHandler,
        mqTriggerHandler,
        redis,
        canvasTransactionService,
        new CanvasExamplesProperties()
);
```

In `CanvasServiceArchiveTest`, add:

```java
@Mock
private CanvasExamplesProperties examplesProperties;
```

- [ ] **Step 5: Run focused backend tests**

```bash
cd backend/canvas-engine
mvn -Dtest=CanvasServiceExampleFilterTest,CanvasServicePublishTest,CanvasServiceArchiveTest test
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasService.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasServiceExampleFilterTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasServicePublishTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasServiceArchiveTest.java
git commit -m "feat: hide example canvases when disabled"
```

---

### Task 6: Preserve Normal Canvas Semantics For Clone and Template Creation

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasOpsService.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/controller/OpsController.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasOpsServiceExampleCloneTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/OpsControllerTemplateTest.java`

- [ ] **Step 1: Write clone test**

Create `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasOpsServiceExampleCloneTest.java`:

```java
package org.chovy.canvas.domain.canvas;

import org.chovy.canvas.domain.constant.CanvasStatusEnum;
import org.chovy.canvas.domain.constant.VersionStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CanvasOpsServiceExampleCloneTest {

    @Mock CanvasMapper canvasMapper;
    @Mock CanvasVersionMapper canvasVersionMapper;
    @Mock org.chovy.canvas.infra.redis.TriggerRouteService triggerRouteService;
    @Mock org.springframework.data.redis.core.StringRedisTemplate redis;

    @Test
    void cloneDoesNotInheritExampleMarkers() {
        Canvas src = new Canvas();
        src.setId(7L);
        src.setName("示例：新客首单券发放");
        src.setDescription("官方示例");
        src.setStatus(CanvasStatusEnum.DRAFT.getCode());
        src.setIsExample(1);
        src.setSourceTemplateKey("ecommerce_new_user_coupon");
        when(canvasMapper.selectById(7L)).thenReturn(src);
        when(canvasVersionMapper.selectOne(any())).thenReturn(draft(7L, "{\"nodes\":[]}"));
        doAnswer(invocation -> {
            Canvas canvas = invocation.getArgument(0);
            canvas.setId(99L);
            return 1;
        }).when(canvasMapper).insert(any(Canvas.class));

        CanvasOpsService service = new CanvasOpsService(canvasMapper, canvasVersionMapper, triggerRouteService, redis);

        service.clone(7L, "alice");

        ArgumentCaptor<Canvas> canvasCaptor = ArgumentCaptor.forClass(Canvas.class);
        verify(canvasMapper).insert(canvasCaptor.capture());
        assertThat(canvasCaptor.getValue().getIsExample()).isEqualTo(0);
        assertThat(canvasCaptor.getValue().getSourceTemplateKey()).isNull();

        ArgumentCaptor<CanvasVersion> versionCaptor = ArgumentCaptor.forClass(CanvasVersion.class);
        verify(canvasVersionMapper).insert(versionCaptor.capture());
        assertThat(versionCaptor.getValue().getCanvasId()).isEqualTo(99L);
        assertThat(versionCaptor.getValue().getGraphJson()).isEqualTo("{\"nodes\":[]}");
    }

    private static CanvasVersion draft(Long canvasId, String graphJson) {
        CanvasVersion version = new CanvasVersion();
        version.setCanvasId(canvasId);
        version.setVersion(1);
        version.setGraphJson(graphJson);
        version.setStatus(VersionStatus.DRAFT.getCode());
        return version;
    }
}
```

- [ ] **Step 2: Write from-template test**

Create `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/OpsControllerTemplateTest.java`:

```java
package org.chovy.canvas.controller;

import org.chovy.canvas.domain.approval.CanvasManualApprovalMapper;
import org.chovy.canvas.domain.canvas.*;
import org.chovy.canvas.domain.constant.CanvasStatusEnum;
import org.chovy.canvas.domain.constant.VersionStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OpsControllerTemplateTest {

    @Mock CanvasTemplateMapper templateMapper;
    @Mock CanvasMapper canvasMapper;
    @Mock CanvasVersionMapper canvasVersionMapper;
    @Mock CanvasManualApprovalMapper approvalMapper;
    @Mock org.chovy.canvas.infra.cache.CanvasConfigCache configCache;

    @Test
    void createFromTemplateCreatesDraftVersionWithTemplateGraph() {
        CanvasTemplate template = new CanvasTemplate();
        template.setId(5L);
        template.setName("示例：事件触发新客领券");
        template.setDescription("事件触发后判断新客并发券");
        template.setGraphJson("{\"nodes\":[]}");
        template.setUseCount(3);
        when(templateMapper.selectById(5L)).thenReturn(template);
        doAnswer(invocation -> {
            Canvas canvas = invocation.getArgument(0);
            canvas.setId(88L);
            return 1;
        }).when(canvasMapper).insert(any(Canvas.class));

        OpsController controller = new OpsController(
                templateMapper, canvasMapper, canvasVersionMapper, approvalMapper, configCache);
        OpsController.FromTemplateReq req = new OpsController.FromTemplateReq();
        req.setName("我的新客发券流程");

        Canvas created = controller.createFromTemplate(5L, req).block().getData();

        assertThat(created.getId()).isEqualTo(88L);
        assertThat(created.getName()).isEqualTo("我的新客发券流程");
        assertThat(created.getStatus()).isEqualTo(CanvasStatusEnum.DRAFT.getCode());
        assertThat(created.getIsExample()).isEqualTo(0);
        assertThat(created.getSourceTemplateKey()).isNull();

        ArgumentCaptor<CanvasVersion> versionCaptor = ArgumentCaptor.forClass(CanvasVersion.class);
        verify(canvasVersionMapper).insert(versionCaptor.capture());
        CanvasVersion version = versionCaptor.getValue();
        assertThat(version.getCanvasId()).isEqualTo(88L);
        assertThat(version.getVersion()).isEqualTo(1);
        assertThat(version.getGraphJson()).isEqualTo("{\"nodes\":[]}");
        assertThat(version.getStatus()).isEqualTo(VersionStatus.DRAFT.getCode());

        assertThat(template.getUseCount()).isEqualTo(4);
        verify(templateMapper).updateById(template);
    }
}
```

- [ ] **Step 3: Run tests to verify they fail**

```bash
cd backend/canvas-engine
mvn -Dtest=CanvasOpsServiceExampleCloneTest,OpsControllerTemplateTest test
```

Expected: FAIL because clone inherits `null` example fields without explicit zeroing, and `createFromTemplate` does not create a `CanvasVersion`.

- [ ] **Step 4: Fix clone**

In `CanvasOpsService.clone`, set normal canvas metadata before insert:

```java
copy.setIsExample(0);
copy.setSourceTemplateKey(null);
```

- [ ] **Step 5: Fix from-template creation**

In `OpsController.createFromTemplate`, set normal metadata before inserting the canvas and then create the draft version:

```java
canvas.setIsExample(0);
canvas.setSourceTemplateKey(null);
canvasMapper.insert(canvas);

CanvasVersion version = new CanvasVersion();
version.setCanvasId(canvas.getId());
version.setVersion(1);
version.setGraphJson(tpl.getGraphJson());
version.setStatus(org.chovy.canvas.domain.constant.VersionStatus.DRAFT.getCode());
version.setCreatedBy("current_user");
canvasVersionMapper.insert(version);
```

- [ ] **Step 6: Run tests**

```bash
cd backend/canvas-engine
mvn -Dtest=CanvasOpsServiceExampleCloneTest,OpsControllerTemplateTest test
```

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasOpsService.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/controller/OpsController.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasOpsServiceExampleCloneTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/controller/OpsControllerTemplateTest.java
git commit -m "fix: create draft canvases from templates"
```

---

### Task 7: Generate Official Template SQL

**Files:**
- Create: `backend/canvas-engine/scripts/generate-canvas-example-sql.mjs`
- Create: `backend/canvas-engine/src/main/resources/db/migration/V55__canvas_example_templates.sql`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasExampleTemplateMigrationTest.java`

- [ ] **Step 1: Write migration content test**

Create `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasExampleTemplateMigrationTest.java`:

```java
package org.chovy.canvas.domain.canvas;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class CanvasExampleTemplateMigrationTest {

    @Test
    void templateMigrationContainsAllOfficialTemplateKeys() throws Exception {
        String sql = new ClassPathResource("db/migration/V55__canvas_example_templates.sql")
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(sql).contains("INSERT INTO canvas_template");
        assertThat(sql.split("template_key = VALUES\\(template_key\\)", -1)).hasSize(2);

        String[] keys = {
                "component_event_if_coupon", "component_mq_validate_route",
                "component_scheduled_audience_push", "component_direct_call_return",
                "component_selector_multi_branch", "component_priority_offer",
                "component_ab_split_compare", "component_hub_wait_all",
                "component_aggregate_kpi", "component_threshold_fast_win",
                "component_logic_relation", "component_manual_approval",
                "component_delay_followup", "component_groovy_transform",
                "component_tagger_offline", "component_tagger_realtime",
                "component_sub_flow_ref", "component_send_mq_receipt",
                "ecommerce_new_user_coupon", "ecommerce_cart_recall",
                "ecommerce_vip_tier_offer", "ecommerce_cross_sell",
                "travel_flight_delay_care", "travel_hotel_bundle",
                "travel_high_value_route", "travel_pre_departure_reminder",
                "fintech_card_activation", "fintech_risk_review",
                "fintech_loan_repay_reminder", "fintech_wealth_cross_sell",
                "saas_trial_nurture", "saas_onboarding_steps",
                "saas_churn_risk_save", "saas_expansion_signal",
                "local_food_coupon", "local_service_reactivation",
                "local_weather_push", "retail_store_lbs",
                "retail_inventory_clearance", "retail_member_anniversary",
                "content_subscription_trial", "content_inactive_reader",
                "gaming_level_reward", "gaming_lost_user_winback",
                "education_course_followup", "education_learning_reminder",
                "b2b_lead_scoring", "logistics_delivery_care"
        };

        for (String key : keys) {
            assertThat(sql).contains("'" + key + "'");
        }
    }
}
```

- [ ] **Step 2: Run the migration test to verify it fails**

```bash
cd backend/canvas-engine
mvn -Dtest=CanvasExampleTemplateMigrationTest test
```

Expected: FAIL because `V55__canvas_example_templates.sql` does not exist.

- [ ] **Step 3: Add the SQL generator**

Create `backend/canvas-engine/scripts/generate-canvas-example-sql.mjs`:

```js
import fs from 'node:fs'
import path from 'node:path'

const out = path.resolve('src/main/resources/db/migration/V55__canvas_example_templates.sql')

const templates = [
  ['component_event_if_coupon', '组件教学', '拉新转化', '入门', '示例：事件触发新客领券', 'EVENT_TRIGGER,IF_CONDITION,COUPON', 'event_if_coupon'],
  ['component_mq_validate_route', '组件教学', '消息触发', '入门', '示例：MQ 消息校验后路由', 'MQ_TRIGGER,IF_CONDITION,SEND_MQ', 'mq_if_mq'],
  ['component_scheduled_audience_push', '组件教学', '定时运营', '入门', '示例：定时人群 Push', 'SCHEDULED_TRIGGER,TAGGER,REACH_PLATFORM', 'schedule_audience_reach'],
  ['component_direct_call_return', '组件教学', '直调返回', '进阶', '示例：直调查询并同步返回', 'DIRECT_CALL,API_CALL,DIRECT_RETURN', 'direct_api_return'],
  ['component_selector_multi_branch', '组件教学', '多分支路由', '进阶', '示例：条件选择器多分支', 'SELECTOR,IN_APP_NOTIFY', 'selector_notify'],
  ['component_priority_offer', '组件教学', '权益匹配', '进阶', '示例：优先级权益匹配', 'PRIORITY,COUPON', 'priority_coupon'],
  ['component_ab_split_compare', '组件教学', '实验分流', '入门', '示例：AB 分流触达实验', 'AB_SPLIT,IN_APP_NOTIFY,REACH_PLATFORM', 'ab_touch'],
  ['component_hub_wait_all', '组件教学', '并行等待', '复杂', '示例：集线器等待并行完成', 'HUB,API_CALL', 'hub_parallel'],
  ['component_aggregate_kpi', '组件教学', '聚合评估', '复杂', '示例：聚合评估成功率', 'AGGREGATE,IF_CONDITION', 'aggregate'],
  ['component_threshold_fast_win', '组件教学', '快速决策', '复杂', '示例：阈值触发快速决策', 'THRESHOLD,SEND_MQ', 'threshold'],
  ['component_logic_relation', '组件教学', '逻辑组合', '进阶', '示例：逻辑关系组合判断', 'LOGIC_RELATION,IF_CONDITION', 'logic_relation'],
  ['component_manual_approval', '组件教学', '人工审批', '进阶', '示例：人工审批后发券', 'MANUAL_APPROVAL,COUPON', 'approval_coupon'],
  ['component_delay_followup', '组件教学', '延迟触达', '入门', '示例：延迟二次触达', 'DELAY,REACH_PLATFORM', 'delay_reach'],
  ['component_groovy_transform', '组件教学', '字段加工', '复杂', '示例：Groovy 字段加工', 'GROOVY,API_CALL', 'groovy_api'],
  ['component_tagger_offline', '组件教学', '离线标签', '入门', '示例：离线标签判断', 'TAGGER,IF_CONDITION', 'tagger_offline'],
  ['component_tagger_realtime', '组件教学', '实时标签', '进阶', '示例：实时标签触发流程', 'TAGGER,IN_APP_NOTIFY', 'tagger_realtime'],
  ['component_sub_flow_ref', '组件教学', '子流程复用', '复杂', '示例：子流程引用', 'SUB_FLOW_REF,CANVAS_TRIGGER', 'subflow'],
  ['component_send_mq_receipt', '组件教学', '消息通知', '入门', '示例：发送 MQ 通知下游', 'SEND_MQ,END', 'send_mq'],
  ['ecommerce_new_user_coupon', '电商', '拉新转化', '入门', '示例：新客首单券发放', 'EVENT_TRIGGER,IF_CONDITION,COUPON', 'event_if_coupon'],
  ['ecommerce_cart_recall', '电商', '弃购召回', '入门', '示例：加购未支付召回', 'EVENT_TRIGGER,DELAY,REACH_PLATFORM', 'delay_reach'],
  ['ecommerce_vip_tier_offer', '电商', '会员运营', '进阶', '示例：会员等级差异化权益', 'TAGGER,PRIORITY,COUPON', 'priority_coupon'],
  ['ecommerce_cross_sell', '电商', '交叉销售', '进阶', '示例：订单完成后关联推荐', 'EVENT_TRIGGER,API_CALL,IN_APP_NOTIFY', 'event_api_notify'],
  ['travel_flight_delay_care', '出行', '服务关怀', '进阶', '示例：航班延误补偿触达', 'EVENT_TRIGGER,IF_CONDITION,COUPON,REACH_PLATFORM', 'event_if_coupon_reach'],
  ['travel_hotel_bundle', '出行', '复购提升', '入门', '示例：机票成交后酒店联售', 'EVENT_TRIGGER,DELAY,REACH_PLATFORM', 'delay_reach'],
  ['travel_high_value_route', '出行', '高价值客户', '进阶', '示例：高价值用户专属活动', 'TAGGER,IF_CONDITION,COUPON', 'tagger_audience_coupon'],
  ['travel_pre_departure_reminder', '出行', '行前提醒', '入门', '示例：出行前多渠道提醒', 'SCHEDULED_TRIGGER,REACH_PLATFORM,SEND_MQ', 'schedule_reach_mq'],
  ['fintech_card_activation', '金融', '激活转化', '入门', '示例：信用卡开卡激活', 'DIRECT_CALL,API_CALL,REACH_PLATFORM', 'direct_api_reach'],
  ['fintech_risk_review', '金融', '风控拦截', '复杂', '示例：大额交易人工复核', 'EVENT_TRIGGER,IF_CONDITION,MANUAL_APPROVAL', 'risk_approval'],
  ['fintech_loan_repay_reminder', '金融', '还款提醒', '入门', '示例：贷款还款分层提醒', 'SCHEDULED_TRIGGER,TAGGER,REACH_PLATFORM', 'schedule_audience_reach'],
  ['fintech_wealth_cross_sell', '金融', '交叉销售', '进阶', '示例：理财产品适配推荐', 'TAGGER,SELECTOR,IN_APP_NOTIFY', 'selector_notify'],
  ['saas_trial_nurture', 'SaaS', '试用转化', '进阶', '示例：试用期行为培育', 'EVENT_TRIGGER,DELAY,IN_APP_NOTIFY', 'delay_reach'],
  ['saas_onboarding_steps', 'SaaS', '新手引导', '入门', '示例：新账号上手路径', 'DIRECT_CALL,SELECTOR,IN_APP_NOTIFY', 'selector_notify'],
  ['saas_churn_risk_save', 'SaaS', '流失挽回', '进阶', '示例：低活跃客户挽留', 'SCHEDULED_TRIGGER,TAGGER,COUPON,REACH_PLATFORM', 'tagger_audience_coupon'],
  ['saas_expansion_signal', 'SaaS', '增购扩容', '复杂', '示例：高用量客户扩容推荐', 'API_CALL,AGGREGATE,SEND_MQ', 'aggregate'],
  ['local_food_coupon', '本地生活', '到店转化', '入门', '示例：餐饮券包发放', 'TAGGER,COUPON,REACH_PLATFORM', 'tagger_audience_coupon'],
  ['local_service_reactivation', '本地生活', '沉睡召回', '入门', '示例：本地服务沉睡用户召回', 'SCHEDULED_TRIGGER,TAGGER,REACH_PLATFORM', 'schedule_audience_reach'],
  ['local_weather_push', '本地生活', '场景营销', '进阶', '示例：天气触发即时权益', 'API_CALL,IF_CONDITION,COUPON', 'event_if_coupon'],
  ['retail_store_lbs', '零售', '到店引流', '进阶', '示例：门店附近用户触达', 'EVENT_TRIGGER,TAGGER,REACH_PLATFORM', 'tagger_audience_coupon'],
  ['retail_inventory_clearance', '零售', '清仓促销', '入门', '示例：库存清仓定向触达', 'SCHEDULED_TRIGGER,TAGGER,IN_APP_NOTIFY', 'schedule_audience_reach'],
  ['retail_member_anniversary', '零售', '会员纪念日', '入门', '示例：会员周年礼', 'SCHEDULED_TRIGGER,COUPON,IN_APP_NOTIFY', 'event_if_coupon'],
  ['content_subscription_trial', '内容平台', '订阅转化', '进阶', '示例：内容试读后订阅转化', 'EVENT_TRIGGER,AB_SPLIT,REACH_PLATFORM', 'ab_touch'],
  ['content_inactive_reader', '内容平台', '活跃提升', '入门', '示例：沉默读者唤醒', 'SCHEDULED_TRIGGER,TAGGER,IN_APP_NOTIFY', 'schedule_audience_reach'],
  ['gaming_level_reward', '游戏', '成长激励', '入门', '示例：等级达成奖励', 'EVENT_TRIGGER,COUPON,IN_APP_NOTIFY', 'event_if_coupon'],
  ['gaming_lost_user_winback', '游戏', '回流召回', '进阶', '示例：流失玩家回流礼包', 'SCHEDULED_TRIGGER,TAGGER,COUPON', 'tagger_audience_coupon'],
  ['education_course_followup', '教育', '课程转化', '入门', '示例：试听课后跟进', 'EVENT_TRIGGER,DELAY,REACH_PLATFORM', 'delay_reach'],
  ['education_learning_reminder', '教育', '学习促活', '入门', '示例：学习计划提醒', 'SCHEDULED_TRIGGER,REACH_PLATFORM,END', 'schedule_reach_mq'],
  ['b2b_lead_scoring', 'B2B', '线索培育', '复杂', '示例：线索评分后分配', 'API_CALL,THRESHOLD,SEND_MQ', 'threshold'],
  ['logistics_delivery_care', '物流', '服务通知', '进阶', '示例：异常配送关怀', 'EVENT_TRIGGER,IF_CONDITION,REACH_PLATFORM', 'event_if_coupon_reach'],
]

function node(id, type, name, category, x, y, config = {}) {
  return { id, type, name, category, x, y, config, bizConfig: config }
}

const graphs = {
  event_if_coupon: () => [
    node('start', 'START', '开始', '流程控制', 400, 0, { nextNodeId: 'event' }),
    node('event', 'EVENT_TRIGGER', '订单完成事件', '行为策略', 400, 140, { eventCode: 'ORDER_COMPLETE', nextNodeId: 'if_new' }),
    node('if_new', 'IF_CONDITION', '是否目标用户', '逻辑分支', 400, 280, { rules: [{ field: 'isNewUser', operator: 'EQ', value: 'true' }], successNodeId: 'coupon', failNodeId: 'end' }),
    node('coupon', 'COUPON', '发放权益券', '权益发放', 180, 420, { couponTypeKey: 'flight_coupon', nextNodeId: 'end' }),
    node('end', 'END', '结束', '流程控制', 400, 560, {}),
  ],
  mq_if_mq: () => [
    node('start', 'START', '开始', '流程控制', 400, 0, { nextNodeId: 'mq' }),
    node('mq', 'MQ_TRIGGER', '订单状态 MQ', '行为策略', 400, 140, { topicKey: 'CANVAS_MQ_TRIGGER', validateResult: true, validateRules: [{ field: 'orderStatus', operator: 'EQ', value: 'PAID' }], nextNodeId: 'if_paid' }),
    node('if_paid', 'IF_CONDITION', '是否支付成功', '逻辑分支', 400, 280, { rules: [{ field: 'orderStatus', operator: 'EQ', value: 'PAID' }], successNodeId: 'send_mq', failNodeId: 'end' }),
    node('send_mq', 'SEND_MQ', '通知下游系统', '其他', 180, 420, { messageCodeKey: 'order_paid_notice', nextNodeId: 'end' }),
    node('end', 'END', '结束', '流程控制', 400, 560, {}),
  ],
  schedule_audience_reach: () => [
    node('start', 'START', '开始', '流程控制', 400, 0, { nextNodeId: 'schedule' }),
    node('schedule', 'SCHEDULED_TRIGGER', '每日定时触发', '行为策略', 400, 140, { scheduleType: 'CRON', cronExpression: '0 0 9 * * ?', timezone: 'Asia/Shanghai', nextNodeId: 'audience' }),
    node('audience', 'TAGGER', '判断目标人群', '人群圈选', 400, 280, { mode: 'audience', audienceId: 90001, hitNextNodeId: 'reach', missNextNodeId: 'end' }),
    node('reach', 'REACH_PLATFORM', '发送营销触达', '用户触达', 180, 420, { serviceSceneKey: 'promo_push', nextNodeId: 'end' }),
    node('end', 'END', '结束', '流程控制', 400, 560, {}),
  ],
  direct_api_return: () => [
    node('start', 'START', '开始', '流程控制', 400, 0, { nextNodeId: 'direct' }),
    node('direct', 'DIRECT_CALL', '业务直调触发', '行为策略', 400, 140, { eventCode: 'ORDER_COMPLETE', nextNodeId: 'api' }),
    node('api', 'API_CALL', '查询用户信息', '其他', 400, 280, { apiKey: 'query_user_info', outputPrefix: 'user', nextNodeId: 'return' }),
    node('return', 'DIRECT_RETURN', '同步返回结果', '用户触达', 400, 420, { buildType: 'CUSTOM', data: [{ key: 'userLevel', value: '${user.level}' }] }),
  ],
  selector_notify: () => [
    node('start', 'START', '开始', '流程控制', 400, 0, { nextNodeId: 'selector' }),
    node('selector', 'SELECTOR', '按用户分层选择', '逻辑分支', 400, 160, { branches: [{ label: '高价值', rules: [{ field: 'score', operator: 'GT', value: '80' }], nextNodeId: 'notify_a' }, { label: '普通', rules: [{ field: 'score', operator: 'GTE', value: '40' }], nextNodeId: 'notify_b' }], elseNodeId: 'notify_c' }),
    node('notify_a', 'IN_APP_NOTIFY', '高价值消息', '用户触达', 120, 340, { messageCodeKey: 'vip_message', nextNodeId: 'end' }),
    node('notify_b', 'IN_APP_NOTIFY', '普通消息', '用户触达', 400, 340, { messageCodeKey: 'normal_message', nextNodeId: 'end' }),
    node('notify_c', 'IN_APP_NOTIFY', '兜底消息', '用户触达', 680, 340, { messageCodeKey: 'fallback_message', nextNodeId: 'end' }),
    node('end', 'END', '结束', '流程控制', 400, 520, {}),
  ],
  priority_coupon: () => [
    node('start', 'START', '开始', '流程控制', 400, 0, { nextNodeId: 'priority' }),
    node('priority', 'PRIORITY', '权益优先级', '逻辑分支', 400, 160, { priorities: [{ order: 1, nextNodeId: 'coupon_a' }, { order: 2, nextNodeId: 'coupon_b' }], nextNodeId: 'end' }),
    node('coupon_a', 'COUPON', '高价值券', '权益发放', 180, 340, { couponTypeKey: 'vip_coupon', nextNodeId: 'end' }),
    node('coupon_b', 'COUPON', '普通券', '权益发放', 620, 340, { couponTypeKey: 'flight_coupon', nextNodeId: 'end' }),
    node('end', 'END', '结束', '流程控制', 400, 520, {}),
  ],
  ab_touch: () => [
    node('start', 'START', '开始', '流程控制', 400, 0, { nextNodeId: 'ab' }),
    node('ab', 'AB_SPLIT', 'AB 分流', '人群圈选', 400, 160, { experimentKey: 'exp_push_campaign', groups: [{ groupKey: 'A', nextNodeId: 'push_a' }, { groupKey: 'B', nextNodeId: 'push_b' }] }),
    node('push_a', 'IN_APP_NOTIFY', 'A 组站内信', '用户触达', 200, 340, { messageCodeKey: 'flight_promo_push', nextNodeId: 'end' }),
    node('push_b', 'REACH_PLATFORM', 'B 组触达平台', '用户触达', 600, 340, { serviceSceneKey: 'promo_push', nextNodeId: 'end' }),
    node('end', 'END', '结束', '流程控制', 400, 520, {}),
  ],
  delay_reach: () => [
    node('start', 'START', '开始', '流程控制', 400, 0, { nextNodeId: 'event' }),
    node('event', 'EVENT_TRIGGER', '行为事件', '行为策略', 400, 140, { eventCode: 'ORDER_COMPLETE', nextNodeId: 'delay' }),
    node('delay', 'DELAY', '等待 30 分钟', '其他', 400, 280, { duration: 30, unit: 'MINUTE', nextNodeId: 'reach' }),
    node('reach', 'REACH_PLATFORM', '二次触达', '用户触达', 400, 420, { serviceSceneKey: 'promo_push', nextNodeId: 'end' }),
    node('end', 'END', '结束', '流程控制', 400, 560, {}),
  ],
}

graphs.event_api_notify = () => [
  node('start', 'START', '开始', '流程控制', 400, 0, { nextNodeId: 'event' }),
  node('event', 'EVENT_TRIGGER', '订单完成事件', '行为策略', 400, 140, { eventCode: 'ORDER_COMPLETE', nextNodeId: 'api' }),
  node('api', 'API_CALL', '查询推荐内容', '其他', 400, 280, { apiKey: 'query_user_info', outputPrefix: 'rec', nextNodeId: 'notify' }),
  node('notify', 'IN_APP_NOTIFY', '发送推荐消息', '用户触达', 400, 420, { messageCodeKey: 'flight_promo_push', nextNodeId: 'end' }),
  node('end', 'END', '结束', '流程控制', 400, 560, {}),
]

graphs.event_if_coupon_reach = () => {
  const nodes = graphs.event_if_coupon()
  nodes.splice(4, 0, node('reach', 'REACH_PLATFORM', '补偿触达', '用户触达', 180, 560, { serviceSceneKey: 'promo_push', nextNodeId: 'end' }))
  nodes.find(n => n.id === 'coupon').config.nextNodeId = 'reach'
  nodes.find(n => n.id === 'coupon').bizConfig.nextNodeId = 'reach'
  nodes.find(n => n.id === 'end').y = 700
  return nodes
}

graphs.schedule_reach_mq = () => [
  node('start', 'START', '开始', '流程控制', 400, 0, { nextNodeId: 'schedule' }),
  node('schedule', 'SCHEDULED_TRIGGER', '定时触发', '行为策略', 400, 140, { scheduleType: 'CRON', cronExpression: '0 0 9 * * ?', timezone: 'Asia/Shanghai', nextNodeId: 'reach' }),
  node('reach', 'REACH_PLATFORM', '发送提醒', '用户触达', 400, 280, { serviceSceneKey: 'promo_push', nextNodeId: 'send_mq' }),
  node('send_mq', 'SEND_MQ', '通知业务系统', '其他', 400, 420, { messageCodeKey: 'reminder_sent', nextNodeId: 'end' }),
  node('end', 'END', '结束', '流程控制', 400, 560, {}),
]

graphs.direct_api_reach = () => [
  node('start', 'START', '开始', '流程控制', 400, 0, { nextNodeId: 'direct' }),
  node('direct', 'DIRECT_CALL', '业务直调', '行为策略', 400, 140, { eventCode: 'USER_ACTIVE', nextNodeId: 'api' }),
  node('api', 'API_CALL', '查询状态', '其他', 400, 280, { apiKey: 'query_user_info', outputPrefix: 'user', nextNodeId: 'reach' }),
  node('reach', 'REACH_PLATFORM', '发送激活提醒', '用户触达', 400, 420, { serviceSceneKey: 'promo_push', nextNodeId: 'end' }),
  node('end', 'END', '结束', '流程控制', 400, 560, {}),
]

graphs.risk_approval = () => [
  node('start', 'START', '开始', '流程控制', 400, 0, { nextNodeId: 'event' }),
  node('event', 'EVENT_TRIGGER', '交易事件', '行为策略', 400, 140, { eventCode: 'ORDER_COMPLETE', nextNodeId: 'if_risk' }),
  node('if_risk', 'IF_CONDITION', '是否大额风险', '逻辑分支', 400, 280, { rules: [{ field: 'amount', operator: 'GT', value: '1000' }], successNodeId: 'approval', failNodeId: 'end' }),
  node('approval', 'MANUAL_APPROVAL', '人工复核', '其他', 180, 420, { approvers: ['risk_ops'], timeoutHours: 24, onTimeout: 'REJECT', approveNodeId: 'send_mq', rejectNodeId: 'end' }),
  node('send_mq', 'SEND_MQ', '通知通过结果', '其他', 180, 560, { messageCodeKey: 'risk_passed', nextNodeId: 'end' }),
  node('end', 'END', '结束', '流程控制', 400, 700, {}),
]

graphs.tagger_audience_coupon = () => [
  node('start', 'START', '开始', '流程控制', 400, 0, { nextNodeId: 'audience' }),
  node('audience', 'TAGGER', '圈选目标人群', '人群圈选', 400, 160, { mode: 'audience', audienceId: 90001, hitNextNodeId: 'coupon', missNextNodeId: 'end' }),
  node('coupon', 'COUPON', '发放定向权益', '权益发放', 200, 340, { couponTypeKey: 'flight_coupon', nextNodeId: 'reach' }),
  node('reach', 'REACH_PLATFORM', '权益到账通知', '用户触达', 200, 500, { serviceSceneKey: 'promo_push', nextNodeId: 'end' }),
  node('end', 'END', '结束', '流程控制', 400, 660, {}),
]

graphs.aggregate = () => [
  node('start', 'START', '开始', '流程控制', 400, 0, { nextNodeId: 'api_a' }),
  node('api_a', 'API_CALL', '渠道 A 调用', '其他', 180, 160, { apiKey: 'query_user_info', nextNodeId: 'aggregate' }),
  node('api_b', 'API_CALL', '渠道 B 调用', '其他', 620, 160, { apiKey: 'query_user_info', nextNodeId: 'aggregate' }),
  node('aggregate', 'AGGREGATE', '聚合评估成功率', '逻辑分支', 400, 340, { evaluateMode: 'rate', minRate: 50, successNodeId: 'send_mq', failNodeId: 'end' }),
  node('send_mq', 'SEND_MQ', '同步成功结论', '其他', 200, 500, { messageCodeKey: 'aggregate_passed', nextNodeId: 'end' }),
  node('end', 'END', '结束', '流程控制', 400, 660, {}),
]

graphs.threshold = () => [
  node('start', 'START', '开始', '流程控制', 400, 0, { nextNodeId: 'api_a' }),
  node('api_a', 'API_CALL', '信号 A', '其他', 180, 160, { apiKey: 'query_user_info', nextNodeId: 'threshold' }),
  node('api_b', 'API_CALL', '信号 B', '其他', 620, 160, { apiKey: 'query_user_info', nextNodeId: 'threshold' }),
  node('threshold', 'THRESHOLD', '达到阈值即触发', '逻辑分支', 400, 340, { thresholdMode: 'min_success', threshold: 1, successNodeId: 'send_mq', failNodeId: 'end' }),
  node('send_mq', 'SEND_MQ', '通知达标结果', '其他', 200, 500, { messageCodeKey: 'threshold_passed', nextNodeId: 'end' }),
  node('end', 'END', '结束', '流程控制', 400, 660, {}),
]

graphs.hub_parallel = () => [
  node('start', 'START', '开始', '流程控制', 400, 0, { nextNodeId: 'api_a' }),
  node('api_a', 'API_CALL', '并行任务 A', '其他', 180, 160, { apiKey: 'query_user_info', nextNodeId: 'hub' }),
  node('api_b', 'API_CALL', '并行任务 B', '其他', 620, 160, { apiKey: 'query_user_info', nextNodeId: 'hub' }),
  node('hub', 'HUB', '等待并行完成', '逻辑分支', 400, 340, { timeout: 600, nextNodeId: 'send_mq' }),
  node('send_mq', 'SEND_MQ', '同步完成结果', '其他', 400, 500, { messageCodeKey: 'hub_done', nextNodeId: 'end' }),
  node('end', 'END', '结束', '流程控制', 400, 660, {}),
]
graphs.logic_relation = () => [
  node('start', 'START', '开始', '流程控制', 400, 0, { nextNodeId: 'api_a' }),
  node('api_a', 'API_CALL', '条件来源 A', '其他', 180, 160, { apiKey: 'query_user_info', nextNodeId: 'logic' }),
  node('api_b', 'API_CALL', '条件来源 B', '其他', 620, 160, { apiKey: 'query_user_info', nextNodeId: 'logic' }),
  node('logic', 'LOGIC_RELATION', 'AND 逻辑关系', '逻辑分支', 400, 340, { relation: 'AND', nextNodeId: 'if_passed' }),
  node('if_passed', 'IF_CONDITION', '是否满足组合条件', '逻辑分支', 400, 500, { rules: [{ field: 'logicPassed', operator: 'EQ', value: 'true' }], successNodeId: 'send_mq', failNodeId: 'end' }),
  node('send_mq', 'SEND_MQ', '通知满足条件', '其他', 200, 660, { messageCodeKey: 'logic_passed', nextNodeId: 'end' }),
  node('end', 'END', '结束', '流程控制', 400, 820, {}),
]
graphs.approval_coupon = () => [
  node('start', 'START', '开始', '流程控制', 400, 0, { nextNodeId: 'approval' }),
  node('approval', 'MANUAL_APPROVAL', '人工审批', '其他', 400, 160, { approvers: ['ops_owner'], timeoutHours: 24, onTimeout: 'REJECT', approveNodeId: 'coupon', rejectNodeId: 'end' }),
  node('coupon', 'COUPON', '审批通过发券', '权益发放', 180, 340, { couponTypeKey: 'flight_coupon', nextNodeId: 'end' }),
  node('end', 'END', '结束', '流程控制', 400, 520, {}),
]
graphs.groovy_api = () => [
  node('start', 'START', '开始', '流程控制', 400, 0, { nextNodeId: 'groovy' }),
  node('groovy', 'GROOVY', '加工字段', '其他', 400, 160, { inputParams: [], code: 'return [score: 88]', outputParams: [{ key: 'score', dataType: 'NUMBER' }], nextNodeId: 'api' }),
  node('api', 'API_CALL', '提交加工结果', '其他', 400, 320, { apiKey: 'query_user_info', outputPrefix: 'api', nextNodeId: 'end' }),
  node('end', 'END', '结束', '流程控制', 400, 480, {}),
]
graphs.tagger_offline = () => [
  node('start', 'START', '开始', '流程控制', 400, 0, { nextNodeId: 'tagger' }),
  node('tagger', 'TAGGER', '读取离线标签', '人群圈选', 400, 160, { mode: 'offline', tagCodeKey: 'market_identity', nextNodeId: 'if_tag' }),
  node('if_tag', 'IF_CONDITION', '是否目标标签', '逻辑分支', 400, 320, { rules: [{ field: 'tagValue', operator: 'EQ', value: 'VIP' }], successNodeId: 'notify', failNodeId: 'end' }),
  node('notify', 'IN_APP_NOTIFY', '发送标签消息', '用户触达', 180, 480, { messageCodeKey: 'vip_message', nextNodeId: 'end' }),
  node('end', 'END', '结束', '流程控制', 400, 640, {}),
]
graphs.tagger_realtime = () => [
  node('start', 'START', '开始', '流程控制', 400, 0, { nextNodeId: 'tagger' }),
  node('tagger', 'TAGGER', '实时标签触发', '行为策略', 400, 160, { mode: 'realtime', tagCodeKey: 'high_value_user', nextNodeId: 'notify' }),
  node('notify', 'IN_APP_NOTIFY', '实时标签消息', '用户触达', 400, 320, { messageCodeKey: 'vip_message', nextNodeId: 'end' }),
  node('end', 'END', '结束', '流程控制', 400, 480, {}),
]
graphs.subflow = () => [
  node('start', 'START', '开始', '流程控制', 400, 0, { nextNodeId: 'canvas_trigger' }),
  node('canvas_trigger', 'CANVAS_TRIGGER', '触发子画布', '其他', 400, 160, { targetCanvasId: 1, invokeMode: 'ASYNC', nextNodeId: 'subflow' }),
  node('subflow', 'SUB_FLOW_REF', '引用标准子流程', '其他', 400, 320, { subFlowId: 1, subFlowVersion: -1, outputPrefix: 'sub', nextNodeId: 'end' }),
  node('end', 'END', '结束', '流程控制', 400, 480, {}),
]
graphs.send_mq = () => [
  node('start', 'START', '开始', '流程控制', 400, 0, { nextNodeId: 'send_mq' }),
  node('send_mq', 'SEND_MQ', '发送业务消息', '其他', 400, 160, { messageCodeKey: 'example_notice', nextNodeId: 'end' }),
  node('end', 'END', '结束', '流程控制', 400, 320, {}),
]

function sqlString(value) {
  return `'${String(value).replaceAll('\\', '\\\\').replaceAll("'", "''")}'`
}

const rows = templates.map((item, index) => {
  const [key, companyType, scenario, difficulty, name, covered, pattern] = item
  const graph = { nodes: graphs[pattern]().map(n => ({ ...n, bizConfig: n.config })) }
  const description = `${companyType} / ${scenario} 示例，展示 ${covered} 的配置和组合方式。`
  return `(${[
    sqlString(key),
    sqlString(name),
    sqlString(description),
    sqlString(scenario),
    sqlString(JSON.stringify(graph)),
    'NULL',
    '1',
    '0',
    sqlString('example-seed'),
    'NOW()',
    sqlString(companyType),
    sqlString(scenario),
    sqlString(difficulty),
    sqlString(covered),
    String(index + 1),
    '1',
  ].join(', ')})`
})

const sql = `-- V55: official canvas example templates.

INSERT INTO canvas_template
  (template_key, name, description, category, graph_json, thumbnail,
   is_official, use_count, created_by, created_at,
   company_type, marketing_scenario, difficulty, covered_node_types,
   sort_order, enabled)
VALUES
${rows.join(',\n')}
ON DUPLICATE KEY UPDATE
  template_key = VALUES(template_key),
  name = VALUES(name),
  description = VALUES(description),
  category = VALUES(category),
  graph_json = VALUES(graph_json),
  thumbnail = VALUES(thumbnail),
  is_official = VALUES(is_official),
  created_by = VALUES(created_by),
  company_type = VALUES(company_type),
  marketing_scenario = VALUES(marketing_scenario),
  difficulty = VALUES(difficulty),
  covered_node_types = VALUES(covered_node_types),
  sort_order = VALUES(sort_order),
  enabled = VALUES(enabled);
`

fs.mkdirSync(path.dirname(out), { recursive: true })
fs.writeFileSync(out, sql)
console.log(`Wrote ${templates.length} canvas example templates to ${out}`)
```

- [ ] **Step 4: Run the generator**

```bash
cd backend/canvas-engine
node scripts/generate-canvas-example-sql.mjs
```

Expected: prints `Wrote 48 canvas example templates to ...V55__canvas_example_templates.sql`.

- [ ] **Step 5: Run the migration test**

```bash
cd backend/canvas-engine
mvn -Dtest=CanvasExampleTemplateMigrationTest test
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add backend/canvas-engine/scripts/generate-canvas-example-sql.mjs \
  backend/canvas-engine/src/main/resources/db/migration/V55__canvas_example_templates.sql \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasExampleTemplateMigrationTest.java
git commit -m "feat: seed official canvas example templates"
```

---

### Task 8: Add Example Labels To Canvas List

**Files:**
- Modify: `frontend/src/types/index.ts`
- Modify: `frontend/src/pages/canvas-list/index.tsx`

- [ ] **Step 1: Add type fields**

In `frontend/src/types/index.ts`, extend `Canvas`:

```ts
isExample?: 0 | 1
sourceTemplateKey?: string
```

- [ ] **Step 2: Render an example tag beside the name**

In `frontend/src/pages/canvas-list/index.tsx`, change the name column render to:

```tsx
render: (name, record) => (
  <Space size={6}>
    <Button type="link" onClick={() => navigate(`/canvas/${record.id}/edit`)}>
      {name}
    </Button>
    {record.isExample === 1 && <Tag color="blue">示例</Tag>}
  </Space>
),
```

- [ ] **Step 3: Run frontend checks**

```bash
cd frontend
npm run build
```

Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add frontend/src/types/index.ts frontend/src/pages/canvas-list/index.tsx
git commit -m "feat: mark example canvases in list"
```

---

### Task 9: Add Example Documentation

**Files:**
- Create: `docs/canvas-examples/README.md`
- Create: `docs/canvas-examples/components.md`
- Create: `docs/canvas-examples/combinations.md`
- Create: `docs/canvas-examples/scenarios.md`

- [ ] **Step 1: Create overview doc**

Create `docs/canvas-examples/README.md`:

```markdown
# 画布示例库

画布示例库分为两部分：

1. 官方模板画布：存储在 `canvas_template`，启动后按 `canvas.examples.enabled` 导入到 `canvas` 和 `canvas_version`，默认在画布列表展示。
2. 使用说明文档：解释每个组件怎么配置、怎么连线、适合哪些公司和营销场景。

默认配置：

```yaml
canvas:
  examples:
    enabled: true
```

关闭后，应用不会补齐官方示例，画布列表会隐藏 `is_example=1` 的示例画布。

## 文档入口

- [组件手册](components.md)
- [组合套路](combinations.md)
- [行业场景](scenarios.md)
```
```

- [ ] **Step 2: Create component manual**

Create `docs/canvas-examples/components.md`:

```markdown
# 组件手册

## START

用途：流程入口，每个画布必须有一个。  
配置：无需业务配置，只连接第一个触发节点。  
常见组合：`START -> EVENT_TRIGGER`、`START -> MQ_TRIGGER`、`START -> SCHEDULED_TRIGGER`、`START -> DIRECT_CALL`。  
示例模板：`component_event_if_coupon`、`component_scheduled_audience_push`。

## EVENT_TRIGGER

用途：监听业务事件并触发流程。  
核心配置：`eventCode`。  
输出上下文：由事件定义决定，例如 `orderId`、`userId`、`amount`。  
常见组合：`EVENT_TRIGGER -> IF_CONDITION -> COUPON`。  
示例模板：`component_event_if_coupon`、`ecommerce_new_user_coupon`。

## MQ_TRIGGER

用途：监听 MQ 消息触发流程。  
核心配置：`topicKey`、`validateResult`、`validateRules`。  
常见组合：`MQ_TRIGGER -> IF_CONDITION -> SEND_MQ`。  
示例模板：`component_mq_validate_route`.

## SCHEDULED_TRIGGER

用途：按固定时间或 Cron 批量触发。  
核心配置：`scheduleType`、`cronExpression`、`timezone`。  
常见组合：`SCHEDULED_TRIGGER -> TAGGER(audience) -> REACH_PLATFORM`。  
示例模板：`component_scheduled_audience_push`。

## DIRECT_CALL

用途：业务系统通过 HTTP 直调触发画布。  
核心配置：`eventCode` 或输入参数定义。  
常见组合：`DIRECT_CALL -> API_CALL -> DIRECT_RETURN`。  
示例模板：`component_direct_call_return`。

## IF_CONDITION

用途：根据规则走成功或失败分支。  
核心配置：`rules`、`successNodeId`、`failNodeId`。  
连线规则：成功分支使用 `success` handle，失败分支使用 `fail` handle。  
示例模板：`component_event_if_coupon`。

## SELECTOR

用途：多条件按顺序匹配，命中第一条分支。  
核心配置：`branches`、`elseNodeId`。  
示例模板：`component_selector_multi_branch`。

## PRIORITY

用途：按优先级尝试权益或路径。  
核心配置：`priorities`。  
示例模板：`component_priority_offer`。

## AB_SPLIT

用途：按实验组确定性分流。  
核心配置：`experimentKey`、`groups`。  
示例模板：`component_ab_split_compare`。

## HUB / AGGREGATE / THRESHOLD / LOGIC_RELATION

用途：处理并行后的等待、评估、阈值触发和逻辑组合。  
常见组合：`API_CALL A/B -> AGGREGATE -> SEND_MQ`。  
示例模板：`component_hub_wait_all`、`component_aggregate_kpi`、`component_threshold_fast_win`、`component_logic_relation`。

## TAGGER

用途：标签判断或人群圈选。  
模式：`audience`、`offline`、`realtime`。  
分支：人群模式使用 `hitNextNodeId` 和 `missNextNodeId`。  
示例模板：`component_tagger_offline`、`component_tagger_realtime`、`component_scheduled_audience_push`。

## 动作节点

`API_CALL` 调接口；`COUPON` 发权益；`IN_APP_NOTIFY` 发端内消息；`REACH_PLATFORM` 发触达；`SEND_MQ` 通知下游；`GROOVY` 加工上下文字段。

示例模板：`component_groovy_transform`、`component_send_mq_receipt`。

## 控制节点

`DELAY` 延迟继续；`MANUAL_APPROVAL` 等待人工审批；`DIRECT_RETURN` 同步返回；`SUB_FLOW_REF` 引用子流程；`END` 结束路径。

示例模板：`component_delay_followup`、`component_manual_approval`、`component_direct_call_return`、`component_sub_flow_ref`。
```

- [ ] **Step 3: Create combinations doc**

Create `docs/canvas-examples/combinations.md`:

```markdown
# 组合套路

## 实时事件触发

结构：`START -> EVENT_TRIGGER -> IF_CONDITION -> ACTION -> END`  
适合：下单、支付、注册、激活后的即时运营。  
模板：`component_event_if_coupon`、`ecommerce_new_user_coupon`。

## 定时批量运营

结构：`START -> SCHEDULED_TRIGGER -> TAGGER(audience) -> REACH_PLATFORM -> END`  
适合：每日促活、还款提醒、沉睡用户召回。  
模板：`component_scheduled_audience_push`、`fintech_loan_repay_reminder`。

## 多分支人群运营

结构：`SELECTOR` 或 `PRIORITY` 分层，再连接不同权益或触达。  
适合：会员等级、线索评分、客户生命周期运营。  
模板：`component_selector_multi_branch`、`component_priority_offer`。

## 实验分流

结构：`AB_SPLIT -> A/B 触达 -> END`  
适合：文案、渠道、权益金额的对照实验。  
模板：`component_ab_split_compare`。

## 并行评估

结构：`API_CALL A/B -> AGGREGATE 或 THRESHOLD -> SEND_MQ`  
适合：多渠道投票、风控信号聚合、任一信号达标即触发。  
模板：`component_aggregate_kpi`、`component_threshold_fast_win`。

## 人工审批保护

结构：`IF_CONDITION -> MANUAL_APPROVAL -> SEND_MQ 或 COUPON`  
适合：金融风控、大额权益、异常交易复核。  
模板：`component_manual_approval`、`fintech_risk_review`。

## 同步直调返回

结构：`DIRECT_CALL -> API_CALL -> DIRECT_RETURN`  
适合：业务系统需要同步拿到画布决策结果。  
模板：`component_direct_call_return`。
```

- [ ] **Step 4: Create scenarios doc**

Create `docs/canvas-examples/scenarios.md`:

```markdown
# 行业场景

## 电商

模板：`ecommerce_new_user_coupon`、`ecommerce_cart_recall`、`ecommerce_vip_tier_offer`、`ecommerce_cross_sell`。  
覆盖目标：拉新转化、弃购召回、会员运营、交叉销售。

## 出行

模板：`travel_flight_delay_care`、`travel_hotel_bundle`、`travel_high_value_route`、`travel_pre_departure_reminder`。  
覆盖目标：服务关怀、复购提升、高价值客户运营、行前提醒。

## 金融

模板：`fintech_card_activation`、`fintech_risk_review`、`fintech_loan_repay_reminder`、`fintech_wealth_cross_sell`。  
覆盖目标：激活转化、风控拦截、还款提醒、交叉销售。

## SaaS

模板：`saas_trial_nurture`、`saas_onboarding_steps`、`saas_churn_risk_save`、`saas_expansion_signal`。  
覆盖目标：试用转化、新手引导、流失挽回、增购扩容。

## 本地生活与零售

模板：`local_food_coupon`、`local_service_reactivation`、`local_weather_push`、`retail_store_lbs`、`retail_inventory_clearance`、`retail_member_anniversary`。  
覆盖目标：到店转化、沉睡召回、场景营销、库存促销、会员纪念日。

## 内容、游戏、教育、B2B、物流

模板：`content_subscription_trial`、`content_inactive_reader`、`gaming_level_reward`、`gaming_lost_user_winback`、`education_course_followup`、`education_learning_reminder`、`b2b_lead_scoring`、`logistics_delivery_care`。  
覆盖目标：订阅转化、活跃提升、成长激励、回流召回、课程转化、学习促活、线索培育、服务通知。
```

- [ ] **Step 5: Commit**

```bash
git add docs/canvas-examples/README.md docs/canvas-examples/components.md docs/canvas-examples/combinations.md docs/canvas-examples/scenarios.md
git commit -m "docs: add canvas example library guide"
```

---

### Task 10: Full Verification

**Files:**
- No new files.
- Verify all changed files.

- [ ] **Step 1: Run backend focused tests**

```bash
cd backend/canvas-engine
mvn -Dtest=CanvasExampleLibrarySchemaTest,CanvasExamplesPropertiesTest,CanvasExampleSeederTest,CanvasServiceExampleFilterTest,CanvasOpsServiceExampleCloneTest,OpsControllerTemplateTest,CanvasExampleTemplateMigrationTest,CanvasServicePublishTest,CanvasServiceArchiveTest test
```

Expected: BUILD SUCCESS.

- [ ] **Step 2: Run backend package**

```bash
cd backend/canvas-engine
mvn test
```

Expected: BUILD SUCCESS.

- [ ] **Step 3: Run frontend checks**

```bash
cd frontend
npm test -- branchHandles.test.ts
npm run build
```

Expected: both commands pass.

- [ ] **Step 4: Inspect generated SQL count**

```bash
rg -o "'[a-z0-9_]+'," backend/canvas-engine/src/main/resources/db/migration/V55__canvas_example_templates.sql | wc -l
```

Expected: output is at least `48`.

- [ ] **Step 5: Review worktree**

```bash
git status --short
git log --oneline -10
```

Expected: worktree is clean after commits; recent commits correspond to the tasks above.

---

## Self-Review

Spec coverage:
- Template storage in `canvas_template`: Task 2 and Task 7.
- Default import into `canvas` / `canvas_version`: Task 3, Task 4, Task 7.
- Runtime toggle default enabled: Task 3 and Task 5.
- Idempotent import: Task 4.
- User copies not managed as examples: Task 6.
- Existing frontend rendering path: Task 1 and Task 8.
- 48 official templates: Task 7.
- Component, combination, and scenario docs: Task 9.
- Tests for schema, import, filtering, clone, template creation, and migration content: Tasks 2 through 7 and Task 10.

Placeholder scan:
- The plan contains exact paths, commands, class names, and code snippets for each code task.
- No task depends on undefined method names from a later task.

Type consistency:
- Backend uses Java entity fields `isExample` and `sourceTemplateKey`; frontend uses JSON fields `isExample` and `sourceTemplateKey`.
- Template key field is `templateKey` in Java and `template_key` in SQL.
- Canvas example toggle is `canvas.examples.enabled` in YAML and `CanvasExamplesProperties#isEnabled()` in Java.
