# Spec: RoaringBitmap 哈希碰撞修复

> **编号:** G | **严重度:** High | **迁移难度:** Medium

## Problem

RoaringBitmap 存 Redis（Base64 编码），用 murmur3_32 将 String userId 映射为 int 索引。

**核心问题：**
1. 哈希碰撞 → 误触达 — 1 亿用户下碰撞率 ~1.16%，错误纳入不属该人群的用户（合规风险）
2. 单用户检查需全量反序列化 — `isMember()` 从 Redis 加载整个 bitmap
3. Base64 编码浪费 33% 存储 — Redis 支持二进制安全 String
4. 不支持运行时集合运算 — "人群 A AND NOT 人群 B" 需加载两个完整 bitmap

## Goal

用确定性 userId-to-integer 映射替代哈希，消除碰撞。单用户检查用 Redis `GETBIT`（O(1)）。集合运算用 Redis `BITOP`。

## Scope

### In Scope
- userId→integer 确定性映射（Redis INCR 自增或 DB 序列）
- `AudienceBitmapStore` 接口实现替换
- Redis GETBIT 单用户检查
- Redis BITOP 集合运算
- Base64 → 二进制存储
- 存量 bitmap 迁移策略

### Out of Scope
- 人群计算服务拆分（问题 C）
- 数据基建（问题 K/L/M/O）

## Key Files

| File | Change Type | Description |
|------|-------------|-------------|
| `AudienceBitmapStore.java` | Rewrite | 确定性映射 + GETBIT + BITOP |
| `AudienceUserResolver.java` | Modify | 适配新映射方式 |
| `AudienceBatchComputeService.java` | Modify | 适配新存储格式 |
| Redis key schema | Modify | 新增 userId→integer 映射 key |

## Success Criteria

1. 零哈希碰撞（确定性映射）
2. 单用户检查 O(1)，零反序列化
3. 集合运算用 Redis BITOP，无需应用层反序列化
4. 存量 bitmap 有迁移路径
5. 存储空间减少 ~33%（去除 Base64）
