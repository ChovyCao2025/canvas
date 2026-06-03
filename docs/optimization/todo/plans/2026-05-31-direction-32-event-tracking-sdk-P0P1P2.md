# 方向㉜：事件追踪SDK与行为数据采集 — 功能清单

> 定位：从"内部 event_log 写表"升级为"完整的事件追踪与行为数据采集平台"——多端SDK(Web/Mobile/Server)+Schema Registry+数据校验管线+数据仓库同步+AI Agent上下文
> 策略评估：事件追踪SDK是SaaS营销平台的数据基础设施，2026年数据驱动营销的核心竞争力在于"数据质量+数据新鲜度"
> 竞品对标：Snowplow(35+SDK、400天Cookie、sub-0.1%错误率、AI Agent上下文)+RudderStack(15+SDK、200+集成、Warehouse-First CDP、Segment兼容)
> 建议：**P1建议做**，⑨营销数据中台+⑬实时画像引擎上线前必备，高质量行为数据是所有分析/归因/画像的基础

---

## 参考出处

| 出处 | 核心观点 | 链接 |
|------|---------|------|
| Snowplow: Behavioral Data Platform 2026 | 35+ SDK + Iglu Schema Registry + 400天first-party cookie + sub-0.1%错误率 + AI Agent上下文交付 + 原子表设计+dbt模型 | https://snowplow.io/ |
| RudderStack: Warehouse-First CDP 2026 | 15+ SDK + 200+集成目标 + Segment兼容API + Reverse ETL + 仓库原生架构 | https://www.rudderstack.com/ |
| Portable.io: RudderStack vs Snowplow 2026 | Snowplow擅长细粒度行为数据+自定义Schema+AI就绪；RudderStack提供完整CDP生命周期+Reverse ETL | https://portable.io/learn/rudderstack-vs-snowplow-comparison |
| Modern DataTools: Snowplow vs RudderStack 2026 | Snowplow: 原子表+dbt模型可SQL直接查询; RudderStack: 每种事件独立表→需20+表JOIN做归因 | https://www.modern-datatools.com/compare/snowplow-vs-rudderstack |
| Snowplow vs RudderStack Comparison 2026 | Snowplow 400天 vs RudderStack 7-14天(ITP限制); sub-0.1% vs 3-4%错误率; 自托管/私有SaaS/混合 vs 仅SaaS | https://snowplow.io/comparisons/snowplow-vs-rudderstack |
| RudderStack: Snowplow Migration Guide 2026 | 迁移指南: Snowplow trackSelfDescribingEvent→RudderStack track; SessionConfiguration/TrackerConfiguration映射 | https://www.rudderstack.com/docs/user-guides/migration-guides/snowplow-migration-guide/ |
| Improvado: 7 Best Snowplow Competitors 2026 | RudderStack专注CDI: 事件收集+身份解析+数据仓库同步, 仓库优先架构 | https://improvado.io/blog/snowplow-competitors |
| RudderStack JS SDK GitHub | JavaScript SDK load API + track/page/identify API + ready callback + device-mode integrations | https://github.com/rudderlabs/rudder-sdk-js |

---

## 现状盘点

| 功能 | 实现程度 | 核心现有代码 | 差距 |
|------|----------|-------------|------|
| 内部事件日志 | **完整** | EventLogDO + EventLogMapper + POST /canvas/events/report | 仅内部记录canvas触发事件，非客户行为采集 |
| TrackEventHandler | **完整** | TrackEventHandler.java — 画布节点写event_log | 画布内事件追踪节点，非独立SDK |
| 事件定义 | **完整** | EventDefinitionDO + event_definition表 | 内部事件schema定义，非对外采集schema |
| 前端SDK | **不存在** | — | 无JavaScript/TypeScript浏览器SDK（@analytics/track-event） |
| 移动端SDK | **不存在** | — | 无iOS/Android/React Native/Flutter SDK |
| 服务端SDK | **不存在** | — | 无Java/Go/Python/Node.js服务端追踪SDK |
| Schema Registry | **不存在** | — | 无JSON Schema验证+Git-backed schema契约+CI/CD |
| 数据校验管线 | **不存在** | — | 无采集点schema校验+失败事件处理+数据质量监控 |
| 数据仓库同步 | **不存在** | — | 无BigQuery/Snowflake/Databricks实时加载器 |
| 身份解析 | **不存在** | — | 无匿名→已知用户拼接+跨设备身份图谱 |
| 自动采集 | **不存在** | — | 无页面浏览/点击/表单提交/滚动深度自动追踪 |

