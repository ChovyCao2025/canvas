# 架构演进路线图 (2026-06-01)

> **定位**: 从单体巨石到模块化私域运营中台 + 数据中台，分4阶段12个月完整演进路线

---

## 一、演进总览

```
当前状态                           Phase 1 (Week 1-8)                  Phase 2 (Month 3-6)                  Phase 3 (Month 7-9)                  Phase 4 (Month 10-12)
┌──────────┐                      ┌────────────────────┐              ┌────────────────────┐              ┌────────────────────┐              ┌────────────────────┐
│ 单体巨石  │  ──────────────►     │ 模块化 + MVC       │  ──────►     │ 数据中台基座       │  ──────►     │ 批量计算 + 治理     │  ──────►     │ 数据服务 + AI      │
│ 65DO     │                      │                    │              │                    │              │                    │              │                    │
│ 49Handler│                      │ 3库隔离            │              │ Kafka+MinIO       │              │ Spark批计算        │              │ 统一数据API        │
│ WebFlux  │                      │ 虚拟线程           │              │ Flink实时计算     │              │ DataHub治理       │              │ 漏斗+留存分析      │
│ 单库33连 │                      │ Gateway+监控       │              │ ClickHouse OLAP   │              │ DolphinScheduler  │              │ AI预测模型         │
└──────────┘                      └────────────────────┘              └────────────────────┘              └────────────────────┘              └────────────────────┘
   生产止血                           私域筑基                            数据引擎上线                         治理+批计算                          智能决策
```

---

## 二、当前状态基线

### 2.1 架构问题清单

| 问题 | 严重度 | 影响范围 | 修复阶段 |
|------|--------|---------|---------|
| 单体巨石无边界 | CRITICAL | 全域 | Phase 1 |
| WebFlux+MyBatis矛盾 | CRITICAL | 并发能力丧失 | Phase 1 |
| 单库单连接池 | CRITICAL | 批量查询饿死其他域 | Phase 1 |
| Handler平铺无分组 | HIGH | 可维护性 | Phase 1 |
| Service层缺失 | HIGH | 业务逻辑散落 | Phase 1 |
| DO直接暴露API | HIGH | 契约紧耦合 | Phase 1 |
| 无数据采集管道 | CRITICAL | 数据中台 | Phase 2 |
| 人群计算O(N)内存 | CRITICAL | OOM风险(>1M用户) | Phase 2 |
| 无OLAP查询能力 | HIGH | 分析查询不可用 | Phase 2 |
| 无数据治理 | MEDIUM | 数据质量不可控 | Phase 3 |
| 无AI/预测能力 | MEDIUM | 竞争力缺失 | Phase 4 |

### 2.2 关键瓶颈量化

| 瓶颈 | 当前上限 | 目标上限 | 解决手段 |
|------|---------|---------|---------|
| 数据库连接数 | 33 (单池) | 60 (3池) | 多数据源隔离 |
| 并发请求 | ~500 (Netty阻塞) | ~10000 (虚拟线程) | WebFlux→MVC |
| 人群计算规模 | ~100万用户(OOM) | ~1亿用户 | ClickHouse OLAP |
| 人群更新延迟 | 全量扫描(分钟级) | 增量(秒级) | Flink实时计算 |
| 标签同步 | 无管道 | CDC实时 | Canal→Kafka |

---

## 三、Phase 1: 生产止血（Week 1-8）

**目标**: 消除架构风险，建立可扩展基础

### 3.1 里程碑

```
M1.1 (Week 2): Maven模块化拆分完成，编译通过
  └── 验收: mvn clean install 全模块通过

M1.2 (Week 4): 3库隔离上线，SQL路由正确
  └── 验收: 各域SQL路由到正确数据库，连接池不互相影响

M1.3 (Week 6): Spring MVC迁移完成
  └── 验收: 所有Controller同步返回，虚拟线程配置生效

M1.4 (Week 8): Gateway + 可观测性上线
  └── 验收: Grafana Dashboard可见，告警规则生效
```

