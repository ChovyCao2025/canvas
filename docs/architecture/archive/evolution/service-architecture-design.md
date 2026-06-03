# 服务划分与新应用搭建方案 (2026-06-01)

> **定位**: 从单体到服务化——哪些拆独立服务、哪些新建、服务间如何通信、每个服务的职责边界

---

## 一、总览：12个服务

```
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                              API Gateway (Kong / Spring Cloud Gateway)                │
│                              认证 · 限流 · 路由 · 日志 · 版本管理                        │
└─────────────────────────────────────────────────────────────────────────────────────┘
        │              │              │              │              │
        ▼              ▼              ▼              ▼              ▼
┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐
│ Canvas   │  │ CDP      │  │ WeCom    │  │ Notify   │  │ DataAPI  │
│ 画布编排  │  │ 用户画像  │  │ 企微私域  │  │ 消息触达  │  │ 数据服务  │
│ [拆分]   │  │ [新建]   │  │ [新建]   │  │ [新建]   │  │ [新建]   │
└──────────┘  └──────────┘  └──────────┘  └──────────┘  └──────────┘
        │              │              │              │              │
        └──────────────┼──────────────┼──────────────┼──────────────┘
                       │              │              │
                       ▼              ▼              ▼
              ┌──────────────────────────────────────────┐
              │          Message Bus (RocketMQ)           │
              │  画布事件 · CDP事件 · 企微事件 · 通知事件    │
              └──────────────────────────────────────────┘
                       │              │              │
                       ▼              ▼              ▼
┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐
│ Platform │  │ Scheduler│  │ Analytics│  │ ML-Serve │  │ Data     │
│ 平台管理  │  │ 调度中心  │  │ 分析引擎  │  │ AI推理   │  │ Pipeline │
│ [拆分]   │  │ [新建]   │  │ [新建]   │  │ [新建]   │  │ [新建]   │
└──────────┘  └──────────┘  └──────────┘  └──────────┘  └──────────┘
                                              │
                    ┌─────────────────────────┼─────────────────────────┐
                    ▼                         ▼                         ▼
            ┌──────────┐            ┌──────────┐            ┌──────────┐
            │ FileSvc  │            │ Sandbox  │            │ Webhook  │
            │ 文件服务  │            │ 安全沙箱  │            │ 回调网关  │
            │ [新建]   │            │ [新建]   │            │ [新建]   │
            └──────────┘            └──────────┘            └──────────┘
```

---

## 二、服务清单

### 2.1 拆分自现有单体（2个）

| 服务 | 定位 | 从现有代码拆出 |
|------|------|---------------|
| **Canvas-Engine** | 画布编排+执行引擎 | 核心保留，Handler/Controller/DagEngine |
| **Platform-Meta** | 租户/配置/审计 | TenantController, ConfigController, AuditLog |

### 2.2 全新搭建（10个）

| 服务 | 定位 | 为什么必须新建 |
|------|------|---------------|
| **CDP-Service** | 用户画像+标签+人群 | 当前散落在 canvas-engine 的 cdp 包，无独立服务边界，且需连接 cdp_db |
| **WeCom-Service** | 企微 SCRM 客户管理 | 当前仅部分 Controller+Handler，缺少回调网关、消息收发、素材管理 |
| **Notification-Service** | 多通道消息触达 | 当前 delivery/channel 代码是 stub，需完整重建 |
| **Data-API** | 统一数据查询 API | Tag/Event/Profile/Audience 四大接口，聚合 ClickHouse+Iceberg |
| **XXL-JOB** | 定时任务调度 | 替换 @Scheduled，管理画布定时触发/标签同步/人群重建 |
| **DolphinScheduler** | 数据管道调度 | Spark/Flink 作业 DAG 编排，数据质量检查 |
| **Analytics-Engine** | 漏斗/留存/事件分析 | 需要 ClickHouse 查询优化+缓存，独立计算密集型服务 |
| **ML-Serve** | AI 模型推理 | Python 模型服务，独立技术栈（FastAPI/gRPC） |
| **File-Service** | 素材/附件/导出管理 | 模板图片、人群导出 CSV、企微素材，统一对象存储访问 |
| **Sandbox** | Groovy/脚本安全执行 | 当前 Groovy 沙箱与引擎耦合，独立部署便于资源隔离 |
| **Webhook-Gateway** | 第三方回调统一入口 | 企微回调、渠道回调、客户 webhook，统一验签+路由 |

---

## 三、逐服务详细设计

### 3.1 Canvas-Engine（拆分）

**职责**: 画布 CRUD + DAG 执行引擎 + 节点 Handler

```
canvas-engine/
├── canvas-api/          # REST: 画布CRUD、版本管理、执行触发
├── engine/
│   ├── scheduler/       # DagEngine (拓扑遍历、并行分支、NodeGate)
│   ├── handler/         # 60+ NodeHandler 实现，按功能分组
│   │   ├── action/      # 动作节点 (发消息/打标签/加人群/发券)
│   │   ├── flow/        # 流程控制 (IF/SPLIT/WAIT/GOAL)
│   │   ├── trigger/     # 触发器 (MQ/定时/事件/行为/延迟)
│   │   └── integration/ # 集成节点 (Webhook/企微/外部API)
│   ├── disruptor/       # Disruptor 事件分发
│   ├── lane/            # 执行泳道 (light/standard/heavy/retry)
│   ├── policy/          # 熔断/超时/重试
│   └── delivery/        # 消息投递(调用 Notification-Service)
├── domain/              # Canvas, Node, Edge, Execution 领域对象
└── infra/               # MyBatis Mapper (canvas_db)
```

**端口**: 8081
**数据库**: canvas_db (HikariCP max=20)
**对外 API**:
- `POST /api/v1/canvas` — 创建画布
- `GET /api/v1/canvas/{id}` — 查询画布
- `POST /api/v1/canvas/{id}/publish` — 发布画布
- `POST /api/v1/canvas/{id}/execute` — 执行画布
- `GET /api/v1/execution/{id}` — 查询执行结果

**依赖服务**:
- Notification-Service (消息投递)
- CDP-Service (受众查询)
- WeCom-Service (企微动作)
- XXL-JOB (定时触发)

**发布事件**:
- `canvas.execution.started` — 画布开始执行
- `canvas.execution.completed` — 画布执行完成
- `canvas.node.executed` — 节点执行完成

---

