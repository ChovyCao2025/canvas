# Spec: 数据基建 — 离线数仓 + 实时数仓 + 实时计算 + 数据管道

> **编号:** K+L+M+O | **严重度:** High | **迁移难度:** Hard
> **四者互为因果，统一方案：** MySQL → Flink CDC 3.6 → Kafka → Doris 4.0

## Problem

MySQL 单库扛全部数据需求。无 ClickHouse/Hive/Doris/StarRocks，无 ETL 管道，无数仓分层。

**核心问题：**
- **K-无离线数仓：** 无历史趋势分析，无分区事实表，无数据归档
- **L-无实时数仓：** 全表加载进 JVM + Java Stream 聚合，分析查询打主库
- **M-无实时计算：** 无 CEP/状态后端/容错，单 JVM 吞吐上限
- **O-无数据管道：** 运营库与分析库无隔离，无 binlog 消费

## Goal

**统一架构：** MySQL（OLTP）→ Flink CDC 3.6（binlog 消费 + schema evolution）→ Kafka（数据总线）→ Doris 4.0（OLAP）

- **K:** Doris 做离线数仓，ODS/DWS/ADS 分层
- **L:** Flink CDC → Doris 实时同步，报表查询走 Doris
- **M:** Flink CEP 做实时事件处理
- **O:** Flink CDC 统一数据管道

## Scope

### In Scope
- Doris 4.0 集群部署（Docker compose）
- Flink CDC 3.6 部署
- MySQL → Doris 全库同步管道
- 数仓分层（ODS/DWS/ADS）
- `CanvasStatsController` 改为查询 Doris
- `HomeOverviewController` 改为查询 Doris
- `TraceWriteBuffer` 增加 Kafka/Doris sink
- Flink CEP 作业（行为触发/事件触发评估）

### Out of Scope
- Trace 单独迁移到 Doris（问题 H，可合并）
- 服务拆分（问题 C）

## Key Files

| File | Change Type | Description |
|------|-------------|-------------|
| `docker-compose.yml` | Modify | 添加 Doris、Flink 服务 |
| `CanvasStatsController.java` | Modify | 查询 Doris |
| `HomeOverviewController.java` | Modify | 查询 Doris |
| `TraceWriteBuffer.java` | Modify | Kafka/Doris sink |
| Flink CDC YAML | Create | MySQL→Doris 同步管道 |
| Flink CEP jobs | Create | 实时事件处理 |
| Doris DDL | Create | ODS/DWS/ADS 表 |

## Success Criteria

1. MySQL binlog 实时同步到 Doris（秒级延迟）
2. 报表查询走 Doris，MySQL 只服务 OLTP
3. Java Stream 聚合替换为 Doris SQL GROUP BY
4. 行为触发/事件触发通过 Flink CEP 评估
5. 历史数据有归档和 TTL 策略
