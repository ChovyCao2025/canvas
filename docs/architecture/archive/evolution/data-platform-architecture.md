# 数据中台架构方案 (2026-06-01)

> **定位**: 补齐数据中台+大数据基础设施，支撑实时/离线人群计算、标签加工、用户画像、数据治理

---

## 一、为什么需要数据中台

### 1.1 当前数据处理的四个致命瓶颈

| 问题 | 代码位置 | 影响 |
|------|---------|------|
| **内存计算无法扩展** | `CdpAudienceSourceService.resolveUserIds()` 加载全量标签/身份到内存 | 用户量>100万时OOM |
| **无增量更新** | `AudienceBatchComputeService.compute()` 每次全量重建Bitmap | 人群计算耗时线性增长 |
| **无实时标签** | TaggerRealtime 仍依赖WebClient同步调用 | 标签变更延迟>30s |
| **数据孤岛** | CDP数据仅存MySQL，无法对接BI/ML平台 | 数据分析靠手动导出 |

### 1.2 数据中台解决的业务问题

```
当前痛点                          数据中台方案
─────────────────────────────────────────────────────────
人群计算每次全量扫MySQL          → Flink增量更新Bitmap，小时级→秒级
标签计算在Java内存中             → ClickHouse OLAP查询，100ms内
用户画像无时间维度                → Iceberg快照，支持任意时间点回溯
数据分析靠手动SQL                → ADS层+数据API，BI工具直连
外部数据无法接入                  → CDC+Kafka统一数据管道
数据质量无法保证                  → 数据治理(质量监控+血缘+目录)
```

---

## 二、数据中台整体架构

### 2.1 五层架构

```
┌──────────────────────────────────────────────────────────────────────────┐
│                          数据应用层 (Data Applications)                    │
│                                                                          │
│  ┌───────────┐  ┌───────────┐  ┌───────────┐  ┌───────────┐            │
│  │ 人群计算   │  │ 标签加工   │  │ 用户画像   │  │ BI看板    │            │
│  │ Audience   │  │ Tag Engine │  │ Profile360│  │ Dashboard │            │
│  └───────────┘  └───────────┘  └───────────┘  └───────────┘            │
│  ┌───────────┐  ┌───────────┐  ┌───────────┐  ┌───────────┐            │
│  │ 行为分析   │  │ 预测模型   │  │ A/B实验   │  │ 推荐引擎   │            │
│  │ Event OLAP │  │ ML Serving │  │ Experiment │  │ Recommend │            │
│  └───────────┘  └───────────┘  └───────────┘  └───────────┘            │
└──────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌──────────────────────────────────────────────────────────────────────────┐
│                       数据服务层 (Data API Services)                       │
│                                                                          │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐       │
│  │ Query API        │  │ Metadata API     │  │ Export API       │       │
│  │ (GraphQL/REST)   │  │ (数据目录/血缘)  │  │ (CSV/Parquet)    │       │
│  └──────────────────┘  └──────────────────┘  └──────────────────┘       │
└──────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌──────────────────────────────────────────────────────────────────────────┐
│                          数据计算层 (Compute Engine)                       │
│                                                                          │
│  ┌───────────────────────────┐  ┌───────────────────────────┐           │
│  │ 实时计算 (Apache Flink)    │  │ 离线计算 (Apache Spark)    │           │
│  │                           │  │                           │           │
│  │ • 人群Bitmap增量更新      │  │ • 全量人群重建 (每日)     │           │
│  │ • 标签实时打标            │  │ • 标签聚合统计            │           │
│  │ • 事件窗口聚合            │  │ • 用户画像宽表构建        │           │
│  │ • 实时特征计算            │  │ • 漏斗/留存分析           │           │
│  │ • 规则引擎实时评估        │  │ • RFM模型计算             │           │
│  └───────────────────────────┘  └───────────────────────────┘           │
└──────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌──────────────────────────────────────────────────────────────────────────┐
│                           数据存储层 (Storage)                             │
│                                                                          │
│  ┌──────────┐  ┌───────────┐  ┌───────────┐  ┌───────────┐            │
│  │ Kafka    │  │ Iceberg   │  │ ClickHouse│  │ Redis     │            │
│  │ (消息)   │  │ (数据湖)  │  │ (OLAP)    │  │ (Bitmap)  │            │
│  │ 3 broker │  │ ODS/DWD   │  │ DWS/ADS   │  │ 热数据    │            │
│  └──────────┘  └───────────┘  └───────────┘  └───────────┘            │
│  ┌──────────┐  ┌───────────┐  ┌───────────┐                           │
│  │ MySQL    │  │ MinIO/S3  │  │ PostgreSQL│                           │
│  │ (元数据) │  │ (文件存储) │  │ (治理元数据)│                          │
│  └──────────┘  └───────────┘  └───────────┘                           │
└──────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌──────────────────────────────────────────────────────────────────────────┐
│                         数据集成层 (Data Ingestion)                        │
│                                                                          │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐       │
│  │ CDC (Canal)       │  │ API Pull         │  │ File/S3 Import   │       │
│  │ canvas_db→Kafka   │  │ 企微/第三方API   │  │ CSV/JSON/Parquet │       │
│  │ cdp_db→Kafka      │  │ 定时拉取         │  │ 批量导入         │       │
│  └──────────────────┘  └──────────────────┘  └──────────────────┘       │
└──────────────────────────────────────────────────────────────────────────┘
```

