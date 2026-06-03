# Audience Snapshot Mode And Defaults Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add explicit audience default send modes, snapshot schema contracts, TAGGER config fields, and audience UI controls.

**Architecture:** Use an additive Flyway migration and a small enum normalizer so backend request paths store one canonical value. Keep frontend mode labels in a pure helper, then wire the helper into the existing audience edit/list pages.

**Tech Stack:** Java 21, Spring Boot, MyBatis-Plus, Flyway, JUnit 5, Mockito, AssertJ, React 18, TypeScript, Ant Design, Vitest.

---

## Spec Reference

- `docs/product-evolution/specs/p1-003-audience-snapshot-mode-and-defaults.md`

## File Structure

- Create: `backend/canvas-engine/src/main/resources/db/migration/V96__audience_snapshot_mode_and_defaults.sql`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/audience/AudienceSnapshotModeMigrationTest.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/common/enums/AudienceSnapshotMode.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AudienceSnapshotDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/AudienceSnapshotMapper.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AudienceDefinitionDO.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/AudienceController.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/AudienceControllerTest.java`
- Modify: `frontend/src/services/audienceApi.ts`
- Create: `frontend/src/pages/audience-edit/audienceSnapshotMode.ts`
- Create: `frontend/src/pages/audience-edit/audienceSnapshotMode.test.ts`
- Modify: `frontend/src/pages/audience-edit/index.tsx`
- Modify: `frontend/src/pages/audience-list/index.tsx`

### Task 1: Schema And Java Data Contract

**Files:**
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/audience/AudienceSnapshotModeMigrationTest.java`
- Create: `backend/canvas-engine/src/main/resources/db/migration/V96__audience_snapshot_mode_and_defaults.sql`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/common/enums/AudienceSnapshotMode.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AudienceSnapshotDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/AudienceSnapshotMapper.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AudienceDefinitionDO.java`

- [ ] **Step 1: Write migration contract test**

Create `AudienceSnapshotModeMigrationTest.java`:

```java
package org.chovy.canvas.domain.audience;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class AudienceSnapshotModeMigrationTest {

    @Test
    void migrationAddsAudienceModeSnapshotTableAndTaggerConfig() throws Exception {
        String sql = Files.readString(Path.of(
                "src/main/resources/db/migration/V96__audience_snapshot_mode_and_defaults.sql"));

        assertThat(sql)
                .contains("ALTER TABLE audience_definition")
                .contains("default_snapshot_mode")
                .contains("CREATE TABLE IF NOT EXISTS audience_snapshot")
                .contains("user_ids_json")
                .contains("audienceSnapshotMode")
                .contains("audienceSnapshotId")
                .contains("STATIC_LOCKED")
                .contains("DYNAMIC_REFRESH");
    }
}
```

- [ ] **Step 2: Run migration test and confirm red state**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=AudienceSnapshotModeMigrationTest
```

Expected: FAIL because `V96__audience_snapshot_mode_and_defaults.sql` does not exist.

- [ ] **Step 3: Add migration**

Create `V96__audience_snapshot_mode_and_defaults.sql`:

```sql
ALTER TABLE audience_definition
    ADD COLUMN default_snapshot_mode VARCHAR(32) NOT NULL DEFAULT 'STATIC_LOCKED'
        COMMENT 'Default TAGGER audience send mode';

CREATE TABLE IF NOT EXISTS audience_snapshot (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    audience_id BIGINT NOT NULL,
    canvas_id BIGINT NULL,
    canvas_version_id BIGINT NULL,
    node_id VARCHAR(128) NULL,
    snapshot_mode VARCHAR(32) NOT NULL,
    user_count BIGINT NOT NULL DEFAULT 0,
    user_ids_json LONGTEXT NOT NULL,
    created_by VARCHAR(128) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_audience_snapshot_source (audience_id, created_at),
    KEY idx_audience_snapshot_canvas (canvas_id, canvas_version_id, node_id)
) COMMENT='Locked audience user lists for static scheduled sends';

UPDATE node_type_registry
SET config_schema = '[{"key":"mode","label":"标签模式","type":"radio","required":true,"options":[{"label":"实时标签","value":"realtime"},{"label":"离线标签","value":"offline"},{"label":"人群圈选","value":"audience"}]},{"key":"tagCodeKey","label":"标签","type":"select","dataSource":"/meta/tagger-tags","required":true,"showWhen":"mode!=audience"},{"key":"audienceId","label":"人群","type":"select","dataSource":"/canvas/audiences/ready","required":true,"showWhen":"mode==audience"},{"key":"audienceSnapshotMode","label":"发送人群","type":"radio","required":false,"showWhen":"mode==audience","options":[{"label":"发布时锁定","value":"STATIC_LOCKED"},{"label":"每次刷新","value":"DYNAMIC_REFRESH"}]},{"key":"audienceSnapshotId","label":"锁定快照","type":"hidden","required":false,"showWhen":"mode==audience"},{"key":"hitNextNodeId","label":"命中分支","type":"edge-hint","showWhen":"mode==audience","icon":"check"},{"key":"missNextNodeId","label":"未命中分支","type":"edge-hint","showWhen":"mode==audience","icon":"close"}]'
WHERE type_key = 'TAGGER';
```

