# Dirty 状态修复 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 保存后再次修改画布时，保存按钮正确切换回"草稿*"状态。

**Architecture:** 在 `canvas-editor/index.tsx` 三处缺失 `setIsDirty(true)` 的变更回调里补上调用，并将裸 `onEdgesChange` 包装成 `onEdgesChangeWrapped` 过滤无意义变更。

**Tech Stack:** React 18, TypeScript, ReactFlow

---

## File Map

| File | Change |
|------|--------|
| `frontend/src/pages/canvas-editor/index.tsx` | 新增 `onEdgesChangeWrapped`；`onNodeDataChange` 和名称 onChange 各补一行 `setIsDirty(true)` |

---

## Task 1: 包装 onEdgesChange

**Files:**
- Modify: `frontend/src/pages/canvas-editor/index.tsx`

- [ ] **Step 1: 在 `onNodesChangeWrapped` 定义之后（约 L466）新增 `onEdgesChangeWrapped`**

```typescript
const onEdgesChangeWrapped = useCallback((changes: EdgeChange[]) => {
  onEdgesChange(changes)
  const significant = changes.some(c => c.type === 'add' || c.type === 'remove')
  if (significant) setIsDirty(true)
}, [onEdgesChange])
```

- [ ] **Step 2: 将 ReactFlow 的 `onEdgesChange` prop 改为 `onEdgesChangeWrapped`**

找到约 L924：
```tsx
onEdgesChange={onEdgesChange}
```
改为：
```tsx
onEdgesChange={onEdgesChangeWrapped}
```

- [ ] **Step 3: 验证 TypeScript 无错误**

```bash
cd frontend && npx tsc --noEmit
```

Expected: 无输出（无错误）

- [ ] **Step 4: 手动测试——连线变更触发 dirty**

启动 dev server（`npm run dev`），打开任意画布 → 点击保存（按钮变"已保存"）→ 拖一条连线 → 按钮应立即变回"草稿*"。

- [ ] **Step 5: Commit**

```bash
git add frontend/src/pages/canvas-editor/index.tsx
git commit -m "fix: wrap onEdgesChange to set isDirty on add/remove"
```

---

## Task 2: onNodeDataChange 补充 dirty 标记

**Files:**
- Modify: `frontend/src/pages/canvas-editor/index.tsx`

- [ ] **Step 1: 在 `onNodeDataChange` 里补 `setIsDirty(true)`**

找到约 L637-641，将：
```typescript
const onNodeDataChange = useCallback((nid: string, patch: Partial<CanvasNodeData>) => {
  setNodes(prev => prev.map(n =>
    n.id === nid ? { ...n, data: { ...n.data as CanvasNodeData, ...patch } } : n
  ))
}, [setNodes])
```
改为：
```typescript
const onNodeDataChange = useCallback((nid: string, patch: Partial<CanvasNodeData>) => {
  setNodes(prev => prev.map(n =>
    n.id === nid ? { ...n, data: { ...n.data as CanvasNodeData, ...patch } } : n
  ))
  setIsDirty(true)
}, [setNodes])
```

- [ ] **Step 2: 手动测试——节点配置修改触发 dirty**

启动 dev server → 打开画布 → 点击保存 → 点击任意节点 → 修改配置面板中的任意字段 → 按钮应立即变回"草稿*"。

- [ ] **Step 3: Commit**

```bash
git add frontend/src/pages/canvas-editor/index.tsx
git commit -m "fix: set isDirty on node data config change"
```

---

## Task 3: 画布名称修改补充 dirty 标记

**Files:**
- Modify: `frontend/src/pages/canvas-editor/index.tsx`

- [ ] **Step 1: 在画布名称输入框的 onChange 里补 `setIsDirty(true)`**

找到约 L718-719，将：
```tsx
onChange={e => setCanvasName(e.target.value)}
```
改为：
```tsx
onChange={e => { setCanvasName(e.target.value); setIsDirty(true) }}
```

- [ ] **Step 2: 手动测试——名称修改触发 dirty**

启动 dev server → 打开画布 → 点击保存 → 修改顶部画布名称 → 按钮应立即变回"草稿*"。

- [ ] **Step 3: 回归测试——保存后按钮正确重置**

修改任意内容 → 点击保存 → 按钮应变回"已保存" → 再次修改 → 按钮应变回"草稿*"。

- [ ] **Step 4: Commit**

```bash
git add frontend/src/pages/canvas-editor/index.tsx
git commit -m "fix: set isDirty on canvas name change"
```
