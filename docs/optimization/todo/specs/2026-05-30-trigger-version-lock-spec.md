# Spec: 触发器版本锁定

> **编号:** #8 | **严重度:** High | **类别:** 设计缺陷

## Problem

触发时从 Caffeine L1 缓存获取画布图结构，但版本解析和图加载之间无锁。画布编辑/重新发布可能导致版本切换。

**核心问题：**
- 重新发布画布时，正在执行的旧版本图可能被淘汰
- 恢复执行加载到新版本图 → DAG 拓扑变化 → 执行结果不可预测
- Wait 恢复时如原版本被删除，缓存返回 null 导致空指针

## Goal

触发时锁定版本，执行全程使用已锁定的版本；旧版本图保留至所有引用它的执行完成。

## Scope

### In Scope
- 触发时记录版本 ID，执行全程使用该版本
- 旧版本图保留策略（引用计数或 TTL）
- Wait 恢复校验版本一致性
- 版本缓存 key 包含版本 ID

### Out of Scope
- DAG 引擎重构（问题 A+B）
- 缓存失效策略（问题 #16）

## Key Files

| File | Change Type | Description |
|------|-------------|-------------|
| `CanvasExecutionService.java` | Modify | 触发时锁定版本 |
| `ContextPersistenceService.java` | Modify | context 包含版本 ID |
| 版本保留策略 | Create | 旧版本保留机制 |

## Success Criteria

1. 执行全程使用触发时锁定的版本
2. 旧版本图不被提前淘汰
3. Wait 恢复校验版本一致性
4. 版本不一致时明确报错而非空指针