### 2.2 技术选型

| 层级 | 技术 | 选型理由 |
|------|------|---------|
| **数据集成** | Canal + Kafka Connect | 原生MySQL CDC，低延迟，已熟悉RocketMQ体系 |
| **消息队列** | Kafka 3.6 | 大数据生态标准，支持精确一次语义 |
| **数据湖** | Apache Iceberg | 支持ACID、时间旅行、Schema演进、隐藏分区 |
| **OLAP** | ClickHouse | 列存、压缩比高、亚秒级OLAP查询 |
| **实时计算** | Apache Flink 1.18 | 精确一次、状态后端RocksDB、SQL化开发 |
| **离线计算** | Apache Spark 3.5 | 生态成熟，支持Iceberg直接读写 |
| **对象存储** | MinIO (开发) / S3 (生产) | S3兼容，Iceberg原生支持 |
| **数据治理** | DataHub | 元数据管理+血缘+数据质量 |
| **调度** | DolphinScheduler | 可视化DAG调度，替代Cron |
| **数据服务** | 自研 QueryService | 轻量，按需查询+缓存 |

---

## 三、数据仓库分层设计

### 3.1 分层规范

```
┌─────────────────────────────────────────────────────────────────┐
│  ADS (Application Data Service) — 应用数据服务层                │
│  • 面向具体应用的高度聚合表                                     │
│  • 人群包结果表、标签宽表、画像卡片、BI报表                      │
│  • 存储: ClickHouse (查询) + Redis (热数据)                     │
├─────────────────────────────────────────────────────────────────┤
│  DWS (Data Warehouse Summary) — 汇总层                          │
│  • 轻度汇总、主题宽表                                           │
│  • 用户日活宽表、标签汇总表、行为聚合表                          │
│  • 存储: ClickHouse                                             │
├─────────────────────────────────────────────────────────────────┤
│  DWD (Data Warehouse Detail) — 明细层                           │
│  • 清洗后的明细数据、维度退化、JSON展开                          │
│  • 事件明细、身份明细、标签变更明细                              │
│  • 存储: Iceberg (数据湖)                                       │
├─────────────────────────────────────────────────────────────────┤
│  ODS (Operational Data Store) — 贴源层                          │
│  • 原始数据，与业务库保持一致                                   │
│  • 全量表(Mysql CDC) + 增量日志表                               │
│  • 存储: Iceberg (数据湖)                                       │
└─────────────────────────────────────────────────────────────────┘
```

### 3.2 核心表设计

#### ODS层 (贴源，从MySQL CDC同步)

