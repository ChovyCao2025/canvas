# Spec: API 层请求取消、去重、重试

> **编号:** #14 | **严重度:** Medium | **类别:** 前端架构

## Problem

`api.ts` 用裸 `axios.create({ baseURL: '/' })`，无 AbortController、无 cancel token、无请求去重、无重试。

**核心问题：**
- 组件卸载后 API 回调仍更新状态 → 内存泄漏/警告
- 同一数据被多个组件并发调用 → 重复网络请求
- 瞬时网络抖动 → 自动保存失败 → 用户工作丢失
- config-panel 缓存无 TTL → 后端 schema 变更后返回过期数据

## Goal

useEffect 清理函数中 abort 请求；引入请求去重；自动保存加入 retry with backoff；config-panel 缓存加 TTL。

## Scope

### In Scope
- axios 实例增加 AbortController 支持
- useEffect 清理函数 abort 请求
- 请求去重（相同 key 的并发请求合并）
- 自动保存 retry with exponential backoff
- config-panel 缓存加 TTL 和版本化失效

### Out of Scope
- 前端状态管理重构（问题 #13）
- 类型安全校验（问题 #15）

## Key Files

| File | Change Type | Description |
|------|-------------|-------------|
| `services/api.ts` | Modify | AbortController + 去重 + 重试 |
| `config-panel/index.tsx` | Modify | 缓存 TTL |

## Success Criteria

1. 组件卸载后 API 回调不更新状态
2. 相同 key 并发请求合并为一个
3. 自动保存有指数退避重试
4. config-panel 缓存有 TTL