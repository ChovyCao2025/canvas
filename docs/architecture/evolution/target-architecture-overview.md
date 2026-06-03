# 目标架构总览 (2026-05-31)

> **定位**: 从单体巨石演进为模块化私域运营中台，支持企微SCRM + DAG画布 + AI预测

---

## 一、当前架构问题

| 问题 | 现状 | 影响 |
|------|------|------|
| 单体巨石 | 65DO/30Controller/49Handler全在canvas-engine | 无服务边界，无法独立扩展 |
| WebFlux+MyBatis矛盾 | 30+.block()阻塞Netty事件循环 | 实际退化为单线程，并发能力丧失 |
| 单库单连接池 | 65表共享HikariCP(max=33) | 批量查询饿死其他域 |
| Handler平铺 | 49个Handler单包无分组 | 随节点增长不可维护 |
| Service层缺失 | 仅1个Service接口 | 业务逻辑散落Controller/Domain |
| DO直接暴露 | API返回DO对象 | 契约与Schema紧耦合 |

---

## 二、目标架构（Phase 1后）

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           API Gateway (新增)                             │
│  功能: 认证/限流/路由/版本管理/请求日志                                   │
│  技术: Spring Cloud Gateway 或 Kong                                      │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
          ┌─────────────────────────┼─────────────────────────┐
          ▼                         ▼                         ▼
┌───────────────────┐   ┌───────────────────┐   ┌───────────────────┐
│   Canvas Core     │   │   CDP-Profile     │   │   WeCom-SCRM      │
│   (画布编排域)    │   │   (用户画像域)    │   │   (企微私域域)    │
│                   │   │                   │   │                   │
│ ┌───────────────┐ │   │ ┌───────────────┐ │   │ ┌───────────────┐ │
│ │ DagEngine     │ │   │ │ TagService    │ │   │ │ CustomerMgr   │ │
│ │ - 57 Handlers │ │   │ │ - Audience    │ │   │ │ - GroupChat   │ │
│ │ - Lane/Disrupt│ │   │ │ - Bitmap      │ │   │ │ - WelcomeMsg  │ │
│ │ - NodeGate    │ │   │ │ - RuleEngine  │ │   │ │ - Moments     │ │
│ └───────────────┘ │   │ └───────────────┘ │   │ └───────────────┘ │
│                   │   │                   │   │                   │
│ ┌───────────────┐ │   │ ┌───────────────┐ │   │ ┌───────────────┐ │
│ │ CanvasService │ │   │ │ ProfileService│ │   │ │ WeComService  │ │
│ │ - CRUD        │ │   │ │ - User360     │ │   │ │ - API适配     │ │
│ │ - Versioning  │ │   │ │ - Segments    │ │   │ │ - 消息推送    │ │
│ └───────────────┘ │   │ └───────────────┘ │   │ └───────────────┘ │
└───────────────────┘   └───────────────────┘   └───────────────────┘
          │                         │                         │
          └─────────────────────────┼─────────────────────────┘
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                      Data Platform (数据中台层，新增)                     │
│                                                                          │
│  ┌───────────────┐  ┌───────────────┐  ┌───────────────┐                │
│  │ Ingestion     │  │ LakeHouse     │  │ OLAP          │                │
│  │ Kafka+Canal   │  │ Iceberg+MinIO │  │ ClickHouse    │                │
│  │ (CDC实时采集) │  │ (S3对象存储)  │  │ (ADS/DWS查询) │                │
│  └───────────────┘  └───────────────┘  └───────────────┘                │
│                                                                          │
│  ┌───────────────┐  ┌───────────────┐                                    │
│  │ Compute       │  │ Governance    │                                    │
│  │ Flink+Spark   │  │ DataHub       │                                    │
│  │ (实时+批量)   │  │ (血缘+质量)   │                                    │
│  └───────────────┘  └───────────────┘                                    │
└─────────────────────────────────────────────────────────────────────────┘
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                        Shared Infrastructure                             │
│                                                                          │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐    │
│  │ Redis HA    │  │ MySQL 3库   │  │ RocketMQ    │  │ 可观测性    │    │
│  │ (L1缓存)    │  │ (隔离)      │  │ (集群)      │  │ (Prometheus)│    │
│  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘    │
│                                                                          │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐    │
│  │ Kafka       │  │ Flink       │  │ Spark       │  │ MinIO       │    │
│  │ (消息队列)  │  │ (实时计算)  │  │ (批量计算)  │  │ (对象存储)  │    │
│  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘    │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 三、7个Bounded Context