### 3.2 详细任务

#### Week 1-2: Maven模块化拆分

```
任务1.1: 创建父POM (canvas-parent)
  ├── 统一依赖版本管理
  ├── 统一插件配置
  └── 工时: 4h

任务1.2: 创建基础模块 (canvas-common, canvas-api)
  ├── canvas-common: 工具类、异常、常量
  ├── canvas-api: DTO/VO/契约
  └── 工时: 8h

任务1.3: 迁移DO到canvas-api
  ├── 65个DO实体迁移
  ├── 包路径调整
  └── 工时: 16h

任务1.4: 创建业务模块骨架
  ├── canvas-engine (执行引擎)
  ├── canvas-cdp (CDP)
  ├── canvas-wecom (企微)
  ├── canvas-notification (通知)
  ├── canvas-platform (平台)
  └── 工时: 8h

任务1.5: 验证编译
  ├── 修复循环依赖
  ├── 修复包引用
  └── 工时: 8h
```

#### Week 3-4: 多数据源配置

```
任务2.1: 创建3个数据库
  ├── canvas_db / cdp_db / meta_db
  ├── 用户权限配置
  └── 工时: 4h

任务2.2: 配置dynamic-datasource
  ├── 3个HikariCP数据源
  ├── 连接池参数调优
  └── 工时: 8h

任务2.3: Mapper @DS注解路由
  ├── 按域标注所有Mapper
  ├── Service层数据源指定
  └── 工时: 12h

任务2.4: Flyway多库迁移
  ├── 拆分V1-V81到3个目录
  ├── 验证迁移脚本可运行
  └── 工时: 8h

任务2.5: Outbox模式实现
  ├── canvas_outbox_event表
  ├── Outbox扫描器
  └── 工时: 8h

任务2.6: tenant_id强制可见
  ├── MetaObjectHandler自动填充
  ├── TenantLineInnerInterceptor全局过滤
  └── 工时: 4h
```

#### Week 5-6: WebFlux → Spring MVC

```
任务3.1: Controller层同步化改造
  ├── 30个Controller Mono→同步返回
  ├── 保持API兼容
  └── 工时: 16h

任务3.2: Service层适配
  ├── 移除Mono.fromCallable+subscribeOn
  ├── 改为直接同步调用
  └── 工时: 16h

任务3.3: 移除WebFlux + 引入Spring MVC
  ├── pom.xml依赖替换
  ├── 移除Netty配置
  ├── 配置Tomcat+虚拟线程
  └── 工时: 8h

任务3.4: DAG引擎内部改造
  ├── DagEngine核心改为同步
  ├── Handler接口适配
  └── 工时: 16h

任务3.5: 外部调用改造
  ├── WebClient→RestClient
  ├── ReactiveRedis→StringRedisTemplate
  └── 工时: 8h

任务3.6: 全量回归测试
  └── 工时: 8h
```

#### Week 7-8: API Gateway + 可观测性

```
任务4.1: Spring Cloud Gateway部署
  ├── 路由规则配置(5个域)
  ├── 限流配置
  └── 工时: 8h

任务4.2: Prometheus + Grafana部署
  ├── Helm安装kube-prometheus-stack
  ├── JVM指标采集
  ├── HikariCP指标采集
  └── 工时: 8h

任务4.3: Grafana Dashboard创建
  ├── HTTP QPS/延迟面板
  ├── 画布执行面板
  ├── 数据库连接池面板
  └── 工时: 8h

任务4.4: 告警规则配置
  ├── 错误率>5%告警
  ├── 画布执行超时告警
  ├── 连接池>80%告警
  └── 工时: 4h

任务4.5: 生产环境部署验证
  ├── K8s集群部署
  ├── HPA验证
  └── 工时: 8h
```

### 3.3 Phase 1 交付物

