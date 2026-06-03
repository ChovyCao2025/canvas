# Spec: 画布级租户隔离（防饿死）

> **编号:** #9 | **严重度:** High | **类别:** 设计缺陷

## Problem

车道限制按类型配置，但无画布维度的准入配额。热门画布可占满所有 slot，阻塞其他画布。

**核心问题：**
- 爆款活动画布流量尖峰可导致整个引擎不可用
- Disruptor ring buffer 全局共享，一种触发类型洪峰挤占全部 buffer
- overflow retry 链在持续过载下本身也成瓶颈

## Goal

增加画布维度 admission rate limit；Disruptor 按优先级或类型分 ring buffer（如已替换为 ThreadPool 则分区线程池）；overflow retry 需要总次数上限 + 终极 DLQ。

## Scope

### In Scope
- 画布级 rate limiter（如 Guava RateLimiter 或 Redis 令牌桶）
- 线程池/队列分区（按画布优先级或大小）
- overflow retry 总次数上限 + 终极 DLQ
- 画布级执行配额配置

### Out of Scope
- Disruptor 替换（问题 D）
- 服务拆分（问题 C）

## Key Files

| File | Change Type | Description |
|------|-------------|-------------|
| `CanvasExecutionService.java` | Modify | 画布级 rate limit |
| `InFlightExecutionRegistry.java` | Modify | 画布维度计数 |
| `application.yml` | Modify | 画布级配额配置 |

## Success Criteria

1. 单画布无法占满所有执行 slot
2. 画布级 rate limit 可配置
3. overflow retry 有总次数上限
4. 终极 DLQ 保证不丢请求