| Context | 职责 | 核心实体 | 数据库 |
|---------|------|---------|--------|
| **Canvas Core** | 画布编排、执行调度 | Canvas, Node, Edge, Execution | canvas_db |
| **Execution Engine** | DAG执行、Handler调度 | ExecutionContext, NodeResult, Lane | canvas_db |
| **CDP-Profile** | 用户画像、标签、人群 | User, Tag, Audience, Segment | cdp_db |
| **WeCom-SCRM** | 企微私域运营 | WeComCustomer, GroupChat, WelcomeMsg | wecom_db |
| **Notification** | 消息触达、渠道适配 | Message, Channel, Delivery | canvas_db |
| **Platform Meta** | 租户、配置、审计 | Tenant, Config, AuditLog | meta_db |
| **Data Platform** | 数据管道/OLAP查询/实时离线计算/数据治理 | DataPipeline, OlapQuery, RealtimeJob, BatchJob, DataAsset | ODS/DWD (Iceberg on MinIO) / DWS/ADS (ClickHouse) |

---

## 四、关键技术决策

| 决策点 | 当前 | 目标 | 理由 |
|--------|------|------|------|
| **Web框架** | WebFlux + MyBatis | Spring MVC + Virtual Threads | Java 21原生支持，避免Reactor陷阱 |
| **数据库** | 单库65表 | 3库隔离 | 避免批量查询饿死其他域 |
| **缓存** | 单Redis | Redis HA + Caffeine L1 | 生产可用，减少网络开销 |
| **消息** | RocketMQ单节点 | RocketMQ集群 | 高可用，避免单点故障 |
| **部署** | 无 | K8s + Helm | 标准化运维，弹性伸缩 |
| **API Gateway** | 无 | Spring Cloud Gateway | 统一认证/限流/路由 |
| **可观测性** | 零散日志 | Prometheus + Grafana + Loki | 生产级监控告警 |

---

## 五、模块化拆分方案

### Maven模块结构

```
canvas-parent (pom)
├── canvas-common (jar)           # 共享工具类
│   ├── exception/
│   ├── utils/
│   └── constants/
├── canvas-api (jar)              # API契约(DTO/VO)
│   ├── canvas/
│   ├── cdp/
│   └── wecom/
├── canvas-engine (jar)           # 执行引擎
│   ├── engine/
│   ├── handler/
│   └── lane/
├── canvas-cdp (jar)              # CDP模块
│   ├── tag/
│   ├── audience/
│   └── profile/
├── canvas-wecom (jar)            # 企微模块
│   ├── customer/
│   ├── group/
│   └── message/
├── canvas-dp (jar)               # 数据平台模块
│   ├── pipeline/                 # 数据管道(CDC/同步)
│   ├── olap/                     # OLAP查询(ClickHouse)
│   ├── realtime/                 # 实时计算(Flink)
│   ├── batch/                    # 批量计算(Spark)
│   └── governance/               # 数据治理(血缘/质量)
├── canvas-notification (jar)     # 通知模块
│   ├── channel/
│   └── delivery/
├── canvas-platform (jar)         # 平台元数据
│   ├── tenant/
│   ├── config/
│   └── audit/
└── canvas-app (spring-boot)      # 主应用(聚合)
    └── Application.java
```

### 模块依赖规则

```
canvas-app → canvas-engine, canvas-cdp, canvas-wecom, canvas-dp, canvas-notification, canvas-platform
canvas-engine → canvas-api, canvas-common
canvas-cdp → canvas-api, canvas-common
canvas-wecom → canvas-api, canvas-common
canvas-dp → canvas-api, canvas-common
canvas-notification → canvas-api, canvas-common
canvas-platform → canvas-api, canvas-common
```

---

## 六、数据源隔离方案

### 3库隔离

| 数据库 | 包含表 | 连接池配置 |
|--------|--------|-----------|
| **canvas_db** | canvas, node, edge, execution, message, delivery | HikariCP max=20 |
| **cdp_db** | user, tag, audience, segment, profile | HikariCP max=30 |
| **meta_db** | tenant, config, audit_log, flyway_history | HikariCP max=10 |

### 配置示例

```yaml
spring:
  datasource:
    canvas:
      jdbc-url: jdbc:mysql://localhost:3306/canvas_db
      maximum-pool-size: 20
    cdp:
      jdbc-url: jdbc:mysql://localhost:3306/cdp_db
      maximum-pool-size: 30
    meta:
      jdbc-url: jdbc:mysql://localhost:3306/meta_db
      maximum-pool-size: 10
```