| 源表 | ODS表 | 同步方式 | 频率 |
|------|-------|---------|------|
| cdp_user_profile | ods_user_profile | Canal CDC | 实时 |
| cdp_user_identity | ods_user_identity | Canal CDC | 实时 |
| cdp_user_tag | ods_user_tag | Canal CDC | 实时 |
| cdp_user_tag_history | ods_user_tag_history | Canal CDC | 实时 |
| audience | ods_audience | Canal CDC | 实时 |
| audience_rule | ods_audience_rule | Canal CDC | 实时 |
| canvas_execution | ods_canvas_execution | Canal CDC | 实时 |
| canvas_event_record | ods_event_record | Canal CDC | 实时 |

#### DWD层 (明细，清洗+维度展开)

```sql
-- 用户事件明细表
CREATE TABLE iceberg.cdp.dwd_user_event (
    event_id        STRING,
    user_id         STRING,
    tenant_id       BIGINT,
    event_code      STRING,
    event_time      TIMESTAMP(3),
    event_props     MAP<STRING, STRING>,   -- 展开的JSON属性
    channel         STRING,                 -- 来源渠道
    canvas_id       BIGINT,
    execution_id    STRING,
    dt              STRING                  -- 分区日期
) USING iceberg PARTITIONED BY (dt);

-- 用户身份宽表明细 (拉链表，支持历史回溯)
CREATE TABLE iceberg.cdp.dwd_user_identity_snap (
    user_id         STRING,
    tenant_id       BIGINT,
    identity_type   STRING,     -- USER_ID/PHONE/EMAIL/DEVICE_ID/WECHAT_OPENID
    identity_value  STRING,
    is_primary      BOOLEAN,
    start_time      TIMESTAMP(3),
    end_time        TIMESTAMP(3),  -- 9999-12-31表示当前有效
    dt              STRING
) USING iceberg PARTITIONED BY (dt);

-- 用户标签变更明细
CREATE TABLE iceberg.cdp.dwd_tag_change (
    user_id         STRING,
    tenant_id       BIGINT,
    tag_code        STRING,
    tag_value       STRING,
    operation       STRING,     -- SET/REMOVE/EXPIRE
    source_type     STRING,     -- CANVAS/API/IMPORT/MANUAL
    source_id       STRING,     -- execution_id或import_batch_id
    idempotency_key STRING,
    change_time     TIMESTAMP(3),
    dt              STRING
) USING iceberg PARTITIONED BY (dt);
```

#### DWS层 (汇总，按主题聚合)

```sql
-- 用户标签宽表 (一键查询用户全部标签)
CREATE TABLE clickhouse.cdp.dws_user_tag_wide (
    user_id         STRING,
    tenant_id       BIGINT,
    tag_values      MAP<STRING, STRING>,     -- {tag_code: value, ...}
    tag_count       INT,
    last_updated    DateTime,
    dt              Date
) ENGINE = ReplacingMergeTree(last_updated)
PARTITION BY (tenant_id, dt)
ORDER BY (tenant_id, user_id);

-- 用户行为聚合表 (日粒度)
CREATE TABLE clickhouse.cdp.dws_user_behavior_daily (
    user_id         STRING,
    tenant_id       BIGINT,
    event_date      Date,
    event_code      STRING,
    event_count     BIGINT,
    first_time      DateTime,
    last_time       DateTime,
    dt              Date
) ENGINE = SummingMergeTree(event_count)
PARTITION BY (tenant_id, toYYYYMM(event_date))
ORDER BY (tenant_id, user_id, event_date, event_code);

-- 人群汇总表
CREATE TABLE clickhouse.cdp.dws_audience_summary (
    audience_id     BIGINT,
    tenant_id       BIGINT,
    user_count      BIGINT,
    compute_time    DateTime,
    compute_duration_ms BIGINT,
    data_source     STRING,
    bitmap_size_kb  INT,
    dt              Date
) ENGINE = ReplacingMergeTree(compute_time)
PARTITION BY (tenant_id, dt)
ORDER BY (tenant_id, audience_id, dt);
```

#### ADS层 (应用，直接服务业务)

