# Spec: DAG 执行状态增量持久化

> **编号:** #1 | **严重度:** Critical | **类别:** 设计缺陷

## Problem

DagEngine 是纯内存 Reactor Mono 驱动的执行引擎。ExecutionContext 全程驻留 JVM 内存，仅在执行完成或暂停时写入 Redis。NodeGate 锁、Hub/LogicRelation 超时定时器、`scheduledHubTimeouts` 集合均为内存对象。

**核心问题：**
- 进程崩溃/重启/滚动部署时，两个节点之间所有中间状态丢失
- 已完成但有副作用的节点可能被重新执行 → 资损
- NodeGate 全部重建为 false → 触发重复执行
- 超时定时器消失 → 依赖超时恢复的节点永久 WAITING

## Goal

每个节点执行完成后，将 `nodeStatuses` + `nodeOutputs` 增量持久化到 Redis；NodeGate 和超时定时器外部化。

## Scope

### In Scope
- 节点执行完成 → 增量持久化 nodeStatuses + nodeOutputs 到 Redis
- NodeGate → Redis SETNX（原子性 + 可序列化）
- 超时定时器 → Redis sorted set 做延迟队列
- 恢复逻辑：从 Redis 加载状态重建执行上下文

### Out of Scope
- WebFlux → MVC（问题 A+B）
- Wait 节点跨部署存活（问题 #3，但有关联）

## Key Files

| File | Change Type | Description |
|------|-------------|-------------|
| `DagEngine.java` | Modify | 节点完成后增量持久化 |
| `ExecutionContext.java` | Modify | NodeGate 可序列化 |
| `ContextPersistenceService.java` | Modify | 增量持久化方法 |
| Redis delay queue | Create | 替代 Mono.delay 超时定时器 |

## Success Criteria

1. 进程崩溃后恢复不产生重复副作用
2. NodeGate 状态可从 Redis 重建
3. 超时定时器可从 Redis 重建
4. 增量持久化不影响正常执行性能（异步写入）