| 交付物 | 说明 |
|--------|------|
| 7个Maven模块 | canvas-parent/engine/cdp/wecom/notification/platform/common/api |
| 3库隔离 | canvas_db (max=20) / cdp_db (max=30) / meta_db (max=10) |
| Spring MVC + 虚拟线程 | Tomcat处理请求，虚拟线程自动调度 |
| API Gateway | 统一认证/限流/路由 |
| 可观测性 | Prometheus + Grafana + 告警规则 |

---

## 四、Phase 2: 数据中台基座（Month 3-6）

**目标**: 搭建数据采集、存储、实时计算基础设施，上线OLAP查询

### 4.1 里程碑

```
M2.1 (Month 3): Kafka + MinIO + CDC管道就绪
  └── 验收: MySQL变更实时流入Kafka，Iceberg表可见

M2.2 (Month 4): ODS/DWD层Iceberg表创建完成
  └── 验收: 用户事件、身份快照、标签变更可查询

M2.3 (Month 5): Flink增量人群计算上线
  └── 验收: 人群Bitmap增量更新延迟<5s

M2.4 (Month 6): ClickHouse集群 + OLAP查询上线
  └── 验收: 人群规则SQL化查询可用，P99延迟<3s
```

### 4.2 详细任务

#### Month 3: 数据平台基础设施

```
任务5.1: Kafka集群部署
  ├── 3 Broker (2-4CPU/4-8Gi/200Gi)
  ├── 关键Topic创建 (cdc-events, user-events, audience-events)
  ├── 分区策略设计 (按tenant_id+user_id取模)
  └── 工时: 16h

任务5.2: MinIO对象存储集群
  ├── 4节点 (2-4CPU/4-8Gi/1Ti)
  ├── Bucket创建 (canvas-lakehouse)
  ├── 生命周期策略 (ODS 30天→冷存储)
  └── 工时: 8h

任务5.3: MySQL CDC管道搭建
  ├── Canal部署 (或Debezium)
  ├── binlog解析→Kafka
  ├── 表白名单配置 (user_profile, tag, audience, wecom_customer...)
  └── 工时: 16h

任务5.4: Iceberg表创建 (ODS层)
  ├── ods_user_profile (CDC原始镜像)
  ├── ods_tag_change (标签变更日志)
  ├── ods_audience_change (人群变更日志)
  └── 工时: 8h
```

#### Month 4: ODS/DWD层建设

```
任务6.1: DWD用户事件表
  ├── dwd_user_event (事件明细，按dt分区)
  ├── 字段: event_id, user_id, event_type, properties, dt
  └── 工时: 8h

任务6.2: DWD用户身份快照表
  ├── dwd_user_identity_snap (拉链表)
  ├── 字段: user_id, identity_type, identity_value, start_dt, end_dt
  └── 工时: 8h

任务6.3: DWD标签快照表
  ├── dwd_tag_snap (每日快照)
  ├── 按dt分区，支持时间旅行查询
  └── 工时: 8h

任务6.4: Flink CDC作业
  ├── MySQL binlog→Iceberg ODS
  ├── ODS→DWD ETL (清洗/去重/补全)
  ├── Checkpoint 60s
  └── 工时: 16h
```

#### Month 5: 实时计算

```
任务7.1: Flink集群部署
  ├── JobManager 2副本 (2-4CPU/4-8Gi/50Gi)
  ├── TaskManager 4副本 (4-8CPU/8-16Gi/100Gi)
  ├── HA配置 (ZooKeeper)
  └── 工时: 16h

任务7.2: 增量人群Bitmap计算
  ├── AudienceMembershipEvaluator (Flink作业)
  ├── Kafka CDC source + Broadcast rule state
  ├── 增量Bitmap→Redis (RoaringBitmap)
  ├── 替换AudienceBatchComputeService全量扫描
  └── 工时: 24h

任务7.3: 实时标签处理
  ├── 事件驱动标签计算
  ├── 窗口聚合 (tumble/slide/session)
  ├── 标签结果写入cdp_db + Redis缓存
  └── 工时: 16h

任务7.4: 实时事件窗口聚合
  ├── 近7天活跃/近30天购买等窗口指标
  ├── 写入ClickHouse DWS层
  └── 工时: 8h
```