### 3.2 CDP-Service（新建）

**职责**: 用户画像、标签管理、人群计算、身份融合

```
cdp-service/
├── user-api/            # REST: 用户查询、用户360
│   ├── UserController
│   └── ProfileController
├── tag-api/             # REST: 标签CRUD、标签导入、标签分布
│   ├── TagController
│   └── TagGroupController
├── audience-api/        # REST: 人群创建、人群计算、人群导出
│   ├── AudienceController
│   └── AudienceComputeController
├── domain/
│   ├── user/            # UserProfile, UserIdentity
│   ├── tag/             # Tag, TagGroup, TagImport
│   └── audience/        # Audience, AudienceRule, BitmapSegment
├── service/
│   ├── UserService      # 用户CRUD、身份融合(OneID)
│   ├── TagService       # 标签CRUD、标签导入、标签计算
│   ├── AudienceService  # 人群规则管理
│   └── IdentityMerge    # 多身份归并 (手机/邮箱/企微/设备ID→OneID)
├── compute/             # 人群计算引擎
│   ├── OlapAudienceService  # ClickHouse SQL push-down (>1亿用户)
│   ├── BitmapService        # RoaringBitmap 读取/合并
│   └── RuleEvaluator        # 规则引擎(Aviator)本地评估
└── infra/
    ├── mapper/          # cdp_db Mapper
    ├── clickhouse/      # ClickHouse JDBC
    └── redis/           # Bitmap缓存
```

**端口**: 8082
**数据库**: cdp_db (HikariCP max=30) + ClickHouse (OLAP查询)
**缓存**: Redis (RoaringBitmap 人群位图 + 用户标签缓存)

**关键 API**:
- `GET /api/v1/users/{userId}` — 用户 360 视图
- `POST /api/v1/users/identity/resolve` — 身份解析 (手机/邮箱/企微→user_id)
- `GET /api/v1/tags/{userId}` — 用户标签
- `POST /api/v1/tags/import` — 批量导入标签
- `POST /api/v1/audiences` — 创建人群
- `POST /api/v1/audiences/{id}/compute` — 计算人群
- `GET /api/v1/audiences/{id}/users?page=1` — 人群用户列表
- `POST /api/v1/audiences/{id}/export` — 人群导出

**核心设计决策**:

| 决策 | 选择 | 理由 |
|------|------|------|
| 人群计算 <100万 | 本地 Aviator 规则引擎 | 低延迟，无需跨服务 |
| 人群计算 >100万 | ClickHouse SQL push-down | 避免 OOM，支持1亿+ |
| Bitmap 存储 | Redis + CDC 增量更新 | 毫秒级读取，Flink 增量写入 |
| 身份融合(OneID) | Kafka 消费 CDC 事件 | 实时归并多身份 |

---

### 3.3 WeCom-Service（新建）

**职责**: 企微客户管理、群聊运营、欢迎语、朋友圈、素材库

```
wecom-service/
├── customer-api/        # REST: 客户管理
│   ├── CustomerController      # 客户列表/详情/同步
│   └── CustomerTagController   # 企微标签同步
├── group-api/           # REST: 群聊管理
│   ├── GroupChatController     # 群列表/详情/群发
│   └── GroupWelcomeController  # 自动回复/欢迎语
├── message-api/         # REST: 消息管理
│   ├── WelcomeMsgController    # 欢迎语模板
│   ├── BroadcastController     # 群发任务
│   └── MomentController        # 朋友圈任务
├── material-api/        # REST: 素材管理
│   ├── MaterialController      # 素材上传/管理
│   └── TemplateController      # 消息模板
├── callback/            # 企微回调处理
│   ├── CallbackController      # 回调入口 (URL验证+解密)
│   ├── EventRouter             # 事件路由 (加好友/删好友/进群/退群)
│   └── SyncScheduler           # 全量+增量同步
├── domain/
│   ├── customer/        # WeComCustomer, CustomerTag
│   ├── group/           # WeComGroup, GroupMember
│   ├── message/         # WelcomeMsg, BroadcastTask, MomentTask
│   └── material/        # Material, MessageTemplate
├── service/
│   ├── CustomerSyncService     # 全量同步 (分页拉取企微API)
│   ├── EventProcessor          # 回调事件处理
│   ├── BroadcastService        # 群发任务管理
│   └── RateLimitService        # 企微API限流控制
└── infra/
    ├── mapper/          # wecom相关表 (暂放canvas_db)
    ├── wecom-sdk/       # 企微API客户端 (Token管理/重试/限流)
    └── mq/              # 事件发布
```

**端口**: 8083
**数据库**: 可复用 canvas_db 或独立 wecom_db
**缓存**: Redis (企微 Token / 客户列表缓存)

**关键 API**:
- `GET /api/v1/wecom/customers?page=1&size=50` — 客户列表
- `POST /api/v1/wecom/customers/sync` — 触发全量同步
- `POST /api/v1/wecom/broadcast` — 创建群发任务
- `POST /api/v1/wecom/welcome-msg` — 创建欢迎语

**企微回调处理**:
```
企业微信 → POST /api/v1/wecom/callback
  ├── URL验证 (echostr)
  ├── 消息解密 (AES)
  ├── 事件路由:
  │   ├── add_external_contact → 发布 WeComCustomerCreated 事件
  │   ├── del_external_contact → 发布 WeComCustomerDeleted 事件
  │   ├── change_external_chat  → 同步群变更
  │   └── ...
  └── 异步处理 (MQ 解耦)
```

**关键设计**:
- 企微 API 限流严格(2000次/分钟)，需令牌桶限流 + 失败重试队列
- 全量同步使用企微「联系我」+ 客户列表分页拉取
- 增量同步通过回调事件实时处理

---

### 3.4 Notification-Service（新建）

**职责**: 多通道消息触达、模板渲染、发送队列、回执处理