```sql
-- 用户360画像卡片
CREATE TABLE clickhouse.cdp.ads_user_profile_360 (
    user_id         STRING,
    tenant_id       BIGINT,
    basic_info      STRING,     -- JSON: {name, phone, email, ...}
    all_tags        STRING,     -- JSON: {tag: value, ...}
    segment_ids     STRING,     -- JSON数组: [audience_id, ...]
    behavior_30d    STRING,     -- JSON: {event: count, ...}
    last_active     DateTime,
    lifecycle_stage STRING,     -- NEW/ACTIVE/AT_RISK/CHURNED
    dt              Date
) ENGINE = ReplacingMergeTree(last_active)
PARTITION BY tenant_id
ORDER BY (tenant_id, user_id);

-- 人群包导出表
CREATE TABLE clickhouse.cdp.ads_audience_export (
    audience_id     BIGINT,
    tenant_id       BIGINT,
    user_id         STRING,
    export_time     DateTime,
    dt              Date
) ENGINE = MergeTree()
PARTITION BY (tenant_id, dt)
ORDER BY (tenant_id, audience_id, user_id);
```

---

## 四、数据管道设计

### 4.1 CDC实时同步管道

```
MySQL (canvas_db/cdp_db/meta_db)
  │
  │ binlog
  ▼
Canal Server (1 instance, 多destination)
  │
  │ protobuf/JSON
  ▼
Kafka (topic per table)
  │
  ├─→ Flink CDC Job: 写入Iceberg ODS层
  │
  ├─→ Flink Streaming Job: 实时标签/人群增量更新
  │
  └─→ Flink Streaming Job: 写入ClickHouse DWS宽表
```

#### Canal配置

```yaml
canal:
  server:
    mode: tcp
    port: 11111
  instance:
    - destination: canvas_db
      mysql:
        host: mysql-primary
        port: 3306
        username: canal
        password: ${CANAL_PASSWORD}
      mq:
        topic: cdc_canvas_db
        partition: 8
    - destination: cdp_db
      mysql:
        host: mysql-primary
        port: 3306
        username: canal
        password: ${CANAL_PASSWORD}
      mq:
        topic: cdc_cdp_db
        partition: 8
```

### 4.2 Flink实时增量人群计算

替换当前 `AudienceBatchComputeService.compute()` 的全量模式：

```java
// 新: Flink 增量人群更新 Job
public class AudienceIncrementalJob {

    public static void main(String[] args) {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.enableCheckpointing(60000);  // 每分钟checkpoint

        // 1. 监听标签变更流 (cdp_user_tag binlog)
        DataStream<TagChangeEvent> tagStream = env
            .fromSource(KafkaSources.cdcSource("cdc_cdp_db", "cdp_user_tag"),
                        WatermarkStrategy.noWatermarks(),
                        "cdc-tag-source")
            .map(CdcParser::toTagChangeEvent);

        // 2. 监听人群规则变更流 (audience_rule binlog)
        DataStream<AudienceRuleEvent> ruleStream = env
            .fromSource(KafkaSources.cdcSource("cdc_cdp_db", "audience_rule"),
                        WatermarkStrategy.noWatermarks(),
                        "cdc-rule-source")
            .map(CdcParser::toAudienceRuleEvent);

        // 3. 标签变更+人群规则匹配 → 增量更新Bitmap
        tagStream
            .connect(ruleStream.broadcast(ruleStateDescriptor))
            .process(new AudienceMembershipEvaluator())  // 核心: 规则评估
            .addSink(new RedisBitmapSink());             // 增量更新Redis Bitmap
    }
}
```

#### 增量计算收益

| 场景 | 当前全量模式 | Flink增量模式 | 提升 |
|------|-------------|-------------|------|
| 100万用户人群 | ~60s | ~5s | 12x |
| 1000万用户人群 | ~600s (OOM风险) | ~10s | 60x |
| 标签变更→人群更新 | 人工触发 | <1s自动 | 实时 |
| 内存占用 | 全量加载O(N) | 流式O(1) | N→1 |

### 4.3 Flink实时标签加工

```java
// 标签加工: 多数据源实时merge到用户标签宽表
public class TagMergeJob {

    public static void main(String[] args) {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        // 多源标签流 union
        DataStream<TagChangeEvent> canvasTags = /* canvas执行的标签变更 */;
        DataStream<TagChangeEvent> apiTags = /* 外部API导入的标签 */;
        DataStream<TagChangeEvent> behaviorTags = /* 行为触发的实时标签 */;

        canvasTags
            .union(apiTags, behaviorTags)
            .keyBy(e -> e.userId + ":" + e.tagCode)
            .process(new TagDeduplicationFunction())   // 去重+幂等
            .map(new TagToWideTableMapper())            // 转为宽表格式
            .addSink(new ClickHouseTagWideSink())       // 写入CK宽表
            .name("tag-merge-sink");
    }
}
```

