# Spec: Handler 系统级幂等框架

> **编号:** #5 | **严重度:** High | **类别:** 设计缺陷

## Problem

DagEngine 的 repeat/retry 机制可让同一 handler 执行多次，但无系统级幂等 key 传递给 handler。各 handler 需独立实现幂等，无统一保证。handler 执行失败后可能已部分写入 ctx，污染下游判断。

**核心问题：**
- 未实现幂等的 handler 在 retry/repeat 时产生重复副效果
- 部分失败 + ctx 污染可能导致防资损逻辑误判

## Goal

引入系统级 `idempotencyKey = executionId + nodeId + attemptCount`，在 handler 执行前持久化，执行后标记完成；ctx 写入在 handler 成功后原子性提交。

## Scope

### In Scope
- 幂等 key 生成和传递机制
- 执行前持久化幂等 key（Redis）
- 执行后标记完成
- ctx 写入原子性（handler 成功后一次性提交）
- 幂等 key 检查：重复执行时跳过

### Out of Scope
- DAG 引擎重构（问题 A+B）
- 各 handler 具体幂等逻辑（已有如 ReachDeliveryService.idempotencyKey 保留）

## Key Files

| File | Change Type | Description |
|------|-------------|-------------|
| `DagEngine.java` | Modify | 幂等 key 生成 + 检查 + 标记 |
| `NodeHandler.java` | Modify | 接口增加幂等 key 参数 |
| `ExecutionContext.java` | Modify | 延迟写入（handler 成功后提交） |
| `IdempotencyService.java` | Create | 幂等 key 持久化服务 |

## Success Criteria

1. 所有 handler 执行前有幂等 key 检查
2. 重复执行时跳过已完成的副效果
3. ctx 写入在 handler 成功后原子性提交
4. 部分失败不污染下游判断
