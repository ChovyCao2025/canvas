# 方向⑱：数据导出与集成 — 功能清单

> 定位：从"数据封闭"升级为"数据可导出+可集成"——让画布数据能流向BI工具/数据仓库/第三方系统
> 策略评估：当前无任何导出能力，数据只能通过API查询；3-5人月可完成核心
> 竞品对标：Segment Reverse ETL、Fivetran Connector、Braze Data Export、神策数据导出
> 建议：**P2建议做**，数据消费刚需，对接BI是客户常见需求

---

## 现状盘点

| 功能 | 实现程度 | 核心现有代码 | 差距 |
|------|----------|-------------|------|
| API查询 | **完整** | CanvasController/CanvasStatsController/CdpUserController | 数据可通过API查询 |
| 执行记录查询 | **完整** | CanvasExecutionMapper | 执行记录可查 |
| 数据导出 | **不存在** | — | 完全缺失 |
| 定时导出 | **不存在** | — | 完全缺失 |
| BI对接 | **不存在** | — | 无Tableau/PowerBI连接器 |
| 数据仓库同步 | **不存在** | — | 无数据推送到外部仓库 |
| Webhook推送 | **不存在** | — | 方向⑪覆盖 |

### 关键洞察

客户常见需求：
1. **导出执行数据**：把画布执行记录导出为CSV/Excel做分析
2. **导出用户数据**：把用户画像/标签导出
3. **对接BI**：把数据推送到Tableau/PowerBI做可视化
4. **对接数据仓库**：把数据同步到客户自己的BigQuery/Redshift/Snowflake
5. **对接神策**：把画布触达事件推送到神策做分析

---

## 功能清单

### P0 — 数据导出核心

---

#### 1. 即时数据导出 [中复杂度 | 1.5人月]

**现状**：无导出能力

**需补齐**：

| 导出类型 | 描述 | 格式 |
|---------|------|------|
| 画布执行记录 | 按画布+时间范围导出执行记录 | CSV/Excel |
| 用户画像 | 导出用户画像+标签 | CSV/Excel |
| 人群用户 | 导出人群中的用户列表 | CSV/Excel |
| 效果指标 | 导出画布效果指标 | CSV/Excel |
| 事件日志 | 导出事件上报日志 | CSV/JSON |

**导出流程**：

```
1. 用户选择导出类型+筛选条件
2. 系统创建导出任务(ASYNC)
3. 异步查询数据+写入文件
4. 文件上传到对象存储
5. 通知用户下载
6. 7天后自动清理文件
```

**数据库DDL**：

```sql
CREATE TABLE data_export_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    export_type VARCHAR(30) NOT NULL COMMENT 'CANVAS_EXECUTION/USER_PROFILE/AUDIENCE_USERS/METRICS/EVENT_LOG',
    format VARCHAR(10) NOT NULL DEFAULT 'CSV' COMMENT 'CSV/XLSX/JSON',
    filters JSON NOT NULL COMMENT '筛选条件',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/RUNNING/COMPLETED/FAILED',
    total_rows INT COMMENT '总行数',
    file_key VARCHAR(500) COMMENT '文件存储Key',
    file_url VARCHAR(500) COMMENT '文件下载URL',
    file_size_bytes BIGINT COMMENT '文件大小',
    error_message VARCHAR(500) COMMENT '失败原因',
    expires_at DATETIME COMMENT '文件过期时间',
    requested_by VARCHAR(64) NOT NULL,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_status (status),
    INDEX idx_tenant (tenant_id),
    INDEX idx_requested (requested_by)
) COMMENT '数据导出任务';
```

---

#### 2. 定时数据导出 [中复杂度 | 1.0人月]

**现状**：无定时导出

**需补齐**：

| 子功能 | 描述 |
|--------|------|
| 定时任务 | 配置定时导出（每日/每周/每月） |
| 推送方式 | 导出文件推送到：对象存储/FTP/邮件 |
| 数据范围 | 每次导出增量数据（上次导出后新增的） |
| 失败重试 | 导出失败自动重试 |
| 任务管理 | 查看/暂停/删除定时导出任务 |

**数据库DDL**：

```sql
CREATE TABLE scheduled_export (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(200) NOT NULL,
    export_type VARCHAR(30) NOT NULL,
    format VARCHAR(10) NOT NULL DEFAULT 'CSV',
    filters JSON NOT NULL,
    cron VARCHAR(50) NOT NULL COMMENT 'Cron表达式',
    delivery_method VARCHAR(20) NOT NULL COMMENT 'S3/FTP/EMAIL',
    delivery_config JSON NOT NULL COMMENT '推送配置',
    incremental TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否增量',
    last_run_at DATETIME,
    last_run_status VARCHAR(20),
    next_run_at DATETIME,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_enabled (enabled),
    INDEX idx_next_run (next_run_at),
    INDEX idx_tenant (tenant_id)
) COMMENT '定时导出配置';
```

---

### P1 — BI与数据仓库集成

---

#### 3. BI连接器 [中复杂度 | 1.5人月]

**现状**：无BI对接

**需补齐**：

| 连接器 | 描述 | 接入方式 |
|--------|------|---------|
| Tableau | Tableau连接画布数据 | JDBC/ODBC数据源 |
| PowerBI | PowerBI连接画布数据 | DirectQuery/Import |
| Metabase | Metabase连接画布数据 | JDBC数据源 |
| Superset | Superset连接画布数据 | SQLAlchemy |