```
notification-service/
├── channel-api/         # REST: 通道管理
│   └── ChannelController
├── message-api/         # REST: 消息发送
│   └── MessageController
├── domain/
│   ├── Message          # 消息实体 (模板+参数→内容)
│   ├── Channel          # 通道配置 (企微/短信/邮件/推送/App-In)
│   ├── Delivery         # 投递记录 (状态/回执)
│   └── Template         # 消息模板 (变量/样式/预览)
├── service/
│   ├── MessageRouter    # 消息路由 (选择通道)
│   ├── TemplateRenderer # 模板渲染 (变量替换+条件逻辑)
│   ├── RateLimiter      # 通道限流 (按通道/租户)
│   └── ReceiptHandler   # 回执处理 (成功/失败/退订)
├── channel/             # 通道适配器
│   ├── ChannelAdapter   # 接口
│   ├── WeComAdapter     # 企微消息 (文本/图文/小程序卡片)
│   ├── SmsAdapter       # 短信 (阿里云/腾讯云)
│   ├── EmailAdapter     # 邮件
│   ├── PushAdapter      # App推送 (极光/个推)
│   ├── InAppAdapter     # 站内信
│   └── WebhookAdapter   # Webhook回调
└── infra/
    ├── mapper/          # message/delivery/channel表
    └── mq/              # 事件消费 + 回执队列
```

**端口**: 8084
**数据库**: 复用 canvas_db (message/delivery/channel_config表)
**MQ**: RocketMQ (发送队列 + 回执队列)

**关键 API**:
- `POST /api/v1/messages/send` — 发送消息
- `GET /api/v1/messages/{id}/status` — 查询投递状态
- `POST /api/v1/channels` — 注册通道配置
- `POST /api/v1/templates` — 创建消息模板

**通道适配器接口**:
```java
public interface ChannelAdapter {
    /** 通道类型标识 */
    String channelType();
    
    /** 发送消息，返回投递ID */
    DeliveryResult send(MessageRequest request);
    
    /** 查询投递状态 */
    DeliveryStatus checkStatus(String deliveryId);
    
    /** 处理回调回执 */
    void handleReceipt(Map<String, String> receiptData);
    
    /** 通道是否可用 */
    boolean isAvailable();
}
```

---

### 3.5 Data-API（新建）

**职责**: 统一数据查询 API，聚合 ClickHouse + Iceberg，对外屏蔽底层数据源

```
data-api/
├── tag-api/             # 标签查询接口
│   └── TagDataController
├── event-api/           # 事件查询接口
│   └── EventDataController
├── profile-api/         # 用户画像接口
│   └── ProfileDataController
├── audience-api/        # 人群数据接口
│   └── AudienceDataController
├── query/
│   ├── QueryEngine      # 查询路由 (选择ClickHouse vs Iceberg)
│   ├── SqlBuilder       # DSL→SQL (防注入)
│   ├── ResultCache      # 查询结果缓存 (Caffeine+Redis)
│   └── QueryLimiter     # 查询限制 (超时/行数/复杂度)
└── infra/
    ├── clickhouse/      # ClickHouse JDBC
    ├── iceberg/         # Iceberg Spark查询
    └── redis/           # 缓存
```

**端口**: 8085
**存储**: ClickHouse (在线查询) + Iceberg/MinIO (离线查询)
**缓存**: Caffeine L1 (5min) + Redis L2 (30min)

**关键 API**:
- `GET /api/v1/data/tags/{userId}` — 查询用户标签
- `POST /api/v1/data/tags/distribution` — 标签分布统计
- `POST /api/v1/data/events/search` — 事件查询
- `POST /api/v1/data/events/aggregate` — 事件聚合 (count/sum/avg by 维度)
- `GET /api/v1/data/profiles/{userId}` — 用户360
- `POST /api/v1/data/audiences/overlap` — 人群重叠分析

**查询路由规则**:
| 查询类型 | 数据量 | 路由目标 | 典型延迟 |
|---------|--------|---------|---------|
| 单用户标签 | 1行 | MySQL cdp_db | <10ms |
| 标签分布 | 聚合 | ClickHouse DWS | <500ms |
| 事件查询 | 明细 | ClickHouse DWD | <1s |
| 人群重叠 | 大集合 | ClickHouse ADS | <2s |
| 历史归档 | 全量 | Iceberg (Spark) | 10s-60s |

---

### 3.6 定时任务调度：XXL-JOB + DolphinScheduler（使用开源，不自己搭建）

**为什么不用自研 Scheduler-Center**：写一个调度引擎的 Demo 只需要 2 天，但做到生产级（任务分片、失败重试、可视化 Dashboard、告警通道）需要 3 年以上。开源方案已经过数千家公司验证。

#### XXL-JOB（业务定时任务）

**职责**: 画布定时触发、标签同步、人群重建、数据清理等业务定时任务

```
XXL-JOB 部署架构:
┌─────────────────────────────────────────────────────┐
│  xxl-job-admin (独立部署, 2 replicas)                │
│  - 任务管理 Dashboard                                │
│  - 调度中心 (Quartz)                                  │
│  - 失败重试 + 告警                                    │
│  - 数据库: meta_db (xxl_job_* 表)                    │
└──────────────┬──────────────────────────────────────┘
               │ HTTP 调度 (RPC)
     ┌─────────┼─────────┬─────────┐
     ▼         ▼         ▼         ▼
┌─────────┐ ┌──────┐ ┌──────┐ ┌──────┐
│ Canvas  │ │ CDP  │ │ WeCom│ │ 其他 │  ← 各服务内嵌 XXL-JOB Executor
│ Engine  │ │ Svc  │ │ Svc  │ │ 服务 │
└─────────┘ └──────┘ └──────┘ └──────┘
```

```xml
<!-- 每个服务引入 -->
<dependency>
    <groupId>com.xuxueli</groupId>
    <artifactId>xxl-job-core</artifactId>
    <version>2.4.1</version>
</dependency>
```

```yaml
# 每个服务配置
xxl:
  job:
    admin:
      addresses: http://xxl-job-admin:8080/xxl-job-admin
    executor:
      appname: ${spring.application.name}
      port: 9999
      logpath: /data/applogs/xxl-job
      logretentiondays: 30
```

```java
// 画布定时触发
@Component
public class CanvasScheduledJob {
    
    @XxlJob("canvasScheduledTriggerJob")
    public void execute() {
        String param = XxlJobHelper.getJobParam();
        Long canvasId = Long.parseLong(param);
        // 虚拟线程下执行，不阻塞 XXL-JOB 回调
        canvasExecutionService.execute(canvasId);
    }
    
    // 分片广播：人群计算分片执行
    @XxlJob("audienceRebuildJob")
    public void audienceRebuild() {
        // XXL-JOB 分片参数
        int shardIndex = XxlJobHelper.getShardIndex();  // 0, 1, 2, ...
        int shardTotal = XxlJobHelper.getShardTotal();  // 4
        // 每个分片处理 1/4 的用户
        audienceService.rebuildShard(shardIndex, shardTotal);
    }
}
```

