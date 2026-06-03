# Spec: 缓存失效保证 — L1 脏数据修复

> **编号:** #16 | **严重度:** Medium | **类别:** 缓存与审计

## Problem

TieredCacheImpl 的 L1 失效依赖 MQ 异步发布事件。MQ 临时不可用 → 失效事件丢失 → 其他实例 L1 缓存脏数据直到 Caffeine TTL 过期。`safeWrite` 用 `Thread.sleep` 做双删延迟。`staleValues` 是 JVM 本地 ConcurrentHashMap。

**核心问题：**
- MQ 不可用时 L1 缓存可能长期脏
- `Thread.sleep` 阻塞调用线程
- 各实例 stale value 降级结果不一致

## Goal

MQ 不可用时降级为同步 Redis 版本校验；`safeWrite` 改用异步延迟任务；stale value 可考虑接受（降级一致性）。

## Scope

### In Scope
- MQ 失效失败时降级为 Redis 版本校验
- `safeWrite` 的 `Thread.sleep` → 异步延迟任务
- stale value 一致性策略确定

### Out of Scope
- TieredCache 整体重构
- Redis 架构变更

## Key Files

| File | Change Type | Description |
|------|-------------|-------------|
| `TieredCacheImpl.java` | Modify | 失效降级 + 异步延迟 |

## Success Criteria

1. MQ 不可用时 L1 脏数据有兜底机制
2. `safeWrite` 不阻塞调用线程
3. stale value 策略明确