#### Month 6: OLAP查询

```
任务8.1: ClickHouse集群部署
  ├── 2分片 × 2副本 (4-8CPU/16-32Gi/500Gi)
  ├── ZooKeeper 3节点 (1-2CPU/2-4Gi)
  └── 工时: 16h

任务8.2: DWS/ADS层表创建
  ├── dws_user_tag_wide (ReplacingMergeTree，标签宽表)
  ├── ads_user_profile_360 (用户360视图)
  ├── ads_audience_export (人群导出结果)
  └── 工时: 8h

任务8.3: RuleToSqlConverter实现
  ├── Aviator RuleGroup→ClickHouse SQL
  ├── 支持: =, !=, >, <, IN, BETWEEN, LIKE, AND/OR/NOT
  ├── 支持: 标签条件、属性条件、事件条件
  └── 工时: 16h

任务8.4: OlapAudienceService上线
  ├── 替换CdpAudienceSourceService (O(N)内存→SQL push-down)
  ├── 支持>1亿用户人群计算
  └── 工时: 16h

任务8.5: 数据同步到ClickHouse
  ├── Flink作业: Iceberg DWD→ClickHouse DWS
  ├── 全量+增量同步
  └── 工时: 8h
```

### 4.3 Phase 2 交付物

| 交付物 | 说明 |
|--------|------|
| Kafka集群 | 3 Broker，CDC事件实时采集 |
| MinIO数据湖 | 4节点，Iceberg表存储 |
| ODS/DWD层 | 用户事件/身份/标签/人群原始+明细数据 |
| Flink实时计算 | 增量人群Bitmap、实时标签、窗口聚合 |
| ClickHouse OLAP | 2分片，DWS/ADS查询加速 |
| OlapAudienceService | SQL push-down人群计算，支持1亿+用户 |

---

## 五、Phase 3: 批量计算 + 数据治理（Month 7-9）

**目标**: 上线批量计算能力，建立数据治理体系

### 5.1 里程碑

```
M3.1 (Month 7): Spark集群 + 用户画像宽表
  └── 验收: 每日全量画像宽表产出正常

M3.2 (Month 8): DataHub血缘+质量上线
  └── 验收: 全链路数据血缘可视化，质量规则通过率>95%

M3.3 (Month 9): DolphinScheduler调度上线
  └── 验收: 所有定时任务迁移到DolphinScheduler，SLA达成率>99%
```

### 5.2 详细任务

#### Month 7: Spark批量计算

```
任务9.1: Spark集群部署
  ├── Driver 2副本 (2-4CPU/4-8Gi)
  ├── Executor 4副本 (4-8CPU/8-16Gi)
  └── 工时: 8h

任务9.2: 用户画像宽表全量重建
  ├── 每日全量: user_profile + tag + event聚合
  ├── 写入ClickHouse ads_user_profile_360
  └── 工时: 16h

任务9.3: 人群全量重建
  ├── 每日全量Bitmap重建（补充增量）
  ├── 人群交叉/排除/合并计算
  └── 工时: 16h

任务9.4: 标签全量聚合
  ├── 标签覆盖度统计
  ├── 标签分布分析
  └── 工时: 8h
```

#### Month 8: 数据治理

```
任务10.1: DataHub部署
  ├── 8-16CPU/16-32Gi/100Gi
  ├── MySQL/Hive/ClickHouse元数据采集
  └── 工时: 16h

任务10.2: 数据血缘配置
  ├── MySQL→Canal→Kafka→Iceberg ODS
  ├── ODS→Flink→DWD→ClickHouse DWS
  ├── DWS→Spark→ADS
  └── 工时: 16h

任务10.3: 数据质量规则
  ├── 完整性: 关键字段非空率>99%
  ├── 一致性: 跨表行数偏差<1%
  ├── 及时性: CDC延迟<30s
  ├── 唯一性: user_id唯一性检查
  └── 工时: 16h

任务10.4: 数据质量告警
  ├── Prometheus告警规则
  ├── 质量dashboard
  └── 工时: 8h
```