#### DolphinScheduler（数据平台任务调度）

**职责**: Spark 批量任务、Flink 作业管理、数据质量检查 DAG

```
适用场景:
  - Spark 画像宽表重建 (每日 02:00)
  - Spark 人群全量重建 (每日 03:00)
  - Flink 作业停止/启动管理
  - 数据质量检查 DAG (每日 06:00)
  - DataX 数据同步任务
  - 任务依赖链: 质量检查通过 → 画像重建 → 人群重建 → 数据导出
```

#### 迁移对照

| 原设计 | 改为 | 理由 |
|--------|------|------|
| Scheduler-Center 自研服务 | **XXL-JOB** + **DolphinScheduler** | 开源验证过的方案，不要重造轮子 |
| 服务端口 8086 | 取消该服务 | 调度平台独立部署，不占服务端口 |
| 自建 Dashboard | XXL-JOB 自带 Dashboard | 开箱即用 |
| meta_db 的 scheduled_task 表 | XXL-JOB 在 meta_db 自动建表 | xxl_job_group / xxl_job_info / xxl_job_log 等 |

---

### 3.7 Analytics-Engine（新建）

**职责**: 漏斗分析、留存分析、事件分析、自定义报表

```
analytics-engine/
├── funnel-api/          # 漏斗分析
│   └── FunnelController
├── retention-api/       # 留存分析
│   └── RetentionController
├── event-analyzer/      # 事件分析
│   └── EventAnalyzerController
├── report-api/          # 报表管理
│   └── ReportController
├── service/
│   ├── FunnelService    # 漏斗计算 (windowFunnel)
│   ├── RetentionService # 留存计算 (retention)
│   ├── EventStatsService # 事件统计
│   └── ReportCache      # 报表缓存策略
└── infra/
    └── clickhouse/      # ClickHouse JDBC (只读)
```

**端口**: 8087
**存储**: ClickHouse (分析查询，只读)
**缓存**: Redis (报表缓存 1h-24h)

**关键 API**:
- `POST /api/v1/analytics/funnel` — 漏斗分析
- `POST /api/v1/analytics/retention` — 留存分析
- `POST /api/v1/analytics/events/stats` — 事件统计分析

**ClickHouse 查询示例**（漏斗）:
```sql
SELECT windowFunnel(86400)(time, 
    event_type = 'page_view',
    event_type = 'add_cart', 
    event_type = 'place_order',
    event_type = 'pay_success'
) AS level, count() 
FROM dwd_user_event 
WHERE dt >= '2026-05-01' AND dt <= '2026-05-31'
GROUP BY level ORDER BY level;
```

---

### 3.8 ML-Serve（新建，Python）

**职责**: AI 模型推理服务，CLV 预测/流失预测/购买倾向

```
ml-serve/
├── api/                 # FastAPI 推理接口
│   ├── router.py              # API路由
│   └── schemas.py             # 请求/响应模型
├── models/              # 模型文件
│   ├── clv_model.pkl          # CLV预测模型
│   ├── churn_model.pkl        # 流失预测模型
│   └── propensity_model.pkl   # 购买倾向模型
├── service/
│   ├── predictor.py           # 模型加载+推理
│   ├── feature_builder.py     # 特征工程
│   └── model_registry.py      # 模型版本管理
└── train/               # 训练脚本 (离线运行)
    ├── clv_train.py
    ├── churn_train.py
    └── feature_engineering.py
```

**端口**: 8090
**协议**: gRPC (高性能) + REST (调试)
**部署**: 独立 Pod，GPU 可选（初期 CPU 可满足）

**推理 API** (gRPC):
```protobuf
service MLService {
  rpc PredictCLV(CLVRequest) returns (CLVResponse);
  rpc PredictChurn(ChurnRequest) returns (ChurnResponse);
  rpc PredictPropensity(PropensityRequest) returns (PropensityResponse);
  rpc BatchPredict(stream BatchRequest) returns (stream BatchResponse);
}
```

---

### 3.9 File-Service（新建）

**职责**: 统一对象存储访问——素材上传、人群导出 CSV、企微素材管理

```
file-service/
├── upload-api/          # 文件上传
│   └── UploadController
├── download-api/        # 文件下载
│   └── DownloadController
├── service/
│   ├── FileStore        # 对象存储适配 (MinIO/OSS/S3)
│   ├── ExportService    # 大数据量导出 (流式CSV)
│   └── ImageProcessor   # 图片处理 (缩略图/水印/格式转换)
└── infra/
    └── minio/           # MinIO Client
```

**端口**: 8088
**存储**: MinIO (默认) 或 云 OSS (生产推荐)

**关键 API**:
- `POST /api/v1/files/upload` — 上传文件 (返回 fileId+URL)
- `GET /api/v1/files/{fileId}` — 下载/预览文件
- `POST /api/v1/files/export/audience` — 人群导出 (异步，返回下载链接)

---

### 3.10 Sandbox（新建）

**职责**: Groovy/JavaScript 脚本安全沙箱执行，资源隔离

```
sandbox/
├── execute-api/         # 脚本执行
│   └── SandboxController
├── service/
│   ├── GroovySandbox    # Groovy 脚本执行 (超时5s/max输出64KB)
│   ├── JsSandbox        # 可选: JavaScript引擎
│   ├── ResourceMonitor  # CPU/内存监控
│   └── ScriptValidator  # 脚本安全检查 (黑名单关键词)
└── security/
    ├── ClassFilter      # 禁止类加载
    ├── ImportFilter     # 禁止import
    └── LoopDetector     # 无限循环检测
```

**端口**: 8089
**为什么独立部署**: 资源隔离（CPU/内存限制）、安全隔离（禁止访问文件系统/网络）、崩溃不影响主服务

---

### 3.11 Webhook-Gateway（新建）

**职责**: 第三方回调统一入口——验签、解密、路由、重试

```
webhook-gateway/
├── callback-api/        # 回调接收
│   └── WebhookController
├── service/
│   ├── SignatureVerifier  # 签名验签 (企微/自定义)
│   ├── Decryptor          # 消息解密
│   ├── EventRouter        # 事件路由 → MQ
│   └── RetryQueue         # 失败重试
└── registry/
    └── WebhookRegistry    # 注册的webhook类型
```