---

## 七、API Gateway设计

### 路由规则

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: canvas-core
          uri: lb://canvas-app
          predicates:
            - Path=/api/v1/canvas/**, /api/v1/execution/**
        - id: cdp
          uri: lb://canvas-app
          predicates:
            - Path=/api/v1/users/**, /api/v1/tags/**, /api/v1/audiences/**
        - id: wecom
          uri: lb://canvas-app
          predicates:
            - Path=/api/v1/wecom/**
        - id: data-platform
          uri: lb://canvas-app
          predicates:
            - Path=/api/v1/data/**
        - id: notification
          uri: lb://canvas-app
          predicates:
            - Path=/api/v1/messages/**, /api/v1/channels/**
```

### 限流配置

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: canvas-core
          filters:
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 100
                redis-rate-limiter.burstCapacity: 200
```

---

## 八、可观测性架构

```
┌─────────────────────────────────────────────────────────────┐
│                      Grafana Dashboard                       │
│  (业务指标/系统指标/告警)                                     │
└─────────────────────────────────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        ▼                     ▼                     ▼
┌───────────────┐   ┌───────────────┐   ┌───────────────┐
│ Prometheus    │   │ Loki          │   │ Tempo         │
│ (指标存储)    │   │ (日志存储)    │   │ (链路追踪)    │
└───────────────┘   └───────────────┘   └───────────────┘
        ▲                     ▲                     ▲
        │                     │                     │
┌─────────────────────────────────────────────────────────────┐
│                    OpenTelemetry SDK                         │
│  (Metrics / Logs / Traces 统一采集)                          │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    Canvas Application                        │
│  (Micrometer + Logback + OTel Java Agent)                   │
└─────────────────────────────────────────────────────────────┘
```

### 关键指标

| 类型 | 指标名 | 用途 |
|------|--------|------|
| 业务 | canvas_execution_total | 画布执行次数 |
| 业务 | canvas_execution_duration | 画布执行耗时 |
| 系统 | jvm_threads_live | JVM线程数 |
| 系统 | http_server_requests_seconds | HTTP请求延迟 |
| 系统 | hikaricp_connections_active | 数据库连接数 |

---

## 九、实施路线图

```
Week 1-2: Maven模块化拆分
  ├── 创建canvas-parent/canvas-common/canvas-api
  ├── 迁移DO到canvas-api
  └── 验证编译通过

Week 3-4: 多数据源配置
  ├── 配置3个HikariCP数据源
  ├── MyBatis-Plus多数据源路由
  └── 验证SQL路由正确

Week 5-6: WebFlux→Spring MVC迁移
  ├── 移除WebFlux依赖
  ├── Controller改为同步
  └── 虚拟线程配置

Week 7-8: API Gateway + 可观测性
  ├── Spring Cloud Gateway配置
  ├── Prometheus + Grafana部署
  └── 告警规则配置

Month 3-4: 数据平台基础设施
  ├── Kafka集群部署(3 Broker)
  ├── MinIO对象存储集群(4节点)
  ├── MySQL CDC管道搭建(Canal→Kafka)
  └── ODS/DWD层Iceberg表创建

Month 5-6: 实时计算 + OLAP
  ├── Flink集群部署(JM+TM)
  ├── 增量人群Bitmap实时更新
  ├── ClickHouse集群部署(2分片)
  └── OLAP查询服务上线(RuleToSqlConverter)

Month 7-9: 批量计算 + 数据治理
  ├── Spark集群部署
  ├── 用户画像宽表 + 人群全量重建
  ├── DataHub血缘+质量监控
  └── DolphinScheduler调度上线

Month 10-12: 数据服务 + 智能分析
  ├── 统一数据API(Tag/Event/Profile/Audience)
  ├── 漏斗分析 + 留存分析
  ├── AI预测模型集成(CLV/流失/倾向)
  └── 全链路压测 + 生产上线
```

---

## 十、相关文档

- [架构师审查报告：8个关键缺失](architect-critical-review.md)
- [服务划分与新应用搭建方案](service-architecture-design.md)
- [架构演进路线图](architecture-evolution-roadmap.md)
- [数据平台架构设计](data-platform-architecture.md)
- [企微SCRM模块设计](wecom-scrm-module-design.md)
- [K8s部署方案](k8s-deployment-plan.md)
- [WebFlux→Spring MVC迁移](webflux-to-mvc-migration.md)
- [多数据源隔离方案](multi-datasource-isolation.md)
