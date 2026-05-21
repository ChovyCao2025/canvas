# Canvas Archive Feature Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Allow users to soft-delete (archive) a canvas from the list page, hiding it from the default view without affecting in-flight executions.

**Architecture:** Add `ARCHIVED=3` to `CanvasStatusEnum`, wire a new `POST /canvas/{id}/archive` endpoint through `CanvasTransactionService → CanvasService → CanvasController`, exclude archived canvases from the default list query, and add a Dropdown "更多" menu with a confirmation modal in the frontend list page.

**Tech Stack:** Java 17, Spring WebFlux, MyBatis-Plus, JUnit 5 + AssertJ (backend); React 18, Ant Design, TypeScript (frontend)

---

## File Map

| File | Change |
|------|--------|
| `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/constant/CanvasStatusEnum.java` | Add `ARCHIVED(3)` |
| `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasTransactionService.java` | Add `archiveDb()` |
| `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasService.java` | Add `archive()`, fix `list()` |
| `backend/canvas-engine/src/main/java/org/chovy/canvas/controller/CanvasController.java` | Add `POST /{id}/archive` |
| `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasServiceArchiveTest.java` | New unit test |
| `frontend/src/services/api.ts` | Add `archive()` to `canvasApi` |
| `frontend/src/pages/canvas-list/index.tsx` | Dropdown + archive modal |

---

## Task 1: Add ARCHIVED enum value

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/constant/CanvasStatusEnum.java`

- [ ] **Step 1: Open the enum and add ARCHIVED(3)**

Replace the existing class body:

```java
@Getter
@AllArgsConstructor
public enum CanvasStatusEnum {

    DRAFT(0),
    PUBLISHED(1),
    OFFLINE(2),
    ARCHIVED(3),
    KILLED(4);

    private final Integer code;
}
```

- [ ] **Step 2: Compile to verify no errors**

```bash
cd backend && mvn compile -pl canvas-engine -q
```

Expected: `BUILD SUCCESS`

- [ ] **Step 3: Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/domain/constant/CanvasStatusEnum.java
git commit -m "feat: add ARCHIVED(3) status to CanvasStatusEnum"
```

---

## Task 2: Backend service — archiveDb() + archive() + list() fix (TDD)

**Files:**
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasServiceArchiveTest.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasTransactionService.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasService.java`

- [ ] **Step 1: Write the failing tests**

Create `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasServiceArchiveTest.java`:

```java
package org.chovy.canvas.domain.canvas;