### 关键洞察

现有 event_log vs 事件追踪SDK的本质差异：
- **event_log**：Canvas内部的画布触发日志（记录"哪个事件触发了哪个画布"）
- **事件追踪SDK**：面向外部客户的行为数据采集基础设施（采集"用户在网站/App上做了什么"）

Snowplow的数据质量哲学启示：
- **"Schema at Collection"**：数据在采集点就做校验，脏数据不进数仓
- **原子表设计**：所有事件存一张表+dbt模型分层，避免RudderStack的"每事件类型一张表→归因需20+表JOIN"
- **400天Cookie**：通过first-party domain collection实现超长归因窗口（RudderStack受ITP限制仅7-14天）

RudderStack的生态哲学启示：
- **Segment兼容API**：降低迁移成本，Segment用户可以无缝切换
- **Warehouse-First**：数据直入客户自有数仓，不锁定数据
- **Reverse ETL**：数仓→运营工具反向同步，激活数仓数据

---

## 功能清单

### P0 — 多端事件追踪SDK

---

#### 1. Web JavaScript/TypeScript SDK [高复杂度 | 2.0人月]

**现状**：零前端追踪SDK

**需补齐**：

| 子功能 | 描述 | 参考出处 |
|--------|------|---------|
| SDK初始化 | load API + source write key + data plane URL配置 | RudderStack JS SDK: load API |
| 页面浏览追踪 | trackPageView: URL/标题/referrer自动采集 | Snowplow: page ping + beacon |
| 用户行为追踪 | track: 自定义事件+属性+上下文 | RudderStack: track API |
| 用户身份识别 | identify: userId + traits(匿名→已知拼接) | RudderStack: identify API |
| 自动采集 | 点击/表单提交/滚动深度/页面停留自动追踪 | Snowplow: 130+自动上下文 |
| Session管理 | SessionConfiguration: 会话超时/跨页面会话保持 | Snowplow: Session context |
| 队列与重试 | 离线队列+指数退避重试+事件排序 | Snowplow/RudderStack: outbox pattern |
| 隐私合规 | GDPR consent检查+doNotTrack+数据匿名化 | Snowplow: consent自动化 |
| TypeScript类型 | 完整TypeScript类型定义+事件类型自动推导 | Snowplow: TypeScript tracker |
| Cookie持久化 | First-party cookie 400天+ITP兼容策略 | Snowplow: 400天first-party cookie |

**SDK API设计**：

```typescript
// SDK初始化
const canvas = CanvasTracker.init({
  writeKey: "src_xxx",
  dataPlaneUrl: "https://events.yourdomain.com",
  autoTrack: {
    pageViews: true,
    clicks: true,
    formSubmissions: true,
    scrollDepth: true
  },
  session: {
    timeout: 30 * 60 * 1000 // 30分钟超时
  },
  privacy: {
    respectDoNotTrack: true,
    anonymizeIp: false
  }
});

// 页面浏览（自动或手动）
canvas.trackPageView({
  title: "产品详情页",
  url: "/products/123",
  referrer: "https://google.com"
});

// 用户识别
canvas.identify("user_12345", {
  email: "user@example.com",
  name: "张三",
  plan: "pro"
});

// 自定义事件追踪
canvas.track("order_completed", {
  orderId: "ORD-2026-001",
  revenue: 299.00,
  currency: "CNY",
  products: ["SKU-001", "SKU-002"]
});

// 事件上下文（自动附加）
// page: { url, title, referrer }
// session: { sessionId, sessionIndex, previousSessionId }
// device: { userAgent, screenResolution, viewport, language }
// campaign: { source, medium, campaign, term, content }
```

---

#### 2. 移动端SDK [高复杂度 | 2.0人月]

