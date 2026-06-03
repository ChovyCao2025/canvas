# Spec: 前端编辑器实时更新

> **编号:** #11 | **严重度:** High | **类别:** 前端架构

## Problem

画布编辑器仅在挂载时从服务端加载一次数据，之后无任何 WebSocket/SSE/轮询机制。

**核心问题：**
- 发布画布后无法看到执行状态更新
- 两人同时编辑画布只有 409 冲突时才知道
- 金丝雀推广/回滚后需 `window.location.reload()` 丢失全部内存状态

## Goal

增加执行事件的 SSE/WebSocket 推送通道；并发编辑至少需要 operational transform 或 CRDT 意识；409 冲突改为 diff/merge UI。

## Scope

### In Scope
- SSE/WebSocket 推送通道（执行状态变更）
- 并发编辑感知（至少显示"其他人正在编辑"提示）
- 409 冲突改为 diff/merge UI

### Out of Scope
- CRDT/OT 完整实现（远期目标）
- 前端状态管理重构（问题 #13）

## Key Files

| File | Change Type | Description |
|------|-------------|-------------|
| `canvas-editor/index.tsx` | Modify | SSE/WebSocket 订阅 |
| 后端 WebSocket endpoint | Create | 推送执行状态变更 |
| 409 冲突处理 | Modify | diff/merge UI |

## Success Criteria

1. 执行状态变更实时推送到编辑器
2. 并发编辑有感知和提示
3. 409 冲突不丢失任何一方的工作