**端口**: 8091

**回调流程**:
```
外部回调 → Webhook-Gateway
  → 验签(SHA1/MD5/HMAC)
  → 解密(AES)
  → 事件路由 → RocketMQ
    → WeCom-Service (企微事件)
    → Notification-Service (渠道回执)
    → XXL-JOB (触发事件)
    → WeCom-Service (企微事件)
```

---

### 3.12 Platform-Meta（拆分）

**职责**: 租户管理、系统配置、操作审计、权限管理

```
platform-meta/
├── tenant-api/          # 租户管理
│   └── TenantController
├── config-api/          # 系统配置
│   └── ConfigController
├── audit-api/           # 审计日志
│   └── AuditController
├── auth-api/            # 认证授权
│   ├── AuthController
│   └── PermissionController
├── domain/
│   ├── Tenant, TenantConfig, TenantQuota, TenantUsage
│   ├── AuditLog, OperationRecord
│   └── Role, Permission, UserRole
└── infra/
    └── mapper/          # meta_db
```

**端口**: 8092
**数据库**: meta_db (HikariCP max=10)

---

## 四、服务间通信

### 4.1 通信方式矩阵

| 场景 | 方式 | 说明 |
|------|------|------|
| 同步查询（低延迟） | REST (HTTP/2) | CDP-Service 查用户标签 |
| 同步查询（高性能） | gRPC | ML-Serve 推理调用 |
| 异步事件 | RocketMQ | 画布事件→CDP打标签 |
| 大数据量传输 | 对象存储共享 | 人群导出 CSV → MinIO → File-Service |
| 回调通知 | Webhook | Notification-Service 回执回调 |

### 4.2 事件总线 (RocketMQ Topic 设计)

| Topic | 生产者 | 消费者 | 说明 |
|-------|--------|--------|------|
| `canvas.execution` | Canvas-Engine | CDP, Notification, Platform | 画布执行事件 |
| `cdp.user.event` | CDP-Service | Analytics, ML-Serve | 用户数据变更 |
| `cdp.audience.change` | CDP-Service | Canvas-Engine, Notification | 人群变更 |
| `wecom.callback` | Webhook-Gateway | WeCom-Service | 企微回调事件 |
| `wecom.contact.change` | WeCom-Service | CDP-Service | 企微客户变更→同步CDP |
| `notification.delivery` | Notification | Canvas-Engine, Analytics | 消息投递状态 |

### 4.3 服务发现

```
各服务启动 → 注册到 Nacos/Consul → API Gateway 动态路由
健康检查: /actuator/health (5s间隔)
优雅下线: /actuator/shutdown (等待进行中请求完成)
```

---

## 五、部署架构

### 5.1 服务部署规格

| 服务 | 实例数 | CPU | 内存 | JVM 参数 |
|------|--------|-----|------|---------|
| Canvas-Engine | 3-5 | 2 | 4Gi | -Xmx2g -XX:+UseZGC |
| CDP-Service | 3 | 2 | 4Gi | -Xmx2g -XX:+UseZGC |
| WeCom-Service | 2 | 1 | 2Gi | -Xmx1g |
| Notification-Service | 2 | 1 | 2Gi | -Xmx1g |
| Data-API | 2-3 | 2 | 4Gi | -Xmx2g |
| Analytics-Engine | 2 | 2 | 4Gi | -Xmx2g |
| ML-Serve | 2 | 2 | 4Gi | Python, 无JVM |
| File-Service | 2 | 1 | 2Gi | -Xmx1g |
| Sandbox | 2 | 1 | 1Gi | -Xmx512m, 严格资源限制 |
| Webhook-Gateway | 2 | 0.5 | 1Gi | -Xmx512m |
| Platform-Meta | 2 | 1 | 2Gi | -Xmx1g |

**合计**: ~22 实例, ~28 CPU, ~56Gi 内存 (不含数据平台组件)

### 5.2 K8s 部署结构

```
canvas-platform/
├── canvas-engine/       # Deployment + Service + HPA
├── cdp-service/         # Deployment + Service + HPA
├── wecom-service/       # Deployment + Service
├── notification-svc/    # Deployment + Service
├── data-api/            # Deployment + Service + HPA
├── scheduler-center/    # StatefulSet + Service (避免重复执行)
├── analytics-engine/    # Deployment + Service
├── ml-serve/            # Deployment + Service (Python)
├── file-service/        # Deployment + Service
├── sandbox/             # Deployment + ResourceQuota
├── webhook-gateway/     # Deployment + Service
├── platform-meta/       # Deployment + Service
├── api-gateway/         # Spring Cloud Gateway
└── shared/
    ├── rocketmq/
    ├── redis/
    ├── mysql/
    └── config/          # Nacos Config
```

---

## 六、实施优先级

### 6.1 三批上线

**第一批 (Phase 1, Week 1-8) — 4个服务**:
```
Canvas-Engine (拆分)
Platform-Meta (拆分)
WeCom-Service (新建, 核心私域能力)
API Gateway
```
上线后即可支撑企微私域运营基本场景。

**第二批 (Phase 2, Month 3-6) — 5个服务**:
```
CDP-Service (新建, 画像+人群)
Notification-Service (新建, 多通道)
Data-API (新建, 数据查询)
File-Service (新建, 素材管理)
Scheduler-Center (新建, 调度)
```
上线后完成数据闭环+消息触达闭环。

**第三批 (Phase 3+, Month 7+) — 3个服务**:
```
Analytics-Engine (新建, 分析)
ML-Serve (新建, AI)
Sandbox (新建, 安全)
Webhook-Gateway (新建, 统一回调)
```
上线后具备分析+AI能力。

### 6.2 依赖关系

```
第一批: Canvas-Engine ← 无外部依赖
        Platform-Meta ← MySQL meta_db
        WeCom-Service ← Canvas-Engine (获取画布触发), Platform-Meta (租户配置)
        
第二批: CDP-Service ← Canvas-Engine (受众查询), Kafka (CDC)
        Notification-Service ← Canvas-Engine (消息发送), WeCom-Service (企微通道)
        Data-API ← CDP-Service (数据源), ClickHouse
        File-Service ← MinIO
        Scheduler-Center ← 调用其他所有服务

第三批: Analytics-Engine ← Data-API (ClickHouse)
        ML-Serve ← CDP-Service (训练数据)
        Sandbox ← Canvas-Engine (脚本执行需求)
        Webhook-Gateway ← WeCom-Service (路由)
```