**现状**：零移动端SDK

**需补齐**：

| 子功能 | 描述 |
|--------|------|
| iOS SDK (Swift) | Swift Package Manager分发+UIKit/SwiftUI支持 |
| Android SDK (Kotlin) | Maven Central分发+Compose/XML支持 |
| React Native SDK | npm包+原生桥接 |
| Flutter SDK | pub.dev分发+Dart原生实现 |
| 自动采集 | 屏幕浏览/应用前后台/安装来源/推送打开 |
| 离线缓存 | SQLite队列+网络恢复自动重传 |
| 生命周期 | 应用生命周期事件(app_install/app_open/app_close) |
| 移动特有上下文 | 设备型号/OS版本/运营商/网络类型/电量/存储 |

---

#### 3. 服务端SDK [中复杂度 | 1.0人月]

**现状**：零服务端SDK

**需补齐**：

| 子功能 | 描述 |
|--------|------|
| Java SDK | Maven依赖+Spring Boot自动配置+Reactor/虚拟线程支持 |
| Go SDK | Go module+goroutine安全+context传播 |
| Python SDK | pip分发+asyncio支持+Django/Flask/FastAPI集成 |
| Node.js SDK | npm分发+Express/Koa/NestJS中间件 |
| 服务端事件 | 订单创建/支付完成/退款/用户注册/订阅变更 |
| 批量发送 | 批量聚合+异步非阻塞发送+背压控制 |

---

### P1 — Schema Registry与数据管线

---

#### 4. 事件Schema Registry [高复杂度 | 1.5人月]

**现状**：零schema registry

**需补齐**：

| 子功能 | 描述 | 参考出处 |
|--------|------|---------|
| JSON Schema定义 | 每个事件类型一个JSON Schema文件 | Snowplow Iglu: Schema Registry |
| Schema版本管理 | 语义化版本+Schema兼容性检查(向前/向后) | Snowplow: SchemaVer |
| Git-backed契约 | Schema文件存Git仓库+CI/CD自动校验 | Snowplow: Git-backed schema contracts |
| Schema自动注册 | SDK上报新事件→自动注册Schema(draft状态) | Snowplow: self-describing events |
| Schema市场 | 预定义行业标准Schema(电商/内容/社交) | Snowplow: Iglu Central |
| Schema Linting | Schema规范性检查+命名规范+字段类型约束 | Lingoport: i18n linting模式 |

**Schema Registry DDL**：

```sql
CREATE TABLE event_schema (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    vendor VARCHAR(50) NOT NULL COMMENT 'Schema厂商(com.yourcompany)',
    event_name VARCHAR(100) NOT NULL COMMENT '事件名(page_view/order_completed)',
    schema_version VARCHAR(20) NOT NULL COMMENT 'SchemaVer(1-0-0)',
    json_schema JSON NOT NULL COMMENT 'JSON Schema定义',
    description TEXT COMMENT '事件描述',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PUBLISHED/DEPRECATED',
    breaking_change TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否Breaking Change',
    examples JSON COMMENT '事件示例',
    created_by VARCHAR(64),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_event_version (vendor, event_name, schema_version),
    INDEX idx_status (status),
    INDEX idx_vendor_event (vendor, event_name)
) COMMENT '事件Schema注册表';

CREATE TABLE schema_evolution (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    schema_id BIGINT NOT NULL,
    from_version VARCHAR(20) NOT NULL,
    to_version VARCHAR(20) NOT NULL,
    change_type VARCHAR(20) NOT NULL COMMENT 'ADDITION/DEPRECATION/CHANGE/BREAKING',
    change_detail JSON NOT NULL COMMENT '变更详情(field_path/old_type/new_type)',
    compatible TINYINT(1) NOT NULL COMMENT '是否向后兼容',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_schema (schema_id)
) COMMENT 'Schema演化历史';
```