---

## 五、OLAP查询层

### 5.1 替换当前内存计算

当前 `CdpAudienceSourceService` 加载全量数据到内存评估规则 → 替换为ClickHouse SQL下推：

```java
// 改造前: 内存评估 (CdpAudienceSourceService.resolveUserIds)
public List<String> resolveUserIds(AudienceDefinition def) {
    // 1. 加载全量标签到内存 Map<String, Map<String, String>>
    Map<String, Map<String, String>> factsByUser = cdpTagMapper.selectAllActiveAsMap();
    // 2. 循环每个用户，内存中评估规则
    return factsByUser.entrySet().stream()
        .filter(e -> matchesFacts(e.getValue(), def.getRuleGroup()))
        .map(Map.Entry::getKey)
        .collect(toList());
    // 问题: 100万用户 × 100标签 = 1亿条数据在JVM堆中
}

// 改造后: ClickHouse OLAP下推
@Service
@DS("clickhouse")  // 新增 ClickHouse 数据源
public class OlapAudienceService {

    public List<String> resolveUserIds(AudienceDefinition def) {
        // 规则 → ClickHouse SQL
        String sql = RuleToSqlConverter.convert(def.getRuleGroup());
        // WHERE tag['age'] >= 18 AND tag['city'] IN ('北京','上海')
        return clickHouseMapper.queryUserIds(sql);
        // 引擎层面完成过滤，只返回满足条件的user_id
    }
}
```

### 5.2 RuleGroup → ClickHouse SQL 转换

```java
public class RuleToSqlConverter {

    public static String convert(RuleGroup group) {
        String op = group.isAnd() ? " AND " : " OR ";
        return group.getConditions().stream()
            .map(c -> conditionToSql(c))
            .collect(Collectors.joining(op));
    }

    private static String conditionToSql(RuleCondition c) {
        return switch (c.getOp()) {
            case "="  -> "tag_values['%s'] = '%s'".formatted(c.getField(), c.getValue());
            case "!=" -> "tag_values['%s'] != '%s'".formatted(c.getField(), c.getValue());
            case ">"  -> "toFloat64OrNull(tag_values['%s']) > %s".formatted(c.getField(), c.getValue());
            case "<"  -> "toFloat64OrNull(tag_values['%s']) < %s".formatted(c.getField(), c.getValue());
            case "IN" -> "tag_values['%s'] IN (%s)".formatted(c.getField(), c.getInValues());
            default   -> throw new IllegalArgumentException("Unsupported op: " + c.getOp());
        };
    }
}
```

### 5.3 ClickHouse多数据源配置

```java
@Configuration
public class ClickHouseDataSourceConfig {

    @Bean("clickhouseDataSource")
    @ConfigurationProperties("spring.datasource.clickhouse")
    public DataSource clickhouseDataSource() {
        return ClickHouseDataSourceBuilder.create().build();
    }

    @Bean("clickhouseSqlSessionFactory")
    public SqlSessionFactory clickhouseSqlSessionFactory(
            @Qualifier("clickhouseDataSource") DataSource ds) throws Exception {
        SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
        bean.setDataSource(ds);
        bean.setMapperLocations(new PathMatchingResourcePatternResolver()
            .getResources("classpath*:mapper/clickhouse/**/*.xml"));
        return bean.getObject();
    }
}
```

```yaml
spring:
  datasource:
    clickhouse:
      url: jdbc:clickhouse://clickhouse:8123/cdp
      username: default
      password: ${CK_PASSWORD}
      driver-class-name: com.clickhouse.jdbc.ClickHouseDriver
```

---

## 六、数据治理

### 6.1 元数据管理 (DataHub)

