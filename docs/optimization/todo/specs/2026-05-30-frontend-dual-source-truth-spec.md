# Spec: 前端边数据双源真相修复

> **编号:** #10 | **严重度:** High | **类别:** 前端架构

## Problem

边同时存在于两处：React Flow 的 `useEdgesState` 管理的边数组，以及每个节点的 `bizConfig` 中的出口引用字段（如 `yesNodeId`、`noNodeId`）。`deriveEdges()` 从后端节点重建边，之后每次修改需手动保持两处同步。

**核心问题：**
- 双源真相是 bug 温床
- 修改边需同时更新两处，遗漏任一处即产生界面与数据不一致
- undo/redo 用浅拷贝 `[...nodes]`，节点对象仍是同一引用
- 修改 `node.data.bizConfig` 会静默篡改历史快照

## Goal

以 `bizConfig` 为唯一真相源，React Flow 的边通过 `useMemo` 从 bizConfig 派生；undo/redo 需深拷贝或使用 immutable 数据结构。

## Scope

### In Scope
- 移除 `useEdgesState`，边通过 `useMemo` 从 bizConfig 派生
- 所有边修改操作只更新 bizConfig
- undo/redo 改为深拷贝或使用 immer
- 验证所有边同步代码已移除

### Out of Scope
- React Flow 替换（问题 I）
- 前端状态管理重构（问题 #13）

## Key Files

| File | Change Type | Description |
|------|-------------|-------------|
| `canvas-editor/index.tsx` | Modify | 移除 useEdgesState，边从 bizConfig 派生 |
| `outletRouting.ts` | Modify | 只更新 bizConfig |
| undo/redo 逻辑 | Modify | 深拷贝或 immer |

## Success Criteria

1. bizConfig 是边的唯一真相源
2. 边修改只更新 bizConfig，React Flow 边自动派生
3. undo/redo 不篡改历史快照
4. 无边同步代码残留
