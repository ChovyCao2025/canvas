# Spec: MySQL 执行轨迹 → Doris OLAP

> **编号:** H | **严重度:** Medium | **迁移难度:** Medium

## Problem

MySQL 8.0 存储所有数据：画布定义、执行轨迹、DLQ、人群定义、发送记录等。执行轨迹是高吞吐追加写入的时序数据。

**核心问题：**
1. 表膨胀 → 索引退化 → 查询变慢
2. trace 查询与 OLTP 操作共享 DB → 互相影响
3. `TraceWriteBuffer` 50K 缓冲 + 丢数据降级是症状 — MySQL 扛不住写入量

## Goal

执行轨迹用 Apache Doris（或 ClickHouse/TimescaleDB），10-100x 写入吞吐 + 压缩比。MySQL 保留给 OLTP 数据。

## Scope

### In Scope
- Doris 部署（Docker compose）
- `TraceWriteBuffer` 增加 Doris sink
- trace 表 schema 迁移到 Doris
- trace 查询 API 改为查询 Doris
- 历史数据迁移策略

### Out of Scope
- 实时数仓（问题 L）
- 数据同步管道（问题 O）

## Key Files

| File | Change Type | Description |
|------|-------------|-------------|
| `TraceWriteBuffer.java` | Modify | 增加 Doris sink |
| `CanvasExecutionTraceMapper.java` | Modify | trace 写入改为 Doris |
| `CanvasStatsController.java` | Modify | trace 查询改为 Doris |
| `docker-compose.yml` | Modify | 添加 Doris 服务 |
| Doris DDL | Create | trace 表 schema |

## Success Criteria

1. trace 写入吞吐提升 10x+
2. trace 查询不影响 OLTP 操作
3. 历史数据有迁移路径
4. `TraceWriteBuffer` 丢数据降级机制可移除或降级为备份