---

## 七、后端项目文件夹结构

### 7.1 整体 Maven 模块

```
backend/
├── pom.xml                          # canvas-parent: 统一版本管理
├── canvas-common/                   # 公共模块 (jar)
│   ├── pom.xml
│   └── src/main/java/com/canvas/common/
│       ├── exception/               # 统一异常 (BusinessException, SystemException)
│       ├── utils/                   # 工具类 (JsonUtils, DateUtils, IdGenerator)
│       ├── constants/               # 常量 (Constants, ErrorCode)
│       └── context/                 # 上下文 (TenantContext, UserContext)
│
├── canvas-api/                      # API 契约模块 (jar)
│   ├── pom.xml
│   └── src/main/java/com/canvas/api/
│       ├── dto/                     # 传输对象 (Request/Response)
│       │   ├── canvas/              # 画布相关 DTO
│       │   ├── cdp/                 # CDP 相关 DTO
│       │   ├── wecom/               # 企微相关 DTO
│       │   ├── notification/        # 通知相关 DTO
│       │   └── data/                # 数据API DTO
│       ├── vo/                      # 视图对象
│       │   ├── canvas/              # CanvasVO, NodeVO, EdgeVO, ExecutionVO
│       │   ├── cdp/                 # UserVO, TagVO, AudienceVO
│       │   ├── wecom/               # CustomerVO, GroupVO
│       │   └── notification/        # MessageVO, DeliveryVO
│       ├── enums/                   # 枚举 (NodeType, ExecutionStatus, ChannelType)
│       └── feign/                   # Feign 接口定义 (服务间调用)
│           ├── CanvasFeignClient
│           ├── CdpFeignClient
│           ├── WeComFeignClient
│           ├── NotificationFeignClient
│           └── DataApiFeignClient
│
├── canvas-engine/                   # 画布编排服务 (spring-boot)
│   ├── pom.xml
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── db/migration/canvas/     # Flyway (canvas_db)
│   └── src/main/java/com/canvas/engine/
│       ├── CanvasApplication.java
│       ├── controller/              # REST 接口
│       │   ├── CanvasController     # 画布 CRUD
│       │   ├── CanvasVersionController # 版本管理
│       │   ├── ExecutionController  # 执行触发+查询
│       │   └── HealthController
│       ├── engine/
│       │   ├── scheduler/           # DagEngine (拓扑遍历/并行分支/NodeGate)
│       │   ├── handler/             # 60+ NodeHandler
│       │   │   ├── action/          # 动作节点 (Message/SendTag/JoinAudience/Coupon)
│       │   │   ├── flow/            # 流程控制 (IfNode/SplitNode/WaitNode/GoalNode)
│       │   │   ├── trigger/         # 触发器 (Mq/Scheduled/Event/Behavior/Delay)
│       │   │   └── integration/     # 集成节点 (Webhook/WeCom/ExternalApi)
│       │   ├── disruptor/           # Disruptor 事件分发
│       │   ├── lane/                # 执行泳道
│       │   ├── policy/              # 熔断/超时/重试
│       │   ├── rule/                # 规则评估
│       │   └── audience/            # 受众解析
│       ├── domain/                  # 领域对象
│       │   ├── Canvas, CanvasVersion, CanvasNode, CanvasEdge
│       │   ├── ExecutionContext, NodeResult
│       │   ├── CircuitBreakerState, LaneState, WaitState
│       │   └── MarketingPolicy, ExperimentRecord
│       ├── repository/              # MyBatis-Plus Mapper
│       │   ├── CanvasMapper
│       │   ├── NodeMapper
│       │   ├── EdgeMapper
│       │   ├── ExecutionMapper
│       │   └── ...
│       └── client/                  # Feign 客户端调用
│           ├── NotificationClient   # 调用 notification-service
│           ├── CdpClient            # 调用 cdp-service
│           └── WeComClient          # 调用 wecom-service
│
├── canvas-cdp/                      # 用户画像服务 (spring-boot, 新建)
│   ├── pom.xml
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── db/migration/cdp/        # Flyway (cdp_db)
│   └── src/main/java/com/canvas/cdp/
│       ├── CdpApplication.java
│       ├── controller/
│       │   ├── UserController       # 用户查询/身份融合
│       │   ├── TagController        # 标签 CRUD/导入
│       │   ├── TagGroupController   # 标签分组
│       │   ├── AudienceController   # 人群 CRUD
│       │   └── AudienceComputeController # 人群计算
│       ├── service/
│       │   ├── UserService          # 用户 CRUD、身份融合(OneID)
│       │   ├── TagService           # 标签 CRUD、导入、计算
│       │   ├── AudienceService      # 人群规则管理
│       │   ├── IdentityMergeService # 多身份归并
│       │   └── OlapAudienceService  # ClickHouse SQL push-down 人群计算
│       ├── domain/
│       │   ├── user/                # UserProfile, UserIdentity, UserAttribute
│       │   ├── tag/                 # Tag, TagGroup, UserTagRel, TagImportRecord
│       │   └── audience/            # Audience, AudienceRule, BitmapSegment
│       ├── repository/
│       │   ├── UserMapper
│       │   ├── TagMapper
│       │   ├── AudienceMapper
│       │   └── BitmapMapper
│       ├── compute/
│       │   ├── BitmapService        # RoaringBitmap 读取/合并
│       │   ├── RuleEvaluator        # Aviator 规则引擎
│       │   └── RuleToSqlConverter   # 规则 → ClickHouse SQL
│       └── infra/
│           ├── clickhouse/          # ClickHouse JDBC
│           └── redis/               # Bitmap 缓存
│
├── canvas-wecom/                    # 企微私域服务 (spring-boot, 新建)
│   ├── pom.xml
│   ├── src/main/resources/
│   │   └── application.yml
│   └── src/main/java/com/canvas/wecom/
│       ├── WeComApplication.java
│       ├── controller/
│       │   ├── CustomerController   # 客户管理
│       │   ├── GroupChatController  # 群聊管理
│       │   ├── WelcomeMsgController # 欢迎语
│       │   ├── BroadcastController  # 群发任务
│       │   ├── MaterialController   # 素材管理
│       │   └── CallbackController   # 企微回调入口 (URL验证+解密)
│       ├── callback/
│       │   ├── EventRouter          # 事件路由 (加好友/删好友/进群/退群)
│       │   ├── EventProcessor       # 事件处理器接口
│       │   ├── AddContactProcessor  # 加好友处理
│       │   ├── DeleteContactProcessor # 删好友处理
│       │   └── ChangeGroupProcessor # 群变更处理
│       ├── service/
│       │   ├── CustomerSyncService  # 全量+增量同步
│       │   ├── BroadcastService     # 群发任务管理
│       │   ├── MaterialService      # 素材管理
│       │   └── RateLimitService     # 企微 API 限流控制
│       ├── domain/
│       │   └── WeComCustomer, WeComGroup, BroadcastTask, Material, WelcomeMsg
│       ├── repository/
│       │   ├── WeComCustomerMapper
│       │   ├── WeComGroupMapper
│       │   └── BroadcastTaskMapper
│       └── infra/
│           └── wecom/               # 企微 SDK 客户端
│               ├── WeComApiClient   # API 调用封装 (Token管理/重试/限流)
│               ├── WeComCryptUtil   # 加解密工具
│               └── WeComConfig      # 企微配置
│
├── canvas-notification/             # 消息触达服务 (spring-boot, 新建)
│   ├── pom.xml
│   ├── src/main/resources/
│   │   └── application.yml
│   └── src/main/java/com/canvas/notification/
│       ├── NotificationApplication.java
│       ├── controller/
│       │   ├── MessageController    # 消息发送
│       │   ├── ChannelController    # 通道管理
│       │   └── TemplateController   # 模板管理
│       ├── service/
│       │   ├── MessageRouter        # 消息路由 (选择通道)
│       │   ├── TemplateRenderer     # 模板渲染 (变量替换+条件逻辑)
│       │   ├── RateLimiter          # 通道+租户限流
│       │   └── ReceiptHandler       # 回执处理 (成功/失败/退订)
│       ├── channel/                 # 通道适配器
│       │   ├── ChannelAdapter       # 接口
│       │   ├── WeComAdapter         # 企微消息
│       │   ├── SmsAdapter           # 短信 (阿里云/腾讯云)
│       │   ├── EmailAdapter         # 邮件
│       │   ├── PushAdapter          # App 推送 (极光/个推)
│       │   ├── InAppAdapter         # 站内信
│       │   └── WebhookAdapter       # Webhook 回调
│       ├── domain/
│       │   └── Message, Channel, Delivery, Template
│       └── repository/
│           ├── MessageMapper
│           ├── ChannelMapper
│           └── DeliveryMapper
│
├── canvas-data-api/                 # 统一数据 API (spring-boot, 新建)
│   ├── pom.xml
│   ├── src/main/resources/
│   │   └── application.yml
│   └── src/main/java/com/canvas/data/
│       ├── DataApiApplication.java
│       ├── controller/
│       │   ├── TagDataController    # 标签查询
│       │   ├── EventDataController  # 事件查询
│       │   ├── ProfileDataController # 用户360
│       │   └── AudienceDataController # 人群数据
│       ├── query/
│       │   ├── QueryEngine          # 查询路由 (ClickHouse vs Iceberg)
│       │   ├── SqlBuilder           # DSL→SQL (防注入)
│       │   ├── ResultCache          # 查询结果缓存
│       │   └── QueryLimiter         # 查询限制 (超时/行数/复杂度)
│       └── infra/
│           ├── clickhouse/          # ClickHouse JDBC
│           └── iceberg/             # Iceberg Spark 查询
│
├── canvas-scheduler/                # 调度中心 (spring-boot, 新建)
│   ├── pom.xml
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── db/migration/meta/       # Flyway (meta_db - scheduled_task)
│   └── src/main/java/com/canvas/scheduler/
│       ├── SchedulerApplication.java
│       ├── controller/
│       │   ├── JobController        # 任务 CRUD
│       │   ├── JobLogController     # 执行日志
│       │   └── TriggerController    # 触发器管理
│       ├── service/
│       │   ├── JobScheduler         # 调度引擎
│       │   ├── JobExecutor          # 任务执行器 (HTTP/MQ/Flink/Spark)
│       │   └── DAGResolver          # 依赖拓扑排序
│       ├── domain/
│       │   └── Job, Trigger, JobLog, JobDependency
│       ├── repository/
│       │   └── JobMapper, JobLogMapper
│       └── executor/
│           ├── HttpExecutor         # 调用内部服务 API
│           ├── MqExecutor           # 发送 RocketMQ 消息
│           ├── FlinkExecutor        # 提交 Flink Job
│           └── SparkExecutor        # 提交 Spark Job
│
├── canvas-analytics/                # 分析引擎 (spring-boot, 新建)
│   ├── pom.xml
│   ├── src/main/resources/
│   │   └── application.yml
│   └── src/main/java/com/canvas/analytics/
│       ├── AnalyticsApplication.java
│       ├── controller/
│       │   ├── FunnelController     # 漏斗分析
│       │   ├── RetentionController  # 留存分析
│       │   ├── EventAnalyzerController # 事件分析
│       │   └── ReportController     # 报表管理
│       ├── service/
│       │   ├── FunnelService        # funnel/留存计算
│       │   ├── RetentionService
│       │   ├── EventStatsService    # 事件统计
│       │   └── ReportCache          # 报表缓存
│       └── infra/
│           └── clickhouse/          # ClickHouse 只读
│
├── canvas-ml-serve/                 # AI 推理服务 (Python FastAPI, 新建)
│   ├── requirements.txt
│   ├── Dockerfile
│   ├── api/
│   │   ├── router.py               # API 路由
│   │   └── schemas.py              # Schema
│   ├── models/
│   │   ├── clv_model.pkl
│   │   ├── churn_model.pkl
│   │   └── propensity_model.pkl
│   ├── service/
│   │   ├── predictor.py            # 模型加载+推理
│   │   ├── feature_builder.py      # 特征工程
│   │   └── model_registry.py       # 模型版本管理
│   └── train/                      # 离线训练脚本
│       ├── clv_train.py
│       ├── churn_train.py
│       └── feature_engineering.py
│
├── canvas-file/                     # 文件服务 (spring-boot, 新建)
│   ├── pom.xml
│   ├── src/main/resources/
│   │   └── application.yml
│   └── src/main/java/com/canvas/file/
│       ├── FileApplication.java
│       ├── controller/
│       │   ├── UploadController     # 上传
│       │   └── DownloadController   # 下载/导出
│       ├── service/
│       │   ├── FileStore            # 对象存储适配 (MinIO/OSS/S3)
│       │   ├── ExportService        # 大数据量导出 (流式CSV)
│       │   └── ImageProcessor       # 图片处理 (缩略图/水印)
│       └── infra/
│           └── minio/               # MinIO Client
│
├── canvas-sandbox/                  # 安全沙箱 (spring-boot, 新建)
│   ├── pom.xml
│   ├── src/main/resources/
│   │   └── application.yml
│   └── src/main/java/com/canvas/sandbox/
│       ├── SandboxApplication.java
│       ├── controller/
│       │   └── SandboxController    # 脚本执行
│       ├── service/
│       │   ├── GroovySandbox        # Groovy 执行 (超时5s/max输出64KB)
│       │   ├── ResourceMonitor      # CPU/内存监控
│       │   └── ScriptValidator      # 脚本安全检查
│       └── security/
│           ├── ClassFilter          # 禁止类加载
│           ├── ImportFilter         # 禁止 import
│           └── LoopDetector         # 无限循环检测
│
├── canvas-webhook/                  # Webhook 回调网关 (spring-boot, 新建)
│   ├── pom.xml
│   ├── src/main/resources/
│   │   └── application.yml
│   └── src/main/java/com/canvas/webhook/
│       ├── WebhookApplication.java
│       ├── controller/
│       │   └── WebhookController    # 回调入口
│       ├── service/
│       │   ├── SignatureVerifier    # 签名验签
│       │   ├── Decryptor            # 消息解密
│       │   ├── EventRouter          # 事件路由→MQ
│       │   └── RetryQueue           # 失败重试
│       └── registry/
│           └── WebhookRegistry      # 注册的 webhook 类型
│
├── canvas-platform/                 # 平台管理服务 (spring-boot, 拆分)
│   ├── pom.xml
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── db/migration/meta/       # Flyway (meta_db)
│   └── src/main/java/com/canvas/platform/
│       ├── PlatformApplication.java
│       ├── controller/
│       │   ├── TenantController     # 租户管理
│       │   ├── ConfigController     # 系统配置
│       │   ├── AuditController      # 审计日志
│       │   └── AuthController       # 认证授权
│       ├── service/
│       │   ├── TenantService
│       │   ├── AuditService
│       │   └── AuthService
│       ├── domain/
│       │   └── Tenant, TenantConfig, AuditLog, Role, Permission
│       ├── repository/
│       │   ├── TenantMapper
│       │   ├── AuditLogMapper
│       │   └── RoleMapper
│       └── security/
│           ├── JwtTokenProvider
│           ├── SecurityConfig
│           └── TenantInterceptor
│
└── canvas-gateway/                  # API 网关 (spring-boot)
    ├── pom.xml
    ├── src/main/resources/
    │   └── application.yml
    └── src/main/java/com/canvas/gateway/
        ├── GatewayApplication.java
        ├── filter/
        │   ├── AuthFilter           # JWT 认证
        │   ├── RateLimitFilter      # 限流
        │   ├── RequestLogFilter     # 请求日志
        │   └── TenantFilter         # 租户隔离
        └── config/
            ├── RouteConfig          # 路由配置
            └── CorsConfig           # 跨域配置
```