```
┌──────────────────────────────────────────────────────────┐
│                      DataHub                              │
│                                                          │
│  ┌─────────────────┐  ┌─────────────┐  ┌─────────────┐  │
│  │ 数据目录         │  │ 数据血缘    │  │ 数据质量    │  │
│  │ (Catalog)       │  │ (Lineage)   │  │ (Quality)   │  │
│  │                 │  │             │  │             │  │
│  │ • 表/字段搜索   │  │ • 字段血缘  │  │ • 空值检查  │  │
│  │ • 数据分类      │  │ • 任务血缘  │  │ • 唯一性    │  │
│  │ • 敏感标签      │  │ • 影响分析  │  │ • 新鲜度    │  │
│  └─────────────────┘  └─────────────┘  └─────────────┘  │
└──────────────────────────────────────────────────────────┘
```

### 6.2 数据血缘 (从MySQL→Kafka→Iceberg→ClickHouse)

```
cdp_user_tag (MySQL)
  │
  │ Canal CDC
  ▼
ods_user_tag (Kafka Topic)
  │
  │ Flink ETL
  ▼
dwd_tag_change (Iceberg)
  │
  ├─→ Flink Aggregation → dws_user_tag_wide (ClickHouse)
  │                              │
  │                              └─→ ads_user_profile_360 (ClickHouse)
  │
  └─→ Flink Rule Engine → audience_bitmap (Redis)
```

### 6.3 数据质量监控

```yaml
# DataHub quality checks
checks:
  - name: "ods层数据新鲜度"
    table: ods_user_tag
    rule: "max(update_time) > now() - interval 5 minute"
    severity: critical

  - name: "用户标签覆盖度"
    table: dws_user_tag_wide
    rule: "count_if(tag_values is not null) / count(*) > 0.8"
    severity: warning

  - name: "人群计算成功率"
    table: dws_audience_summary
    rule: "count_if(status='READY') / count(*) > 0.95"
    severity: critical

  - name: "ODS vs MySQL数据一致性"
    tables: [ods_user_tag, cdp_user_tag]
    rule: "row_count_diff < 100"
    severity: critical
```

---

## 七、数据API服务层

### 7.1 统一查询服务

```java
@RestController
@RequestMapping("/api/v1/data")
public class DataQueryController {

    // GraphQL 风格的数据查询，支持关联查询和字段选择
    @PostMapping("/query")
    public DataQueryResult query(@RequestBody DataQueryRequest request) {
        // {
        //   "entity": "user_profile_360",
        //   "fields": ["userId", "allTags", "segmentIds", "behavior_30d"],
        //   "filter": { "tenantId": 1, "lifecycleStage": "ACTIVE" },
        //   "page": { "size": 100, "token": "..." }
        // }
        return dataQueryService.execute(request);
    }

    // 人群导出API
    @PostMapping("/audience/{audienceId}/export")
    public ExportJob exportAudience(@PathVariable Long audienceId,
                                     @RequestBody ExportConfig config) {
        // 支持导出格式: CSV, Parquet, JSON
        // 支持导出目标: S3, SFTP, HTTP callback
        return exportService.submit(audienceId, config);
    }

    // 用户段查询API
    @GetMapping("/user/{userId}/profile")
    public UserProfile360 getUserProfile(@PathVariable String userId) {
        return profile360Service.get(userId);
    }
}
```