**BI数据模型**：

```
为BI提供预构建的数据视图(View)：

v_canvas_daily_metrics    — 画布日指标(发送/送达/打开/点击/转化)
v_canvas_execution_log    — 画布执行记录
v_user_profile            — 用户画像
v_user_tag                — 用户标签
v_message_delivery        — 消息投递记录
v_event_log               — 事件日志
```

---

#### 4. 数据仓库同步 [中复杂度 | 1.5人月]

**现状**：无数据仓库同步

**需补齐**：

| 目标仓库 | 描述 | 同步方式 |
|---------|------|---------|
| BigQuery | Google BigQuery | Streaming Insert |
| Redshift | AWS Redshift | COPY from S3 |
| Snowflake | Snowflake | COPY INTO |
| ClickHouse | ClickHouse | INSERT Batch |
| MySQL | 外部MySQL | INSERT Batch |

**同步架构**：

```
Canvas DB → CDC/Binlog → Kafka → Sync Consumer → 目标仓库
                                    ↓
                              Schema适配+类型转换
                                    ↓
                              Upsert(幂等)
```

**数据库DDL**：

```sql
CREATE TABLE data_warehouse_sync (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(200) NOT NULL,
    target_type VARCHAR(30) NOT NULL COMMENT 'BIGQUERY/REDSHIFT/SNOWFLAKE/CLICKHOUSE/MYSQL',
    connection_config JSON NOT NULL COMMENT '连接配置(加密)',
    sync_tables JSON NOT NULL COMMENT '同步表列表',
    sync_mode VARCHAR(20) NOT NULL DEFAULT 'INCREMENTAL' COMMENT 'FULL/INCREMENTAL',
    sync_interval INT NOT NULL DEFAULT 3600 COMMENT '同步间隔(秒)',
    last_sync_at DATETIME,
    last_sync_status VARCHAR(20),
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    INDEX idx_enabled (enabled),
    INDEX idx_tenant (tenant_id)
) COMMENT '数据仓库同步配置';
```

---

### P2 — 高级集成

---

#### 5. Reverse ETL [低复杂度 | 0.5人月]

**描述**：将画布数据推送到营销/CRM系统

| 目标系统 | 描述 |
|---------|------|
| Salesforce | 画布触达事件推送到Salesforce Activity |
| HubSpot | 画布触达事件推送到HubSpot Contact Timeline |
| 神策 | 画布触达事件作为神策自定义事件 |

---

#### 6. 数据API [低复杂度 | 0.5人月]

**描述**：提供标准化的数据查询API（供BI/数据团队使用）

| API | 描述 |
|-----|------|
| /data/v1/metrics | 画布指标查询(支持SQL-like过滤) |
| /data/v1/executions | 执行记录查询(支持分页+排序) |
| /data/v1/users | 用户画像查询(支持标签过滤) |
| /data/v1/events | 事件日志查询(支持时间范围) |

---

## 工作量估算

| 优先级 | 功能 | 后端人月 | 前端人月 | 测试人月 | 总计 |
|--------|------|---------|---------|---------|------|
| P0 | 即时数据导出 | 1.0 | 0.5 | 0.2 | 1.7 |
| P0 | 定时数据导出 | 0.7 | 0.3 | 0.2 | 1.2 |
| P1 | BI连接器 | 1.0 | 0.5 | 0.2 | 1.7 |
| P1 | 数据仓库同步 | 1.0 | 0.5 | 0.2 | 1.7 |
| P2 | Reverse ETL | 0.3 | 0.2 | 0.1 | 0.6 |
| P2 | 数据API | 0.3 | 0.2 | 0.1 | 0.6 |
| | **合计** | **4.3** | **2.2** | **1.0** | **7.5** |

---

## 执行顺序

```
Sprint 1 (P0-导出): 即时+定时导出 — 2.9人月
  → 产出：CSV/Excel导出+定时推送

Sprint 2 (P1-BI): BI连接器+数据视图 — 1.7人月
  → 产出：Tableau/PowerBI对接

Sprint 3 (P1-仓库): 数据仓库同步 — 1.7人月
  → 产出：BigQuery/Redshift/ClickHouse同步

Sprint 4 (P2-高级): Reverse ETL+数据API — 1.2人月
  → 产出：CRM推送+数据查询API
```

---

## 风险评估

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| 导出数据量大 | 导出任务执行时间长 | 异步+分页+流式写入 |
| 敏感数据泄露 | 导出含手机号/邮箱等敏感信息 | 脱敏选项+权限控制+审计日志 |
| 仓库同步延迟 | CDC延迟导致数据不一致 | 监控延迟+告警+手动全量同步 |
| Schema变更 | 画布表结构变更影响BI报表 | 版本化View+变更通知 |

---

## 与其他方向的关系

| 方向 | 与⑱的关系 |
|------|----------|
| ⑨ 营销数据中台 | 指标数据是导出的重要内容 |
| ⑬ 用户画像 | 用户画像导出是常见需求 |
| ⑪ 开放平台 | 数据API是开放API的一部分 |
| ⑫ 多租户 | 导出按租户隔离+配额 |