#### Month 9: 调度平台

```
任务11.1: DolphinScheduler部署
  ├── 2副本 (2-4CPU/4-8Gi/50Gi)
  └── 工时: 8h

任务11.2: 任务迁移
  ├── Spark画像重建DAG (每日02:00)
  ├── Spark人群重建DAG (每日03:00)
  ├── Flink增量作业监控
  ├── 数据质量检查DAG (每日06:00)
  └── 工时: 16h

任务11.3: SLA监控
  ├── 任务超时告警
  ├── 依赖链追踪
  ├── 失败自动重试
  └── 工时: 8h
```

### 5.3 Phase 3 交付物

| 交付物 | 说明 |
|--------|------|
| Spark批量计算 | 画像宽表/人群全量重建/标签聚合 |
| DataHub数据治理 | 血缘可视化/质量规则/质量监控 |
| DolphinScheduler | 可视化DAG调度/SLA监控/自动重试 |

---

## 六、Phase 4: 数据服务 + 智能分析（Month 10-12）

**目标**: 统一数据API上线，集成AI预测模型，全链路压测上线

### 6.1 里程碑

```
M4.1 (Month 10): 统一数据API上线
  └── 验收: Tag/Event/Profile/Audience四大API可用

M4.2 (Month 11): 漏斗分析 + 留存分析上线
  └── 验收: 任意事件组合漏斗，自定义周期留存

M4.3 (Month 12): AI预测模型 + 全链路压测
  └── 验收: CLV/流失/倾向模型预测可用，压测通过
```

### 6.2 详细任务

#### Month 10: 统一数据API

```
任务12.1: Tag API
  ├── GET /api/v1/data/tags/{userId} (用户标签查询)
  ├── POST /api/v1/data/tags/batch (批量标签查询)
  ├── GET /api/v1/data/tags/distribution/{tagId} (标签分布)
  └── 工时: 16h

任务12.2: Event API
  ├── GET /api/v1/data/events/{userId} (用户事件查询)
  ├── POST /api/v1/data/events/aggregate (事件聚合查询)
  ├── ClickHouse SQL模板化，防注入
  └── 工时: 16h

任务12.3: Profile API
  ├── GET /api/v1/data/profiles/{userId} (用户360视图)
  ├── POST /api/v1/data/profiles/segment (分群画像)
  └── 工时: 8h

任务12.4: Audience API
  ├── POST /api/v1/data/audiences/compute (人群计算)
  ├── GET /api/v1/data/audiences/{audienceId}/export (人群导出)
  ├── GET /api/v1/data/audiences/{audienceId}/overlap (人群重叠)
  └── 工时: 8h
```

#### Month 11: 分析产品

```
任务13.1: 漏斗分析
  ├── 任意事件序列漏斗
  ├── 转化率/流失率计算
  ├── 分组对比 (按渠道/按标签/按人群)
  ├── ClickHouse windowFunnel实现
  └── 工时: 24h

任务13.2: 留存分析
  ├── N日留存/周留存/月留存
  ├── 自定义初始事件+回访事件
  ├── 留存曲线可视化数据
  └── 工时: 16h

任务13.3: 事件分析
  ├── 事件统计/趋势
  ├── 属性分组/过滤
  ├── 用户细查 (单用户事件时间线)
  └── 工时: 16h

任务13.4: 数据看板
  ├── 运营概览看板 (DAU/MAU/转化/留存)
  ├── 画布效果看板 (执行次数/成功率/节点耗时)
  ├── 企微运营看板 (客户增长/群活跃/消息触达)
  └── 工时: 16h
```

#### Month 12: AI + 上线