import org.chovy.canvas.domain.constant.CanvasStatusEnum;
import org.chovy.canvas.dto.CanvasListQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CanvasServiceArchiveTest {

    @Mock
    private CanvasMapper canvasMapper;
    @Mock
    private CanvasTransactionService canvasTransactionService;

    @InjectMocks
    private CanvasService canvasService;

    private Canvas existingCanvas;

    @BeforeEach
    void setUp() {
        existingCanvas = new Canvas();
        existingCanvas.setId(1L);
        existingCanvas.setStatus(CanvasStatusEnum.DRAFT.getCode());
        existingCanvas.setName("测试画布");
    }

    @Test
    @DisplayName("归档草稿状态画布：调用 archiveDb 并成功")
    void archive_draft_canvas_succeeds() {
        when(canvasMapper.selectById(1L)).thenReturn(existingCanvas);

        canvasService.archive(1L, "user_001");

        verify(canvasTransactionService).archiveDb(1L);
    }

    @Test
    @DisplayName("画布不存在时抛出 IllegalArgumentException")
    void archive_nonexistent_canvas_throws() {
        when(canvasMapper.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> canvasService.archive(99L, "user_001"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("画布已是 ARCHIVED 状态时抛出 IllegalStateException")
    void archive_already_archived_canvas_throws() {
        existingCanvas.setStatus(CanvasStatusEnum.ARCHIVED.getCode());
        when(canvasMapper.selectById(1L)).thenReturn(existingCanvas);

        assertThatThrownBy(() -> canvasService.archive(1L, "user_001"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("已归档");
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
cd backend && mvn test -pl canvas-engine -Dtest=CanvasServiceArchiveTest -q
```

Expected: FAIL — `archive` method does not exist yet.

- [ ] **Step 3: Add archiveDb() to CanvasTransactionService**

Add after the existing `offlineDb()` method in `CanvasTransactionService.java`:

```java
@Transactional
void archiveDb(Long id) {
    Canvas canvas = canvasMapper.selectById(id);
    if (canvas == null) throw new IllegalArgumentException("画布不存在: " + id);
    canvas.setStatus(CanvasStatusEnum.ARCHIVED.getCode());
    canvasMapper.updateById(canvas);
}
```

- [ ] **Step 4: Add archive() to CanvasService and fix list()**

In `CanvasService.java`, add this method (after the existing `offline()` method):

```java
/**
 * 归档画布（软删除）。归档后画布从默认列表隐藏，正在运行的 execution 不受影响。
 *
 * @param id       画布 ID
 * @param operator 操作人
 */
public void archive(Long id, String operator) {
    Canvas canvas = canvasMapper.selectById(id);
    if (canvas == null) throw new IllegalArgumentException("画布不存在: " + id);
    if (CanvasStatusEnum.ARCHIVED.getCode().equals(canvas.getStatus())) {
        throw new IllegalStateException("画布已归档: " + id);
    }
    canvasTransactionService.archiveDb(id);
}
```

Also update `list()` to exclude archived canvases by default. Change lines 131–134 from:

```java
LambdaQueryWrapper<Canvas> wrapper = new LambdaQueryWrapper<Canvas>()
        .eq(q.getStatus() != null, Canvas::getStatus, q.getStatus())
        .like(q.getName() != null && !q.getName().isBlank(), Canvas::getName, q.getName())
        .orderByDesc(Canvas::getCreatedAt);
```

to:

```java
LambdaQueryWrapper<Canvas> wrapper = new LambdaQueryWrapper<Canvas>()
        .eq(q.getStatus() != null, Canvas::getStatus, q.getStatus())
        .ne(q.getStatus() == null, Canvas::getStatus, CanvasStatusEnum.ARCHIVED.getCode())
        .like(q.getName() != null && !q.getName().isBlank(), Canvas::getName, q.getName())
        .orderByDesc(Canvas::getCreatedAt);
```

- [ ] **Step 5: Run tests to verify they pass**

```bash
cd backend && mvn test -pl canvas-engine -Dtest=CanvasServiceArchiveTest -q
```

Expected: `Tests run: 3, Failures: 0, Errors: 0`

- [ ] **Step 6: Run all existing tests to verify no regressions**

```bash
cd backend && mvn test -pl canvas-engine -q
```

Expected: `BUILD SUCCESS`

- [ ] **Step 7: Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasTransactionService.java \
        backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasService.java \
        backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasServiceArchiveTest.java
git commit -m "feat: add archive() service method and exclude ARCHIVED from default list"
```

---

## Task 3: Backend controller endpoint

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/controller/CanvasController.java`

- [ ] **Step 1: Add archive endpoint after the existing offline endpoint (around line 106)**

```java
/**
 * 归档画布（软删除）
 *
 * @param id       画布 ID
 * @param operator 操作人标识
 * @return 成功响应
 */
@PostMapping("/{id}/archive")
public Mono<R<Void>> archive(
        @PathVariable Long id,
        @RequestParam(defaultValue = "system") String operator) {
    return Mono.<Void>fromRunnable(() -> canvasService.archive(id, operator))
            .subscribeOn(Schedulers.boundedElastic())
            .thenReturn(R.ok());
}
```

- [ ] **Step 2: Compile to verify no errors**

```bash
cd backend && mvn compile -pl canvas-engine -q
```

Expected: `BUILD SUCCESS`

- [ ] **Step 3: Start the backend and smoke-test the endpoint**

Start the app, then run:

```bash
# Create a draft canvas first if needed, then archive it (replace 1 with an actual canvas ID)
curl -s -X POST http://localhost:8080/canvas/1/archive \
  -H "Authorization: Bearer $(cat /tmp/canvas_token 2>/dev/null || echo 'YOUR_TOKEN')" \
  | python3 -m json.tool
```

Expected response:
```json
{"code": 200, "msg": "success", "data": null}
```

- [ ] **Step 4: Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/controller/CanvasController.java
git commit -m "feat: add POST /canvas/{id}/archive controller endpoint"
```

---

## Task 4: Frontend — API + list page UI

**Files:**
- Modify: `frontend/src/services/api.ts`
- Modify: `frontend/src/pages/canvas-list/index.tsx`

- [ ] **Step 1: Add archive() to canvasApi in api.ts**

In `frontend/src/services/api.ts`, after the `kill` method (around line 93), add:

```typescript
  archive: (id: number) =>
    http.post<R<void>, R<void>>(`/canvas/${id}/archive`),
```

- [ ] **Step 2: Update canvas-list/index.tsx — imports**

Replace the top two import lines:

```typescript
import { useEffect, useState } from 'react'
import {
  Button, Table, Tag, Space, Modal, Form, Input,
  message, Typography, Tooltip, Dropdown,
} from 'antd'
import {
  PlusOutlined, EditOutlined, CloudUploadOutlined,
  StopOutlined, CopyOutlined, ThunderboltOutlined, BarChartOutlined, EyeOutlined,
  MoreOutlined, ExclamationCircleOutlined,
} from '@ant-design/icons'
```

- [ ] **Step 3: Add handleArchive function**

In `canvas-list/index.tsx`, add this function after `handleClone` (around line 99):

```typescript
const handleArchive = (id: number, name: string) => {
  Modal.confirm({
    title: '归档画布',
    icon: <ExclamationCircleOutlined style={{ color: '#faad14' }} />,
    content: (
      <div>
        <p>确认将「{name}」归档？</p>
        <ul style={{ color: '#8c8c8c', fontSize: 13, paddingLeft: 16, margin: '8px 0 0' }}>
          <li>画布将从列表中隐藏</li>
          <li>正在运行中的流程不受影响</li>
          <li>可联系管理员恢复</li>
        </ul>
      </div>
    ),
    okText: '确认归档',
    okButtonProps: { danger: true },
    cancelText: '取消',
    onOk: async () => {
      await canvasApi.archive(id)
      message.success('已归档')
      fetchList()
    },
  })
}
```

- [ ] **Step 4: Add Dropdown to the operations column**

In the `columns` array, update the `操作` column render function. Replace the closing `</Space>` and the lines before it (the clone and stats buttons) with:

```typescript
          <Tooltip title="克隆">
            <Button size="small" icon={<CopyOutlined />}
              onClick={() => handleClone(record.id)} />
          </Tooltip>

          <Tooltip title="效果看板">
            <Button size="small" icon={<BarChartOutlined />}
              onClick={() => navigate(`/canvas/${record.id}/stats`)} />
          </Tooltip>

          <Dropdown
            menu={{
              items: [
                {
                  key: 'archive',
                  label: <span style={{ color: '#ff4d4f' }}>归档画布</span>,
                  onClick: () => handleArchive(record.id, record.name),
                },
              ],
            }}
            trigger={['click']}
          >
            <Button size="small" icon={<MoreOutlined />} />
          </Dropdown>
        </Space>
```

- [ ] **Step 5: Verify TypeScript compiles**

```bash
cd frontend && npx tsc --noEmit
```

Expected: no errors

- [ ] **Step 6: Start frontend dev server and manually test**

```bash
cd frontend && npm run dev
```

Open http://localhost:5173 → 旅程管理 → click "⋯" on any canvas → click "归档画布" → verify warning modal appears with the 3 bullet points → confirm → verify row disappears and "已归档" toast appears.

Also verify: refreshing the page does not bring the archived canvas back.

- [ ] **Step 7: Commit**

```bash
git add frontend/src/services/api.ts frontend/src/pages/canvas-list/index.tsx
git commit -m "feat: add canvas archive UI — dropdown menu + confirmation modal"
```