- [ ] **Step 4: Add enum and snapshot data objects**

Create `AudienceSnapshotMode.java`:

```java
package org.chovy.canvas.common.enums;

public enum AudienceSnapshotMode {
    STATIC_LOCKED,
    DYNAMIC_REFRESH;

    public static AudienceSnapshotMode normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return STATIC_LOCKED;
        }
        for (AudienceSnapshotMode mode : values()) {
            if (mode.name().equalsIgnoreCase(raw.trim())) {
                return mode;
            }
        }
        throw new IllegalArgumentException("Unsupported audienceSnapshotMode: " + raw);
    }
}
```

Create `AudienceSnapshotDO.java`:

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
@TableName("audience_snapshot")
public class AudienceSnapshotDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long audienceId;
    private Long canvasId;
    private Long canvasVersionId;
    private String nodeId;
    private String snapshotMode;
    private Long userCount;
    private String userIdsJson;
    private String createdBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
```

Create `AudienceSnapshotMapper.java`:

```java
package org.chovy.canvas.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.chovy.canvas.dal.dataobject.AudienceSnapshotDO;

@Mapper
public interface AudienceSnapshotMapper extends BaseMapper<AudienceSnapshotDO> {
}
```

Add this field to `AudienceDefinitionDO`:

```java
/** Default send mode used by TAGGER audience nodes. */
private String defaultSnapshotMode;
```

- [ ] **Step 5: Run schema contract test**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=AudienceSnapshotModeMigrationTest
```

Expected: PASS.

### Task 2: Backend Request Normalization

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/AudienceController.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/AudienceControllerTest.java`

- [ ] **Step 1: Add controller tests**

Add tests to `AudienceControllerTest`:

```java
@Test
void createNormalizesBlankDefaultSnapshotModeToStaticLocked() {
    AudienceBatchComputeService computeService = mock(AudienceBatchComputeService.class);
    AudienceDefinitionDO body = new AudienceDefinitionDO();
    body.setName("高价值人群");
    body.setDefaultSnapshotMode("");
    when(computeService.create(any(AudienceDefinitionDO.class))).thenAnswer(invocation -> invocation.getArgument(0));
    AudienceController controller = testController(computeService);

    R<AudienceDefinitionDO> response = controller.create(body).block();

    assertThat(response.getData().getDefaultSnapshotMode()).isEqualTo("STATIC_LOCKED");
}

@Test
void createRejectsInvalidDefaultSnapshotMode() {
    AudienceDefinitionDO body = new AudienceDefinitionDO();
    body.setName("高价值人群");
    body.setDefaultSnapshotMode("FLOATING");
    AudienceController controller = testController(mock(AudienceBatchComputeService.class));

    assertThatThrownBy(() -> controller.create(body).block())
            .hasMessageContaining("Unsupported audienceSnapshotMode");
}
```

Add a private factory in the same test class using the existing constructor signature:

```java
private AudienceController testController(AudienceBatchComputeService computeService) {
    return new AudienceController(
            mock(AudienceDefinitionMapper.class),
            mock(AudienceStatMapper.class),
            computeService,
            mock(AudienceSchedulerService.class),
            mock(AsyncTaskService.class),
            mock(AudienceComputeTaskRunner.class),
            mock(NotificationService.class),
            mock(CdpAudienceSourceService.class));
}
```

- [ ] **Step 2: Run controller tests and confirm red state**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=AudienceControllerTest
```

Expected: FAIL because `AudienceController` does not normalize `defaultSnapshotMode`.

- [ ] **Step 3: Normalize create and update request bodies**

In `AudienceController`, import `AudienceSnapshotMode` and add:

```java
private void normalizeDefaultSnapshotMode(AudienceDefinitionDO body) {
    body.setDefaultSnapshotMode(AudienceSnapshotMode.normalize(body.getDefaultSnapshotMode()).name());
}
```

Call it before `computeService.create(body)` and before update persistence:

```java
normalizeDefaultSnapshotMode(body);
```

- [ ] **Step 4: Run controller tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=AudienceControllerTest
```

Expected: PASS.

### Task 3: Frontend Audience Mode Controls

**Files:**
- Modify: `frontend/src/services/audienceApi.ts`
- Create: `frontend/src/pages/audience-edit/audienceSnapshotMode.ts`
- Create: `frontend/src/pages/audience-edit/audienceSnapshotMode.test.ts`
- Modify: `frontend/src/pages/audience-edit/index.tsx`
- Modify: `frontend/src/pages/audience-list/index.tsx`

- [ ] **Step 1: Add helper tests**

Create `audienceSnapshotMode.test.ts`:

```ts
import { describe, expect, it } from 'vitest'
import { normalizeAudienceSnapshotMode, snapshotModeLabel } from './audienceSnapshotMode'

