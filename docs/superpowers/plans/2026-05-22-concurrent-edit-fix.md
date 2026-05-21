# 画布并发编辑修复 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复前端 editVersion 初始化错误，并将 409 冲突提示升级为带"立即刷新"按钮的弹窗。

**Architecture:** 纯前端三处改动：补 TS 类型 → 修初始化 → 升级 409 UX。后端无需改动。

**Tech Stack:** React 18, TypeScript, Ant Design

---

## File Map

| File | Change |
|------|--------|
| `frontend/src/types/index.ts` | `Canvas` interface 加 `editVersion?: number` |
| `frontend/src/pages/canvas-editor/index.tsx` | 初始化改为 `detail.canvas.editVersion ?? 0`；409 改为 `Modal.confirm` |

---

## Task 1：补充 TypeScript 类型

**Files:**
- Modify: `frontend/src/types/index.ts`

- [ ] **Step 1：在 Canvas interface 中加入 editVersion 字段**

找到 `export interface Canvas {`（约 L23），在 `cronExpression?: string` 之后加一行：

```typescript
export interface Canvas {
  id: number
  name: string
  description?: string
  status: CanvasStatus
  publishedVersionId?: number
  createdBy?: string
  createdAt: string
  updatedAt: string
  triggerType?:    string
  cronExpression?: string
  editVersion?: number
}
```

- [ ] **Step 2：验证 TypeScript 无报错**

```bash
cd frontend && npx tsc --noEmit
```

Expected: 无输出

- [ ] **Step 3：Commit**

```bash
git add frontend/src/types/index.ts
git commit -m "fix: add editVersion to Canvas TypeScript interface"
```

---

## Task 2：修正 editVersion 初始化

**Files:**
- Modify: `frontend/src/pages/canvas-editor/index.tsx`

- [ ] **Step 1：将 editVersion.current 改为从 detail 读取**

找到约 L215：
```typescript
const editVersion   = useRef(0)
```
改为：
```typescript
const editVersion   = useRef(detail.canvas.editVersion ?? 0)
```

- [ ] **Step 2：验证 TypeScript 无报错**

```bash
cd frontend && npx tsc --noEmit
```

Expected: 无输出

- [ ] **Step 3：手动验证初始化正确**

打开浏览器 Network 面板 → 进入编辑页 → 查看 `/canvas/{id}` 响应中的 `canvas.editVersion` 值（例如 `3`）→ 在 Sources 面板中确认 `editVersion.current` 初始值为 `3`，而非 `0`。

- [ ] **Step 4：Commit**

```bash
git add frontend/src/pages/canvas-editor/index.tsx
git commit -m "fix: initialize editVersion from server response instead of hardcoded 0"
```

---

## Task 3：升级 409 冲突提示 UX

**Files:**
- Modify: `frontend/src/pages/canvas-editor/index.tsx`

- [ ] **Step 1：将 409 分支从 message.error 改为 Modal.confirm**

找到约 L562-564：
```typescript
if (err?.response?.status === 409)
  message.error('画布已被他人修改，请刷新后重试')
```
改为：
```typescript
if (err?.response?.status === 409) {
  Modal.confirm({
    title: '画布已被他人修改',
    content: '当前画布已有新版本，刷新后你的未保存内容将丢失。是否立即刷新？',
    okText: '立即刷新',
    cancelText: '暂不刷新',
    onOk: () => window.location.reload(),
  })
}
```

确认 `Modal` 已在顶部 import（`from 'antd'`，当前已存在）。

- [ ] **Step 2：验证 TypeScript 无报错**

```bash
cd frontend && npx tsc --noEmit
```

Expected: 无输出

- [ ] **Step 3：手动验证（需两个浏览器标签页）**

1. 标签页 A 和 B 同时打开同一画布编辑页
2. 标签页 A 点击保存 → 成功
3. 标签页 B 修改内容后点击保存
4. 应弹出确认弹窗："画布已被他人修改 / 是否立即刷新？"
5. 点击"立即刷新" → 页面重载，显示最新内容
6. 点击"暂不刷新" → 弹窗关闭，编辑页保持不变

- [ ] **Step 4：Commit**

```bash
git add frontend/src/pages/canvas-editor/index.tsx
git commit -m "fix: show refresh-confirm modal on 409 concurrent edit conflict"
```