### 7.2 构建顺序

```
1. canvas-common                       ← 零依赖
2. canvas-api                          ← 依赖 canvas-common
3. canvas-platform (meta_db)           ← 依赖 canvas-api
4. canvas-engine (canvas_db)           ← 依赖 canvas-api
5. canvas-cdp (cdp_db)                 ← 依赖 canvas-api
6. canvas-wecom                        ← 依赖 canvas-api + Feign
7. canvas-notification                 ← 依赖 canvas-api + Feign
8. canvas-data-api                     ← 依赖 canvas-api
9. canvas-scheduler                    ← 依赖 canvas-api + Feign
10. canvas-analytics                   ← 依赖 canvas-api
11. canvas-file                        ← 依赖 canvas-api
12. canvas-sandbox                     ← 依赖 canvas-api
13. canvas-webhook                     ← 依赖 canvas-api
14. canvas-gateway                     ← 独立
15. canvas-ml-serve (Python)           ← 独立技术栈
```

### 7.3 子项目启动顺序

```
Phase 1:
  1. MySQL + Redis + RocketMQ (docker-compose)
  2. canvas-platform (:8092)          ← meta_db Flyway
  3. canvas-engine (:8081)            ← canvas_db Flyway
  4. canvas-gateway (:8080)           ← API 入口

Phase 2:
  5. canvas-cdp (:8082)               ← cdp_db Flyway
  6. canvas-wecom (:8083)             ← 企微回调
  7. canvas-notification (:8084)     ← 消息通道
  8. canvas-data-api (:8085)          ← ClickHouse/Iceberg
  9. canvas-scheduler (:8086)         ← 调度引擎
  10. canvas-file (:8088)             ← 对象存储

Phase 3:
  11. canvas-analytics (:8087)        ← ClickHouse 只读
  12. canvas-sandbox (:8089)          ← 安全沙箱
  13. canvas-webhook (:8091)          ← 统一回调
  14. canvas-ml-serve (:8090)         ← Python 服务
```

### 7.4 统一配置

所有 Spring Boot 服务共享以下基础配置（通过 Nacos Config 统一管理）:

```yaml
# 共用的公共配置
server:
  shutdown: graceful
spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s
  threads:
    virtual:
      enabled: true
  datasource:
    # 各服务按域使用不同数据库，通过 dynamic-datasource @DS 注解路由
    dynamic:
      primary: master
      strict: true
  jackson:
    time-zone: Asia/Shanghai
    date-format: yyyy-MM-dd HH:mm:ss
    default-property-inclusion: non_null
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  metrics:
    tags:
      application: ${spring.application.name}
logging:
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] [%X{traceId}] %-5level %logger{36} - %msg%n"
```

---

## 八、相关文档

- [架构演进路线图](architecture-evolution-roadmap.md)
- [目标架构总览](target-architecture-overview.md)
- [数据平台架构设计](data-platform-architecture.md)
- [K8s部署方案](k8s-deployment-plan.md)
- [企微SCRM模块设计](wecom-scrm-module-design.md)