describe('audience snapshot mode helpers', () => {
  it('defaults blank values to static locked', () => {
    expect(normalizeAudienceSnapshotMode()).toBe('STATIC_LOCKED')
    expect(normalizeAudienceSnapshotMode('')).toBe('STATIC_LOCKED')
  })

  it('keeps dynamic refresh and labels both modes', () => {
    expect(normalizeAudienceSnapshotMode('DYNAMIC_REFRESH')).toBe('DYNAMIC_REFRESH')
    expect(snapshotModeLabel('STATIC_LOCKED')).toBe('发布时锁定')
    expect(snapshotModeLabel('DYNAMIC_REFRESH')).toBe('每次刷新')
  })
})
```

- [ ] **Step 2: Add helper**

Create `audienceSnapshotMode.ts`:

```ts
export type AudienceSnapshotMode = 'STATIC_LOCKED' | 'DYNAMIC_REFRESH'

export function normalizeAudienceSnapshotMode(value?: string): AudienceSnapshotMode {
  return value === 'DYNAMIC_REFRESH' ? 'DYNAMIC_REFRESH' : 'STATIC_LOCKED'
}

export function snapshotModeLabel(value?: string) {
  return normalizeAudienceSnapshotMode(value) === 'DYNAMIC_REFRESH' ? '每次刷新' : '发布时锁定'
}
```

- [ ] **Step 3: Extend API type**

Add the field to the existing `AudienceDefinition` interface in `audienceApi.ts`:

```ts
defaultSnapshotMode?: AudienceSnapshotMode
```

Add the type import or local exported type:

```ts
export type AudienceSnapshotMode = 'STATIC_LOCKED' | 'DYNAMIC_REFRESH'
```

- [ ] **Step 4: Wire edit and list pages**

In `audience-edit/index.tsx`, import `Radio` and the helper. Add a form item:

```tsx
<Form.Item name="defaultSnapshotMode" label="默认发送人群" initialValue="STATIC_LOCKED">
  <Radio.Group>
    <Radio.Button value="STATIC_LOCKED">发布时锁定</Radio.Button>
    <Radio.Button value="DYNAMIC_REFRESH">每次刷新</Radio.Button>
  </Radio.Group>
</Form.Item>
```

Before create/update submit:

```ts
values.defaultSnapshotMode = normalizeAudienceSnapshotMode(values.defaultSnapshotMode)
```

In `audience-list/index.tsx`, add a column:

```tsx
{
  title: '默认发送人群',
  width: 140,
  render: (_, record) => <Tag>{snapshotModeLabel(record.defaultSnapshotMode)}</Tag>,
}
```

- [ ] **Step 5: Run focused frontend test**

Run:

```bash
cd frontend && npm test -- audienceSnapshotMode.test.ts
```

Expected: PASS.

### Task 4: Verification And Commit

**Files:**
- Read: `docs/product-evolution/specs/p1-003-audience-snapshot-mode-and-defaults.md`
- Read: `docs/product-evolution/plans/p1-003-audience-snapshot-mode-and-defaults-plan.md`

- [ ] **Step 1: Run focused backend and frontend tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=AudienceSnapshotModeMigrationTest,AudienceControllerTest
cd frontend && npm test -- audienceSnapshotMode.test.ts
```

Expected: PASS for all named tests.

- [ ] **Step 2: Commit this slice**

Run:

```bash
git add backend/canvas-engine/src/main/resources/db/migration/V96__audience_snapshot_mode_and_defaults.sql \
  backend/canvas-engine/src/main/java/org/chovy/canvas/common/enums/AudienceSnapshotMode.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AudienceDefinitionDO.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AudienceSnapshotDO.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/AudienceSnapshotMapper.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/AudienceController.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/controller/AudienceControllerTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/audience/AudienceSnapshotModeMigrationTest.java \
  frontend/src/services/audienceApi.ts \
  frontend/src/pages/audience-edit/audienceSnapshotMode.ts \
  frontend/src/pages/audience-edit/audienceSnapshotMode.test.ts \
  frontend/src/pages/audience-edit/index.tsx \
  frontend/src/pages/audience-list/index.tsx \
  docs/product-evolution/specs/p1-003-audience-snapshot-mode-and-defaults.md \
  docs/product-evolution/plans/p1-003-audience-snapshot-mode-and-defaults-plan.md
git commit -m "feat: add audience snapshot mode defaults"
```

Expected: commit contains only the audience default mode and schema foundation slice.
