# Spec: 保存流程 while(true) 修复 + 重试机制

> **编号:** #12 | **严重度:** High | **类别:** 前端架构

## Problem

`handleSave` 用 `savingPromiseRef` 合并并发保存，核心逻辑是 `while(true)` 循环：如果保存期间快照变化则重新保存。自动保存失败仅弹 `message.error`，不重试。

**核心问题：**
- 用户持续快速编辑快于保存速度 → while 循环无限运行
- 网络故障 → 自动保存失败 → 仅提示用户 → 3 秒后重试 → 又失败 → 中间编辑仅存内存
- 409 冲突处理是破坏性的（丢失一方全部工作）

## Goal

`while(true)` 循环加最大迭代次数；保存失败加入指数退避重试；409 冲突提供 diff/merge UI。

## Scope

### In Scope
- while(true) → while(iteration < MAX_ITERATIONS)
- 自动保存失败加入指数退避重试
- 409 冲突提供 diff/merge UI（与 #11 合并）

### Out of Scope
- 前端状态管理重构（问题 #13）
- 并发编辑 CRDT（远期目标）

## Key Files

| File | ChangeType | Description |
|------|-------------|-------------|
| `canvas-editor/index.tsx` | Modify | while 限制 + 重试 |

## Success Criteria

1. 保存循环不会无限运行
2. 网络故障有指数退避重试
3. 用户编辑不因保存失败而丢失