```
任务14.1: AI预测模型集成
  ├── CLV预测 (客户生命周期价值)
  ├── 流失预测 (7天/30天流失概率)
  ├── 购买倾向 (品类/金额/时间)
  ├── 模型服务化 (Spring Boot + Python模型)
  └── 工时: 32h

任务14.2: 预测人群创建
  ├── 高价值流失风险人群
  ├── 高潜转化人群
  ├── 自动同步到画布受众节点
  └── 工时: 16h

任务14.3: 全链路压测
  ├── 画布执行压测 (1000并发)
  ├── 人群计算压测 (1亿用户)
  ├── OLAP查询压测 (100 QPS)
  └── 工时: 16h

任务14.4: 生产上线
  ├── 灰度发布 (5%→50%→100%)
  ├── 监控值守
  ├── 回滚预案
  └── 工时: 16h
```

### 6.3 Phase 4 交付物

| 交付物 | 说明 |
|--------|------|
| 统一数据API | Tag/Event/Profile/Audience四大API |
| 漏斗+留存分析 | 任意事件组合，可视化数据输出 |
| AI预测模型 | CLV/流失/倾向预测，预测人群自动创建 |
| 全链路压测报告 | 性能基线确认 |

---

## 七、依赖关系图

```
Phase 1 ────────────────────────────────────────────────────────────────►
  Maven拆分 ──► 多数据源 ──► MVC迁移 ──► Gateway+监控
      │              │            │
      │              ▼            ▼
      │         3库隔离      虚拟线程
      │                           │
      ▼                           ▼
Phase 2 ────────────────────────────────────────────────────────────────►
  Kafka ──► CDC管道 ──► Iceberg ODS ──► Flink实时 ──► ClickHouse OLAP
    │                        │               │              │
    ▼                        ▼               ▼              ▼
  MinIO                    DWD层         增量Bitmap      SQL化查询

Phase 3 ────────────────────────────────────────────────────────────────►
  Spark部署 ──► 画像宽表 ──► 人群全量重建
      │
      ▼
  DataHub部署 ──► 血缘采集 ──► 质量规则 ──► 质量告警
      │
      ▼
  DolphinScheduler ──► 任务迁移 ──► SLA监控

Phase 4 ────────────────────────────────────────────────────────────────►
  统一API ──► 漏斗分析 ──► 留存分析 ──► AI模型 ──► 全链路压测 ──► 生产上线
```

### 7.1 关键路径

```
Maven拆分 → 多数据源 → Flink实时 → ClickHouse OLAP → 统一API → 生产上线
  (2周)     (2周)      (4周)        (4周)           (4周)     (2周)
```

关键路径总时长: 18周，占整个路线图的37.5%

### 7.2 可并行工作

| 并行组 | 内容 | 时间 |
|--------|------|------|
| 组A | Gateway配置 + Prometheus部署 | Week 7-8 |
| 组B | Kafka部署 + MinIO部署 | Month 3 |
| 组C | ClickHouse部署 + Flink部署 | Month 5-6 |
| 组D | DataHub部署 + DolphinScheduler部署 | Month 8-9 |
| 组E | 漏斗分析 + 留存分析 + 事件分析 | Month 11 |

---

## 八、团队与资源

### 8.1 人员配置

| 角色 | 人数 | Phase 1 | Phase 2 | Phase 3 | Phase 4 |
|------|------|---------|---------|---------|---------|
| 后端开发 | 2-3 | 全职 | 全职 | 全职 | 全职 |
| 大数据工程师 | 1-2 | - | 全职 | 全职 | 全职 |
| 前端开发 | 1 | 适配 | 数据分析页面 | 治理UI | 分析产品 |
| DevOps | 1 | 全职 | 全职 | 50% | 50% |
| 数据产品经理 | 1 | 需求 | 需求 | 需求 | 需求 |

### 8.2 资源需求