### 7.2 数据API路由

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: data-query
          uri: lb://canvas-app
          predicates:
            - Path=/api/v1/data/**
          filters:
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 50
                redis-rate-limiter.burstCapacity: 100
```

---

## 八、与现有系统的集成

### 8.1 12个数据节点类型的集成方式

| 节点类型 | 当前实现 | 数据中台改造后 | 收益 |
|---------|---------|-------------|------|
| **AUDIENCE_TRIGGER** | 定时扫描+全量计算 | Flink增量+实时触发 | 小时→秒级 |
| **SCORING** | stub | ClickHouse RFM/评分SQL | 100ms出分 |
| **RECOMMENDATION** | stub | ClickHouse协同过滤 | 可落地 |
| **AI_NEXT_BEST_ACTION** | placeholder | ADS层模型预测结果 | 智能决策 |
| **EXPERIMENT** | 简单A/B分流 | ClickHouse分桶+统计分析 | 统计显著 |
| **TAGGER_REALTIME** | WebClient同步调用 | Flink实时打标 | 毫秒级 |
| **TAGGER_OFFLINE** | WebClient同步调用 | ClickHouse聚合标签 | 100ms |
| **TAGGER** | 3模式路由 | 统一OLAP查询 | 简化架构 |
| **UPDATE_PROFILE** | 直接写MySQL | Flink→宽表更新 | 实时同步 |
| **TAG_OPERATION** | 直接写MySQL | Flink→打标+事件 | 实时+幂等 |
| **TRACK_EVENT** | 写MySQL+MQ | Kafka事件流 | 统一事件总线 |
| **CDP_TAG_WRITE** | 写MySQL+幂等 | Flink→ClickHouse+Redis | 实时双写 |

### 8.2 与现有模块的关系

```
                      ┌─────────────────────┐
                      │   Canvas Core       │
                      │   (画布编排)        │
                      └────────┬────────────┘
                               │ 调用
                               ▼
┌──────────────────┐  ┌──────────────────────┐  ┌──────────────────┐
│ Data Platform    │  │   CDP-Profile        │  │ WeCom-SCRM       │
│ (数据中台)       │◄─┤   (用户画像)         │─►│ (企微私域)       │
│                  │  │                      │  │                  │
│ • ClickHouse查询 │  │ • 数据中台提供API    │  │ • 企微事件→Kafka │
│ • Flink实时计算  │  │ • CDP作为数据消费者  │  │ • 客户标签查询   │
│ • Kafka事件总线  │  │ • 用户360从CK读取   │  │ • 画像→个性化    │
└──────────────────┘  └──────────────────────┘  └──────────────────┘
         │
         │ 数据管道
         ▼
┌──────────────────┐  ┌──────────────────────┐
│ Notification     │  │ Platform Meta        │
│ (消息触达)       │  │ (审计/配置)          │
│                  │  │                      │
│ • 触达事件→Kafka │  │ • 审计日志→CDC同步   │
│ • 触达效果→ODS   │  │ • 配置变更→数据血缘  │
└──────────────────┘  └──────────────────────┘
```

---

## 九、K8s资源规划

### 9.1 新增组件资源

| 组件 | 副本 | CPU Request | CPU Limit | Memory Request | Memory Limit | 存储 |
|------|------|------------|-----------|---------------|-------------|------|
| **Kafka Broker** | 3 | 2 | 4 | 4Gi | 8Gi | 200Gi × 3 |
| **Kafka Connect** | 2 | 1 | 2 | 2Gi | 4Gi | - |
| **Canal Server** | 1 | 1 | 2 | 2Gi | 4Gi | - |
| **Flink JobManager** | 2 | 2 | 4 | 4Gi | 8Gi | - |
| **Flink TaskManager** | 4 | 4 | 8 | 8Gi | 16Gi | 100Gi × 4 |
| **Spark Driver** | 2 | 2 | 4 | 4Gi | 8Gi | - |
| **Spark Executor** | 4 | 4 | 8 | 8Gi | 16Gi | 100Gi × 4 |
| **ClickHouse** | 2 (分片) | 4 | 8 | 16Gi | 32Gi | 500Gi × 2 |
| **ClickHouse Keeper** | 3 | 1 | 2 | 2Gi | 4Gi | 50Gi × 3 |
| **MinIO** | 4 | 2 | 4 | 4Gi | 8Gi | 1Ti × 4 |
| **DataHub** | 1套 | 8 | 16 | 16Gi | 32Gi | 200Gi |
| **DolphinScheduler** | 2 | 2 | 4 | 4Gi | 8Gi | 50Gi |

### 9.2 总资源需求

| 环境 | CPU (requests) | Memory (requests) | 存储 |
|------|---------------|-------------------|------|
| **业务应用 (现有)** | ~8 | ~16Gi | 200Gi |
| **数据中台 (新增)** | ~60 | ~120Gi | ~5Ti |
| **合计** | ~68 | ~136Gi | ~5.2Ti |

---

## 十、实施路线图

### 十阶段渐进式建设

```
Phase 1 (Month 1-2): 数据集成层
  ├── 部署Kafka集群 (3 broker)
  ├── 部署Canal CDC → Kafka管道
  ├── Iceberg表设计 + MinIO部署
  └── MySQL CDC接入: canvas_db, cdp_db, meta_db表实时同步到ODS

Phase 2 (Month 3-4): 数据湖建设
  ├── DWD层ETL: Canal→Flink→Iceberg清洗流水线
  ├── DWS层宽表: Flink 实时聚合+Spark 离线补数
  └── 数据质量: DataHub 质量监控+新鲜度告警

Phase 3 (Month 5): OLAP引擎
  ├── ClickHouse集群部署 (2分片+3Keeper)
  ├── DWS/ADS层表从Iceberg同步到ClickHouse
  └── RuleToSQL 转换引擎 (替换CdpAudienceSourceService内存评估)

Phase 4 (Month 6): 实时计算上线
  ├── Flink增量人群计算 (替换全量AudienceBatchCompute)
  ├── Flink实时标签加工 (替换TaggerRealtime/Offline同步调用)
  └── Redis Bitmap增量更新 (替换全量重建)

Phase 5 (Month 7): 数据服务层
  ├── DataQuery API (GraphQL风格查询)
  ├── 用户360画像API (从ClickHouse读取)
  └── 人群导出服务 (CSV/Parquet→S3/SFTP)

Phase 6 (Month 8): 数据治理
  ├── DataHub 元数据目录 + 血缘 (自动采集)
  ├── 数据质量规则 + 告警
  └── 敏感数据识别 + 脱敏

Phase 7 (Month 9): AI/ML基础设施
  ├── 特征存储 (Feast/自研)
  ├── 模型训练管道 (Spark ML + 离线特征→模型)
  └── 模型服务 (在线推理，服务SCORING/AI_NEXT_BEST_ACTION节点)

Phase 8 (Month 10): BI集成
  ├── Grafana/Superset 连接ClickHouse
  ├── 核心看板: 用户增长/标签覆盖/人群分析/画布效果
  └── 自助分析: 拖拽式报表

Phase 9 (Month 11): 优化与稳定
  ├── 性能调优 + 数据倾斜处理
  ├── 灾备 + 跨可用区部署
  └── 成本优化 (冷热分离、压缩)

Phase 10 (Month 12): 进阶能力
  ├── 实时特征计算 (Flink CEP + 滑动窗口)
  ├── 图计算 (用户关系图谱)
  └── 预测模型 (churn/lifecycle/ltv)
```

### 人力估算

| 阶段 | 数据工程师 | 后端工程师 | 工时合计 |
|------|----------|-----------|---------|
| Phase 1-2 | 2人 × 8周 | 1人 × 4周 | 288h |
| Phase 3-4 | 2人 × 8周 | 2人 × 6周 | 384h |
| Phase 5-6 | 1人 × 8周 | 2人 × 8周 | 288h |
| Phase 7-8 | 2人 × 8周 | 1人 × 4周 | 256h |
| Phase 9-10 | 2人 × 8周 | 1人 × 4周 | 256h |
| **合计** | | | **1472h** |

---

## 十一、风险和应对

| 风险 | 影响 | 应对 |
|------|------|------|
| Kafka/RocketMQ双消息体系 | 运维复杂度 | Kafka仅用于数据管道，业务MQ仍用RocketMQ |
| CDC延迟 | 数据时效性 | 监控binlog lag，告警阈值>30s |
| ClickHouse查询冷启动 | 首次查询慢 | 物化视图预聚合+查询缓存 |
| Flink状态膨胀 | TaskManager OOM | RocksDB状态后端+TTL+定期清理 |
| 现有引擎兼容性 | 回归风险 | 双写模式过渡：同时写MySQL+ClickHouse，验证后切流 |

---

## 十二、相关文档

- [目标架构总览](./target-architecture-overview.md) — 需更新：新增第7个Bounded Context
- [K8s部署方案](./k8s-deployment-plan.md) — 需更新：新增数据中台组件
- [多数据源隔离方案](./multi-datasource-isolation.md)
- [WebFlux→Spring MVC迁移](./webflux-to-mvc-migration.md)
