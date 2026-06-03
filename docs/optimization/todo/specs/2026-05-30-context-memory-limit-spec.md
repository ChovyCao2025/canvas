# Spec: 执行上下文内存强制上限 + Key 命名空间

> **编号:** #4 | **严重度:** High | **类别:** 设计缺陷

## Problem

ExecutionContext 有 1MB `approxSizeBytes` 估算和 `isOversized()` 方法，但 DagEngine 从不检查。`putNodeOutput()` 始终接受数据。key 无命名空间隔离，后执行的节点输出静默覆盖同名 key。

**核心问题：**
- 1MB 限制形同虚设
- 3000 并发 × 超 1MB context = 3GB+ JVM 内存
- key 碰撞导致节点间数据污染

## Goal

`isOversized()` 在 `putNodeOutput` 入口强制检查并拒绝/降级；key 加命名空间前缀；大小估算考虑序列化后实际大小。

## Scope

### In Scope
- `putNodeOutput()` 入口强制检查 `isOversized()`
- key 加命名空间前缀（`nodeId.keyName`）
- 大小估算改进（考虑嵌套对象开销）
- 超限策略：拒绝写入 + 降级为引用

### Out of Scope
- DAG 引擎重构（问题 A+B）

## Key Files

| File | Change Type | Description |
|------|-------------|-------------|
| `ExecutionContext.java` | Modify | 强制上限 + 命名空间 + 估算改进 |
| `DagEngine.java` | Modify | 处理超限降级 |

## Success Criteria

1. `isOversized()` 在 `putNodeOutput` 入口强制生效
2. key 碰撞不再发生（命名空间隔离）
3. 大小估算更准确
4. 超限不导致 OOM