| 资源 | Phase 1 | Phase 2 | Phase 3 | Phase 4 |
|------|---------|---------|---------|---------|
| K8s CPU | ~20核 | +40核 (Kafka/Flink/ClickHouse/MinIO) | +30核 (Spark/DataHub/Dolphin) | +15核 (AI服务) |
| K8s 内存 | ~40Gi | +100Gi | +70Gi | +30Gi |
| 存储 | - | +5Ti (MinIO+ClickHouse) | +2Ti | +1Ti |
| 云成本估算 | ~$500/月 | +$2000/月 | +$1500/月 | +$1000/月 |

---

## 九、风险与对策

### 9.1 技术风险

| 风险 | 概率 | 影响 | 对策 |
|------|------|------|------|
| Maven模块拆分引入循环依赖 | 中 | 高 | 严格执行canvas-api←各模块的依赖规则，CI阶段检查 |
| 虚拟线程性能不如预期 | 低 | 中 | 保留降级开关，可切回传统线程池 |
| Flink CDC延迟过大 | 中 | 高 | 限流消费，预留Partition扩容空间 |
| ClickHouse查询性能不达预期 | 中 | 中 | 合理设计排序键+分区键，物化视图预聚合 |
| 数据迁移不一致 | 中 | 高 | 双写验证期，自动数据对账脚本 |
| AI模型预测效果差 | 高 | 中 | 从规则模型起步，逐步迭代ML模型 |

### 9.2 组织风险

| 风险 | 概率 | 影响 | 对策 |
|------|------|------|------|
| 大数据人才招聘困难 | 高 | 高 | Phase 1期间提前启动招聘，考虑外包部分工作 |
| 需求变更频繁 | 中 | 中 | 每阶段锁定需求，变更走正式的CR流程 |
| 多线并行协调困难 | 中 | 中 | 每周跨团队同步会，Confluence记录决策 |

### 9.3 降级预案

```
如果Phase 2延期:
  → 优先上线Kafka+MinIO (Month 3)
  → Flink和ClickHouse可串行 (Month 4-6)

如果Phase 3延期:
  → 优先DataHub质量监控 (Month 8)
  → Spark和DolphinScheduler可后移

如果Phase 4延期:
  → 优先统一数据API (Month 10)
  → AI模型可用第三方SaaS替代
```

---

## 十、成功标准

### 10.1 阶段性验收指标

| 阶段 | 指标 | 当前值 | 目标值 |
|------|------|--------|--------|
| Phase 1 | 接口P99延迟 | ~500ms | <200ms |
| Phase 1 | 并发连接数 | ~500 | >5000 |
| Phase 1 | 数据库连接利用率 | 100% (单池) | <60% (分池) |
| Phase 2 | 人群计算规模 | <100万 | >1亿 |
| Phase 2 | 人群更新延迟 | 分钟级 | <5秒 |
| Phase 2 | OLAP查询P99 | N/A | <3秒 |
| Phase 3 | 数据质量通过率 | N/A | >95% |
| Phase 3 | 任务SLA达成率 | N/A | >99% |
| Phase 4 | AI模型AUC | N/A | >0.7 |
| Phase 4 | 全链路压测通过 | N/A | 1000并发稳定 |

### 10.2 最终验收

12个月后，系统应具备以下能力：

- 画布编排支持1亿+用户的实时营销
- 人群圈选从数分钟降至秒级
- 用户360视图覆盖全触点
- 营销效果可归因、可分析、可预测
- 数据质量可控、血缘可追溯
- 全链路可观测、可告警

---

## 十一、相关文档

- [服务划分与新应用搭建方案](service-architecture-design.md)
- [目标架构总览](target-architecture-overview.md)
- [数据平台架构设计](data-platform-architecture.md)
- [K8s部署方案](k8s-deployment-plan.md)
- [多数据源隔离方案](multi-datasource-isolation.md)
- [WebFlux→Spring MVC迁移](webflux-to-mvc-migration.md)
- [企微SCRM模块设计](wecom-scrm-module-design.md)
