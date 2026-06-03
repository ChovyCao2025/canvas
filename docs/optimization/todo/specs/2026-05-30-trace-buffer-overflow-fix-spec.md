# Spec: 审计轨迹可靠性 — Trace 缓冲区溢出修复

> **编号:** #17 | **严重度:** Medium | **类别:** 缓存与审计

## Problem

TraceWriteBuffer 50,000 条容量，500ms 刷新周期，每周期最多处理 4000 条（20 批 × 200）。batch insert 失败仅 log 不重试。Spring `@Scheduled` 共享单线程调度器。

**核心问题：**
- 3000 并发 × 5 条 trace/执行 = 15,000 条/秒，缓冲区秒级填满，大多数 trace 被静默丢弃
- 审计数据不可靠，故障排查无据可依
- Context 保存/恢复等关键事件完全未 trace

## Goal

刷新线程独立化不依赖 Spring scheduler；增加 backpressure 机制；关键事件必须 trace；考虑异步 MQ 写入替代直接 DB 写。

## Scope

### In Scope
- 刷新线程独立化（自有 ScheduledExecutorService）
- backpressure：缓冲区满时阻塞或降级为采样
- 关键事件（ctx save/resume、投递请求）必须 trace
- 考虑 MQ 异步写入替代 DB 直写

### Out of Scope
- trace 迁移到 Doris（问题 H，但有关联）
- 数据管道（问题 O）

## Key Files

| File | Change Type | Description |
|------|-------------|-------------|
| `TraceWriteBuffer.java` | Modify | 独立刷新 + backpressure + 关键事件 |

## Success Criteria

1. trace 不会被静默丢弃
2. 关键事件（ctx save/resume）保证 trace
3. 缓冲区满时有明确降级策略
4. 刷新不依赖 Spring scheduler