**JSON Schema示例**：

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "self": {
    "vendor": "com.canvas",
    "name": "order_completed",
    "format": "jsonschema",
    "version": "1-0-0"
  },
  "type": "object",
  "properties": {
    "orderId": {
      "type": "string",
      "description": "订单ID",
      "maxLength": 64
    },
    "revenue": {
      "type": "number",
      "description": "订单金额",
      "minimum": 0
    },
    "currency": {
      "type": "string",
      "description": "货币代码(ISO 4217)",
      "enum": ["CNY", "USD", "EUR", "JPY", "GBP"]
    },
    "products": {
      "type": "array",
      "items": { "type": "string" },
      "maxItems": 100
    },
    "couponCode": {
      "type": "string",
      "maxLength": 50
    }
  },
  "required": ["orderId", "revenue", "currency"],
  "additionalProperties": false
}
```

---

#### 5. 数据校验管线 [中复杂度 | 1.0人月]

**现状**：无采集端校验

**需补齐**：

| 子功能 | 描述 | 参考出处 |
|--------|------|---------|
| 采集点校验 | SDK端根据Schema校验→无效事件不发送 | Snowplow: validation at collection |
| 服务端校验 | Data Plane接收后再次校验→坏数据入死信队列 | Snowplow: failed events handling |
| 数据质量监控 | 错误率/缺失字段率/Schema违规率实时监控 | Snowplow: sub-0.1% error rate |
| 失败事件处理 | 死信队列→人工修复→重新注入 | Snowplow: failed event recovery |
| Schema漂移检测 | 生产数据vs注册Schema→自动检测漂移告警 | 数据治理 |
| 数据采样 | 高流量事件采样降低存储成本 | Snowplow: event sampling |

**数据校验管线DDL**：

```sql
CREATE TABLE event_validation_result (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_id VARCHAR(64) NOT NULL COMMENT '事件唯一ID(event_uuid)',
    schema_id BIGINT COMMENT '匹配的Schema ID',
    validation_status VARCHAR(20) NOT NULL COMMENT 'PASSED/FAILED/SCHEMA_NOT_FOUND',
    errors JSON COMMENT '校验错误详情',
    raw_event JSON COMMENT '原始事件(校验失败时保留)',
    source_ip VARCHAR(45),
    sdk_version VARCHAR(20),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_status (validation_status),
    INDEX idx_schema (schema_id),
    INDEX idx_created (created_at)
) COMMENT '事件校验结果';

