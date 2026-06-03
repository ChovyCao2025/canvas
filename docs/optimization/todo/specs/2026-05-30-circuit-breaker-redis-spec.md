# Spec: 熔断器状态外部化（Redis）

> **编号:** #6 | **严重度:** High | **类别:** 设计缺陷

## Problem

`CircuitBreakerRegistry` 是 JVM 本地 ConcurrentHashMap。多实例部署下熔断形同虚设。

**核心问题：**
- 实例 A 熔断 OPEN，实例 B 仍 CLOSED 放行
- 流量转到未熔断实例，加重外部服务压力
- 熔断保护失效

## Goal

熔断状态存 Redis，用 Lua 脚本保证原子性；或接受 JVM 本地熔断 + 全局限流兜底。

## Scope

### In Scope
- 熔断状态 → Redis（Lua 脚本原子操作）
- 熔断状态变更通知（Pub/Sub 或 MQ）
- 多实例熔断一致性保证

### Out of Scope
- DAG 引擎重构（问题 A+B）

## Key Files

| File | Change Type | Description |
|------|-------------|-------------|
| `CircuitBreakerRegistry.java` | Rewrite | Redis 存储替代 ConcurrentHashMap |
| `application.yml` | Modify | 熔断器 Redis 配置 |

## Success Criteria

1. 多实例熔断状态一致
2. 一个实例熔断，其他实例同步
3. Lua 脚本保证原子性
4. 熔断恢复也同步
