# Spec: Wait 节点状态跨部署存活

> **编号:** #3 | **严重度:** High | **类别:** 设计缺陷

## Problem

Wait 节点订阅状态持久化到 MySQL，但超时定时器是 DagEngine 内存的 `Mono.delay()` 订阅。恢复锁 TTL = globalTimeoutSec（默认 600s），而 Wait 节点可能等待数小时甚至数天。

**核心问题：**
- 滚动部署时内存超时定时器消失，新实例无机制重建
- 恢复锁 10 分钟过期，Wait 节点等 3 天 → 锁过期后同一用户可触发第二次执行
- WAIT 恢复仍走全量 trigger 路径（含额度检查），是已知问题

## Goal

所有定时器外部化到 Redis sorted set 或 DB + polling；恢复锁 TTL 绑定 Wait 实际过期时间；WAIT 恢复短路跳过额度/去重检查。

## Scope

### In Scope
- 超时定时器 → Redis sorted set 延迟队列
- 恢复锁 TTL 绑定 Wait 实际过期时间
- WAIT 恢复短路：跳过额度检查和去重检查
- 定时器重建机制（实例启动时从 Redis 加载 pending timers）

### Out of Scope
- DAG 增量持久化（问题 #1，但有关联）
- WebFlux → MVC（问题 A+B）

## Key Files

| File | Change Type | Description |
|------|-------------|-------------|
| `DagEngine.java` | Modify | Mono.delay → Redis 延迟队列 |
| `WaitSubscriptionService.java` | Modify | 恢复锁 TTL 绑定 |
| `WaitResumeService.java` | Modify | 短路跳过额度/去重检查 |
| `ContextPersistenceService.java` | Modify | 定时器持久化 |

## Success Criteria

1. 滚动部署后 Wait 超时定时器可从 Redis 重建
2. 恢复锁 TTL 与 Wait 过期时间一致
3. WAIT 恢复不走全量 trigger 路径
4. 无重复触达