CREATE TABLE event_dead_letter (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_id VARCHAR(64) NOT NULL,
    raw_event JSON NOT NULL,
    failure_reason TEXT NOT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    max_retries INT NOT NULL DEFAULT 3,
    next_retry_at DATETIME,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/RETRYING/REPAIRED/DISCARDED',
    repaired_by VARCHAR(64),
    repaired_at DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_status (status),
    INDEX idx_next_retry (next_retry_at)
) COMMENT '事件死信队列';
```

---

### P2 — 数据仓库同步与高级能力

---

#### 6. 实时数据流与仓库同步 [高复杂度 | 1.5人月]

**现状**：零数据仓库同步

**需补齐**：

| 子功能 | 描述 | 参考出处 |
|--------|------|---------|
| 实时流加载器 | Kafka/Kinesis→BigQuery/Snowflake/Databricks实时加载 | Snowplow: real-time streaming loaders |
| 原子表设计 | 所有事件存一张宽表+dbt模型分层 | Snowplow: atomic table + dbt models |
| dbt模型包 | 预定义dbt模型: page_views/sessions/users/conversions | Snowplow: pre-built dbt models |
| 增量加载 | 增量同步+幂等写入+dbt incremental materialization | Snowplow: incremental loading |
| 数据回填 | 历史数据回填+Schema演化数据再处理 | 数据治理 |
| 数据保留策略 | 热数据(实时)/温数据(30天)/冷数据(归档)分层 | 成本优化 |

**原子表DDL（Data Warehouse侧）**：

```sql
-- BigQuery / Snowflake 标准事件原子表
CREATE TABLE atomic_events (
    -- 事件标识
    event_id STRING NOT NULL,
    event_name STRING NOT NULL,
    event_vendor STRING NOT NULL,
    event_version STRING NOT NULL,
    
    -- 时间
    collector_tstamp TIMESTAMP NOT NULL,  -- 采集时间
    derived_tstamp TIMESTAMP NOT NULL,     -- 客户端时间(去Clock Skew)
    dvce_created_tstamp TIMESTAMP,         -- 设备创建时间
    
    -- 用户标识
    user_id STRING,
    domain_userid STRING,                  -- Cookie-based匿名ID
    network_userid STRING,                 -- 网络层匿名ID
    domain_sessionid STRING,
    domain_sessionidx INT,
    
    -- 页面上下文
    page_url STRING,
    page_title STRING,
    page_referrer STRING,
    
    -- 设备上下文
    useragent STRING,
    dvce_type STRING,
    dvce_ismobile BOOL,
    os_name STRING,
    os_version STRING,
    br_name STRING,
    br_version STRING,
    
    -- 营销上下文
    mkt_source STRING,
    mkt_medium STRING,
    mkt_campaign STRING,
    mkt_term STRING,
    mkt_content STRING,
    
    -- 地理位置
    geo_country STRING,
    geo_city STRING,
    geo_region STRING,
    
    -- 自定义事件属性 (JSON)
    unstruct_event JSON,
    contexts JSON,
    
    -- 分区
    load_tstamp TIMESTAMP NOT NULL,
    tenant_id STRING NOT NULL
)
PARTITION BY DATE(derived_tstamp)
CLUSTER BY event_name, domain_userid;
```

---

#### 7. 身份解析引擎 [中复杂度 | 1.0人月]

**现状**：无身份拼接

**需补齐**：

| 子功能 | 描述 | 参考出处 |
|--------|------|---------|
| 匿名→已知拼接 | Cookie ID(user_fingerprint)→登录ID(user_id)拼接 | RudderStack: identity resolution |
| 跨设备图谱 | 同一用户多设备行为拼接 | Snowplow: multi-entity Profiles Store |
| 身份优先级 | 登录ID > 邮箱 > 手机号 > 设备ID > Cookie ID | RudderStack: identity stitching |
| 拼接时间窗口 | 30天内匿名行为可回溯拼接 | RudderStack: retroactive stitching |
| 身份冲突解决 | 多用户同设备/同用户多账号→冲突检测+解决 | Snowplow: identity resolution edge cases |

**身份解析DDL**：

```sql
CREATE TABLE identity_map (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    canonical_user_id VARCHAR(64) NOT NULL COMMENT '标准用户ID',
    identity_type VARCHAR(20) NOT NULL COMMENT 'LOGIN_ID/EMAIL/PHONE/DEVICE_ID/COOKIE_ID/EXTERNAL_ID',
    identity_value VARCHAR(200) NOT NULL COMMENT '身份值',
    confidence DECIMAL(3,2) NOT NULL DEFAULT 1.00 COMMENT '置信度0-1',
    first_seen_at DATETIME NOT NULL COMMENT '首次关联时间',
    last_seen_at DATETIME NOT NULL COMMENT '最近关联时间',
    source VARCHAR(50) COMMENT '身份来源(sdk/import/api/manual)',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_identity (identity_type, identity_value),
    INDEX idx_canonical (canonical_user_id),
    INDEX idx_last_seen (last_seen_at)
) COMMENT '身份映射表';

