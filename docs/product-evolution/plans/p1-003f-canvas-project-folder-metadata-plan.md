# Canvas Project Folder Metadata Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add flat project/folder metadata to canvases and expose exact list filters plus frontend list labels.

**Architecture:** Add nullable metadata columns through a current-sequence migration, extend existing canvas DTO/DO/query objects, and centralize list wrapper construction in a small helper. Frontend filtering stays as pure parameter building before wiring Ant Design inputs into the canvas list.

**Implementation note:** Actual migration is `V259__canvas_project_folder_metadata.sql`; `V96_1` is not used because the repository's current Flyway sequence is already far beyond that version and `V258` is occupied by BI storage migration work.

**Tech Stack:** Java 21, Spring Boot, MyBatis-Plus, Flyway, JUnit 5, AssertJ, React 18, TypeScript, Ant Design, Vitest.

---

## Spec Reference

- `docs/product-evolution/specs/p1-003f-canvas-project-folder-metadata.md`

## File Structure

- Create: `backend/canvas-engine/src/main/resources/db/migration/V259__canvas_project_folder_metadata.sql`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasProjectMetadataMigrationTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasProjectFilterTest.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasListQuerySupport.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CanvasDO.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/CanvasCreateReq.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/CanvasUpdateReq.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/query/CanvasListQuery.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasService.java`
- Modify: `frontend/src/types/index.ts`
- Modify: `frontend/src/services/api.ts`
- Create: `frontend/src/pages/canvas-list/canvasProjectFilters.ts`
- Create: `frontend/src/pages/canvas-list/canvasProjectFilters.test.ts`
- Modify: `frontend/src/pages/canvas-list/index.tsx`

### Task 1: Schema And DTO Fields

**Files:**
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasProjectMetadataMigrationTest.java`
- Create: `backend/canvas-engine/src/main/resources/db/migration/V259__canvas_project_folder_metadata.sql`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CanvasDO.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/CanvasCreateReq.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/CanvasUpdateReq.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/query/CanvasListQuery.java`

- [x] **Step 1: Write migration contract test**

Create `CanvasProjectMetadataMigrationTest.java`:

```java
package org.chovy.canvas.domain.canvas;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class CanvasProjectMetadataMigrationTest {

    @Test
    void migrationAddsFlatProjectAndFolderColumns() throws Exception {
        String sql = Files.readString(Path.of(
                "src/main/resources/db/migration/V259__canvas_project_folder_metadata.sql"));

        assertThat(sql)
                .contains("ALTER TABLE canvas")
                .contains("project_key")
                .contains("project_name")
                .contains("folder_key")
                .contains("folder_name")
                .contains("idx_canvas_project_folder");
    }
}
```

- [x] **Step 2: Run migration test and confirm red state**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=CanvasProjectMetadataMigrationTest
```

Expected: FAIL because `V259__canvas_project_folder_metadata.sql` does not exist.

- [x] **Step 3: Add migration**

Create `V259__canvas_project_folder_metadata.sql`:

```sql
ALTER TABLE canvas
    ADD COLUMN project_key VARCHAR(128) NULL COMMENT 'Flat project grouping key',
    ADD COLUMN project_name VARCHAR(255) NULL COMMENT 'Flat project display name',
    ADD COLUMN folder_key VARCHAR(128) NULL COMMENT 'Flat folder grouping key',
    ADD COLUMN folder_name VARCHAR(255) NULL COMMENT 'Flat folder display name',
    ADD KEY idx_canvas_project_folder (project_key, folder_key, status, updated_at);
```

- [x] **Step 4: Add Java fields**

Add to `CanvasDO`, `CanvasCreateReq`, and `CanvasUpdateReq`:

```java
private String projectKey;
private String projectName;
private String folderKey;
private String folderName;
```

Add to `CanvasListQuery`:

```java
private String projectKey;
private String folderKey;
```

- [x] **Step 5: Run migration test**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=CanvasProjectMetadataMigrationTest
```

Expected: PASS.

### Task 2: Backend Create/Update/List Filtering

**Files:**
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasProjectFilterTest.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasListQuerySupport.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasService.java`

- [x] **Step 1: Write list filter test**

Create `CanvasProjectFilterTest.java`:

```java
package org.chovy.canvas.domain.canvas;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.query.CanvasListQuery;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CanvasProjectFilterTest {

    @Test
    void listAppliesProjectAndFolderFilters() {
        CanvasListQuery query = new CanvasListQuery();
        query.setProjectKey("growth");
        query.setFolderKey("new-user");

        LambdaQueryWrapper<CanvasDO> wrapper = CanvasListQuerySupport.build(query, true);

        String sql = wrapper.getTargetSql();
        assertThat(sql).contains("project_key", "folder_key");
    }
}
```

- [x] **Step 2: Run filter test and confirm red state**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=CanvasProjectFilterTest
```

Expected: FAIL because `CanvasListQuerySupport` does not exist.

- [x] **Step 3: Add query support and service wiring**

Create `CanvasListQuerySupport.java`:

```java
package org.chovy.canvas.domain.canvas;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.common.enums.CanvasStatusEnum;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.query.CanvasListQuery;

final class CanvasListQuerySupport {

    private CanvasListQuerySupport() {
    }

    static LambdaQueryWrapper<CanvasDO> build(CanvasListQuery q, boolean examplesEnabled) {
        return new LambdaQueryWrapper<CanvasDO>()
                .eq(q.getStatus() != null, CanvasDO::getStatus, q.getStatus())
                .ne(q.getStatus() == null, CanvasDO::getStatus, CanvasStatusEnum.ARCHIVED.getCode())
                .eq(!examplesEnabled, CanvasDO::getIsExample, 0)
                .like(q.getName() != null && !q.getName().isBlank(), CanvasDO::getName, q.getName())
                .eq(q.getProjectKey() != null && !q.getProjectKey().isBlank(), CanvasDO::getProjectKey, q.getProjectKey())
                .eq(q.getFolderKey() != null && !q.getFolderKey().isBlank(), CanvasDO::getFolderKey, q.getFolderKey())
                .orderByDesc(CanvasDO::getCreatedAt);
    }
}
```

In `CanvasService.create`:

```java
canvas.setProjectKey(req.getProjectKey());
canvas.setProjectName(req.getProjectName());
canvas.setFolderKey(req.getFolderKey());
canvas.setFolderName(req.getFolderName());
```

In `CanvasService.updateDraft`:

```java
canvas.setProjectKey(req.getProjectKey());
canvas.setProjectName(req.getProjectName());
canvas.setFolderKey(req.getFolderKey());
canvas.setFolderName(req.getFolderName());
```

In `CanvasService.list`:

```java
LambdaQueryWrapper<CanvasDO> wrapper = CanvasListQuerySupport.build(q, examplesProperties.isEnabled());
```

- [x] **Step 4: Run backend tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=CanvasProjectMetadataMigrationTest,CanvasProjectFilterTest
```

Expected: PASS.

### Task 3: Frontend Filters And Labels

**Files:**
- Modify: `frontend/src/types/index.ts`
- Modify: `frontend/src/services/api.ts`
- Create: `frontend/src/pages/canvas-list/canvasProjectFilters.ts`
- Create: `frontend/src/pages/canvas-list/canvasProjectFilters.test.ts`
- Modify: `frontend/src/pages/canvas-list/index.tsx`

- [x] **Step 1: Add helper tests**

Create `canvasProjectFilters.test.ts`:

```ts
import { describe, expect, it } from 'vitest'
import { buildCanvasListParams, projectFolderLabel } from './canvasProjectFilters'

describe('canvas project filters', () => {
  it('drops blank project and folder filters', () => {
    expect(buildCanvasListParams({
      page: 2,
      projectKey: ' ',
      folderKey: '',
    })).toEqual({ page: 2, size: 20 })
  })

  it('keeps selected project and folder keys', () => {
    expect(buildCanvasListParams({
      page: 1,
      projectKey: 'growth',
      folderKey: 'new-user',
    })).toEqual({ page: 1, size: 20, projectKey: 'growth', folderKey: 'new-user' })
  })

  it('formats project and folder names for table display', () => {
    expect(projectFolderLabel({
      projectKey: 'growth',
      projectName: '增长',
      folderKey: 'new-user',
      folderName: '新客',
    })).toBe('增长 / 新客')
  })
})
```

- [x] **Step 2: Add helper and type fields**

Create `canvasProjectFilters.ts`:

```ts
import type { Canvas } from '../../types'

export interface CanvasProjectFilterState {
  page: number
  projectKey?: string
  folderKey?: string
}

export function buildCanvasListParams(state: CanvasProjectFilterState) {
  const params: Record<string, string | number> = { page: state.page, size: 20 }
  if (state.projectKey?.trim()) params.projectKey = state.projectKey.trim()
  if (state.folderKey?.trim()) params.folderKey = state.folderKey.trim()
  return params
}

export function projectFolderLabel(canvas: Pick<Canvas, 'projectKey' | 'projectName' | 'folderKey' | 'folderName'>) {
  const project = canvas.projectName || canvas.projectKey
  const folder = canvas.folderName || canvas.folderKey
  return [project, folder].filter(Boolean).join(' / ') || '-'
}
```

Add fields to the existing `Canvas` interface:

```ts
projectKey?: string
projectName?: string
folderKey?: string
folderName?: string
```

Add project/folder fields to `CanvasCreateReq`, `CanvasUpdateReq`, and `CanvasListQuery` in `api.ts`.

- [x] **Step 3: Wire list page filters**

In `canvas-list/index.tsx`, add state:

```tsx
const [projectKey, setProjectKey] = useState('')
const [folderKey, setFolderKey] = useState('')
```

Update list fetch:

```tsx
const res = await canvasApi.list(buildCanvasListParams({ page: p, projectKey, folderKey }))
```

Add controls near existing list actions:

```tsx
<Space>
  <Input allowClear value={projectKey} onChange={event => setProjectKey(event.target.value)} onPressEnter={() => fetchList(1)} />
  <Input allowClear value={folderKey} onChange={event => setFolderKey(event.target.value)} onPressEnter={() => fetchList(1)} />
  <Button onClick={() => fetchList(1)}>筛选</Button>
</Space>
```

Add table column:

```tsx
{
  title: '项目/文件夹',
  width: 180,
  render: (_, record) => projectFolderLabel(record),
}
```

- [x] **Step 4: Run frontend tests**

Run:

```bash
cd frontend && npm test -- canvasProjectFilters.test.ts
```

Expected: PASS.

### Task 4: Verification And Commit

**Files:**
- Read: `docs/product-evolution/specs/p1-003f-canvas-project-folder-metadata.md`
- Read: `docs/product-evolution/plans/p1-003f-canvas-project-folder-metadata-plan.md`

- [x] **Step 1: Run focused tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=CanvasProjectMetadataMigrationTest,CanvasProjectFilterTest
cd frontend && npm test -- canvasProjectFilters.test.ts
```

Expected: PASS.

### Verification Evidence

- Backend project/folder metadata, import/export preservation, and controller loop suite:

```bash
cd backend && mvn -pl canvas-engine -Dtest=CanvasProjectMetadataMigrationTest,CanvasProjectFilterTest,CanvasImportExportServiceTest,CanvasProjectFolderMetadataServiceTest,CanvasControllerOperatorLoopTest -DfailIfNoTests=true test
```

Result: 13 tests, 0 failures, 0 errors, 0 skipped.

- Frontend project/filter, import/export, local draft, and settings helper suites:

```bash
cd frontend && npm test -- canvasProjectFilters.test.ts importExportFlow.test.ts localDraft.test.ts settingsPresentation.test.ts
```

Result: 4 test files, 18 tests passed.

- Frontend production build:

```bash
cd frontend && npm run build
```

Result: TypeScript and Vite build passed.

- [ ] **Step 2: Commit this slice**

Run:

```bash
git add backend/canvas-engine/src/main/resources/db/migration/V259__canvas_project_folder_metadata.sql \
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CanvasDO.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/dto/CanvasCreateReq.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/dto/CanvasUpdateReq.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/query/CanvasListQuery.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasService.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasListQuerySupport.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasProjectMetadataMigrationTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasProjectFilterTest.java \
  frontend/src/types/index.ts \
  frontend/src/services/api.ts \
  frontend/src/pages/canvas-list/canvasProjectFilters.ts \
  frontend/src/pages/canvas-list/canvasProjectFilters.test.ts \
  frontend/src/pages/canvas-list/index.tsx \
  docs/product-evolution/specs/p1-003f-canvas-project-folder-metadata.md \
  docs/product-evolution/plans/p1-003f-canvas-project-folder-metadata-plan.md
git commit -m "feat: add canvas project folder metadata"
```

Expected: commit contains only flat canvas project/folder metadata behavior.
