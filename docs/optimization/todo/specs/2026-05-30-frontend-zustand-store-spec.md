# Spec: 前端状态架构重构 — Zustand Store

> **编号:** #13 | **严重度:** High | **类别:** 前端架构

## Problem

`EditorInner` 组件 2075 行，26 个 `useState`，所有状态/逻辑/UI 混在一起。无外部状态管理。

**核心问题：**
- 任何状态变更触发整个编辑器重渲染
- 选一个节点 → 全部 26 个 useState 重渲染 → 所有节点组件重渲染
- `displayNodes` 每次 `.filter()` 生成新引用，破坏 `memo()`
- Dagre 布局同步执行在主线程，100+ 节点画布卡顿数百毫秒

## Goal

提取 `useCanvasEditor` hook 封装状态+逻辑；节点选择等高频状态用 Zustand 细粒度订阅；Dagre 布局移入 Web Worker；Modal/Drawer 懒加载。

## Scope

### In Scope
- Zustand store 创建（canvasEditorStore）
- 细粒度订阅（selectedNodeId、nodes、edges 等独立 selector）
- `useCanvasEditor` hook 提取
- Dagre 布局移入 Web Worker
- Modal/Drawer 懒加载

### Out of Scope
- React Flow 替换（问题 I）
- 边双源真相修复（问题 #10，但有关联）

## Key Files

| File | ChangeType | Description |
|------|-------------|-------------|
| `canvas-editor/index.tsx` | Modify | 拆分状态到 Zustand |
| `useCanvasEditor.ts` | Create | hook 封装 |
| `canvasEditorStore.ts` | Create | Zustand store |
| `layoutWorker.ts` | Create | Dagre Web Worker |

## Success Criteria

1. EditorInner 行数减少 >50%
2. 选节点不触发全部组件重渲染
3. 100+ 节点画布布局不卡主线程
4. Modal/Drawer 懒加载
5. 所有现有功能不丢失