# Spec: 受众用户流式加载（消除大人群 OOM）

> **编号:** #2 | **严重度:** Critical | **类别:** 设计缺陷

## Problem

AudienceUserResolver 的 `toUid()` 方法通过 JDBC 查询将所有匹配用户 ID 加载进 `ArrayList<String>`。无分页、无流式处理。

**核心问题：**
- 500 万用户人群包 → ArrayList 占 ~200MB+ 堆内存
- 3000 并发执行上限下，多个大人群包同时解析可耗尽 JVM 堆
- murmur3_32 将 userId 映射为 32 位 int，500 万用户碰撞率 ~0.5%

## Goal

人群解析改为流式/分页，直接走 RoaringBitmap 运算而非中间 List；fan-out 触发分批限流。

## Scope

### In Scope
- `toUid()` 改为流式处理（JDBC ResultSet cursor 或分页查询）
- fan-out 触发分批提交（避免瞬间打满 buffer）
- 与 RoaringBitmap 碰撞修复（问题 G）联动

### Out of Scope
- RoaringBitmap 碰撞修复本身（问题 G）
- 服务拆分（问题 C）

## Key Files

| File | Change Type | Description |
|------|-------------|-------------|
| `AudienceUserResolver.java` | Rewrite | 流式/分页加载 |
| `AudienceBatchComputeService.java` | Modify | 适配流式处理 |
| `DagEngine.java` | Modify | fan-out 分批限流 |

## Success Criteria

1. 人群解析不再全量加载到内存
2. 500 万用户人群包内存占用 < 50MB
3. fan-out 分批提交，避免瞬间打满
4. 并发执行不受单个大人群包影响