CREATE TABLE identity_merge_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    from_user_id VARCHAR(64) NOT NULL COMMENT '被合并的用户ID',
    to_user_id VARCHAR(64) NOT NULL COMMENT '合并后的标准用户ID',
    merge_reason VARCHAR(50) NOT NULL COMMENT 'LOGIN_MATCH/EMAIL_MATCH/PHONE_MATCH/DEVICE_MATCH/MANUAL',
    affected_events INT NOT NULL DEFAULT 0 COMMENT '受影响的事件数',
    merged_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_from_user (from_user_id),
    INDEX idx_to_user (to_user_id)
) COMMENT '身份合并日志';
```

---

#### 8. 集成目标市场 [低复杂度 | 0.5人月]

**现状**：无下游集成

**需补齐**：

| 子功能 | 描述 |
|--------|------|
| 分析工具集成 | Google Analytics/Amplitude/Mixpanel/神策 自动同步 |
| 营销工具集成 | Braze/Iterable/个推/极光 事件推送 |
| 数据仓库集成 | BigQuery/Snowflake/Databricks/ClickHouse 加载器 |
| 消息队列输出 | Kafka/Kinesis/Pulsar 实时事件流输出 |
| Webhook目标 | 自定义Webhook推送事件 |
| 目标市场 | 目标连接器配置+启用/禁用+健康检查 |

---

## 工作量估算

| 优先级 | 功能 | 后端人月 | 前端人月 | 测试人月 | 总计 |
|--------|------|---------|---------|---------|------|
| P0 | Web JavaScript SDK | 0.5 | 1.5 | 0.2 | 2.2 |
| P0 | 移动端SDK | 1.2 | 0.8 | 0.2 | 2.2 |
| P0 | 服务端SDK | 0.7 | 0.3 | 0.1 | 1.1 |
| P1 | Schema Registry | 1.0 | 0.5 | 0.2 | 1.7 |
| P1 | 数据校验管线 | 0.7 | 0.3 | 0.1 | 1.1 |
| P2 | 数据仓库同步 | 1.0 | 0.5 | 0.2 | 1.7 |
| P2 | 身份解析引擎 | 0.7 | 0.3 | 0.1 | 1.1 |
| P2 | 集成目标市场 | 0.3 | 0.2 | 0.1 | 0.6 |
| | **合计** | **6.1** | **4.4** | **1.2** | **11.7** |

---

## 执行顺序

```
Sprint 1 (P0-Web): Web JavaScript SDK — 2.2人月
  → 产出：浏览器SDK+自动采集+Session管理+TypeScript类型+隐私合规

Sprint 2 (P0-Mobile): 移动端SDK — 2.2人月
  → 产出：iOS/Android/RN/Flutter SDK+自动采集+离线缓存

Sprint 3 (P0-Server): 服务端SDK — 1.1人月
  → 产出：Java/Go/Python/Node.js SDK+批量发送+背压控制

Sprint 4 (P1-Schema+Validation): Schema Registry+数据校验 — 2.8人月
  → 产出：Schema Registry+Git-backed契约+收集点校验+数据质量监控

Sprint 5 (P2-高级): 数据仓库同步+身份解析+集成 — 3.4人月
  → 产出：实时流加载器+原子表+dbt模型+身份图谱+50+集成目标
```

---

## 风险评估

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| SDK碎片化 | 多端SDK版本不一致→数据格式不统一 | 统一Schema Registry+SDK CI/CD+一致性测试 |
| Cookie/Tracking限制 | ITP/ETP限制Cookie生命周期→数据缺失 | First-party domain collection+Server-side tracking+IDMatch |
| 数据量爆炸 | 行为数据量远超业务数据→存储成本高 | 采样策略+分层存储+TTL自动清理 |
| Schema演化 | Breaking change→历史数据不兼容 | 向后兼容优先+SchemaVer+数据再处理管线 |
| SDK性能 | SDK影响页面加载速度→用户体验差 | 异步加载+最小化打包+CSP兼容 |
| 隐私合规 | 跨地区隐私法规差异→合规风险 | Consent管理+数据本地化+Region-aware配置 |

---

## 与其他方向的关系

| 方向 | 与㉜的关系 |
|------|----------|
| ⑨ 营销数据中台 | 事件追踪SDK是数据中台的首要数据源（行为数据是归因/分析的基础） |
| ⑬ 实时用户画像引擎 | 行为数据+身份解析→画像实时更新（最近行为/活跃度/偏好） |
| ④ AI原生平台 | 高质量行为数据是AI模型训练的核心输入（预测/NBA/推荐） |
| ⑪ 开放平台/Webhook | SDK是开放平台的核心组件（客户通过SDK接入自己的数据） |
| ⑫ 多租户SaaS化 | 租户级事件隔离+Schema registry+数据分区 |
| ⑭ A/B测试平台 | 事件数据是A/B测试的指标数据源（转化率/留存率/收入） |
| ㉖ 功能开关 | Feature Flag SDK可与事件追踪SDK统一部署 |
| ㉗ 偏好与同意管理 | 追踪SDK需根据Consent状态决定是否采集数据 |
