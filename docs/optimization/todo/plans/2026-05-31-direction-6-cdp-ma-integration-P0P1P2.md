# 方向⑥：CDP+MA一体化 — 功能清单

> 定位：从"用数据做营销"升级为"以数据为核心，营销只是数据的消费场景之一"
> 策略评估：重资产（OLAP+SDK+实时计算）；神策4年先发优势；CDP市场正被"可组合CDP"颠覆
> 竞品对标：神策（估值50亿+RMB，CDP+分析全栈）、GrowingIO（分析+MA）、mParticle（可组合CDP）
> 建议：不推荐独立做，应逐步增强现有CDP能力，而非重建CDP

---

## 王状盘点

| 功能 | 实现程度 | 核心现有代码 | 对标神策 |
|------|----------|-------------|---------|
| 数据采集SDK | **部分** | EventReportAuthService+TrackEventHandler+EventLogDO（服务端上报） | 10% |
| 实时计算管道 | **不存在** | 仅MQ消费者做触发 | 0% |
| 身份图/身份识别 | **部分** | CdpUserIdentityDO+CdpUserService.ensureUserByIdentity()（KV映射） | 20% |
| 行为分析 | **不存在** | — | 0% |
| 数据目录 | **部分** | DataSourceConfigController（JDBC数据源+表元数据） | 5% |
| 数据质量 | **不存在** | — | 0% |

---

## 功能清单

### P0 — CDP基础设施

---

#### 1. 数据采集SDK [高复杂度 | 6.0人月]

**现状**：仅有服务端事件上报接口（HMAC-SHA256签名+事件落库），无客户端SDK

**需补齐**：

| SDK | 平台 | 语言 | 核心功能 |
|-----|------|------|---------|
| JS SDK | Web浏览器 | TypeScript | 自动埋点(页面/点击/表单)+自定义事件+用户识别 |
| iOS SDK | iPhone | Swift/Kotlin | 自动埋点(页面/点击)+自定义事件+设备信息 |
| Android SDK | Android手机 | Kotlin | 同iOS |
| 小程序SDK | 微信/支付宝小程序 | TypeScript | 自动埋点(页面/分享/支付)+自定义事件（见方向②） |

**JS SDK核心API**：
```javascript
import { CanvasTracker } from '@canvas/tracker';

const tracker = new CanvasTracker({
  appId: 'app_xxx',
  serverUrl: 'https://track.canvas.com/api/v1/track',
  autoTrack: { pageView: true, click: true, formSubmit: true },
  userId: 'u_12345'
});

tracker.track('purchase', { amount: 99.9, productId: 'P001' });
tracker.identify('u_12345', { name: '张三', vipLevel: 'GOLD' });
tracker.pageView('/product/P001');
```

**服务端接收**：
```
POST /api/v1/track
Headers: X-App-Id, X-SDK-Version, X-Signature(HMAC-SHA256)

批量接收：
{
  "batch": [
    {"type": "track", "event": "purchase", "properties": {...}, "timestamp": ...},
    {"type": "identify", "userId": "u_12345", "traits": {...}},
    {"type": "pageView", "path": "/product/P001", "referrer": "..."}
  ]
}
```

---

#### 2. 实时计算管道 [极高复杂度 | 10.0人月]

**现状**：不存在任何流式计算

**需从零构建**：

| 子功能 | 描述 | 技术选型 |
|--------|------|---------|
| 实时聚合 | 按用户/时间窗口的实时指标计算 | Flink/Kafka Streams |
| 行为序列 | 用户行为序列实时更新 | Redis Sorted Set |
| 规则触发 | 实时行为满足规则时触发画布 | 已有EVENT_TRIGGER |
| 实时标签 | 基于实时行为自动打标签 | 事件驱动+标签更新 |

**Flink管道示例**：
```
Source: Kafka(events_topic)
  → KeyBy(userId)
  → Window(1h sliding, 5min slide)
  → Aggregate: 计算1小时内行为频次
  → Sink: Redis(user_behavior_stats)
  → Sink: 触发规则引擎 → EVENT_TRIGGER
```

**成本估算**：
- Flink集群：3节点 × 8核16G ≈ ¥5000/月
- Kafka集群：3节点 × 4核8G ≈ ¥3000/月
-运维人力：0.5人专职

---

#### 3. 身份图 [高复杂度 | 4.0人月]

**现状**：CdpUserService.ensureUserByIdentity()仅做KV映射（type:value→userId）

**需补齐**：

| 子功能 | 描述 |
|--------|------|
| 多跳身份关联 | 通过中间标识关联两个看似无关的身份 |
| 确定性匹配 | 同一手机号/邮箱→同一用户（100%确定性） |
| 概率性匹配 | 设备指纹/行为模式相似度→同一用户（<100%） |
| 身份合并 | 多个身份确认属于同一用户后，合并画像数据 |
| 身份分裂 | 误合并的身份拆分回独立用户 |

**确定性匹配规则**：
```
高确定性（>99%）：
  手机号 = 手机号 → 同一用户
  邮箱 = 铼箱 → 同一用户
  union_id = union_id → 同一用户

中确定性（>90%）：
  同一设备连续登录不同账号 → 可能同一用户
  同一IP+同一浏览器指纹 → 可能同一用户
```

**技术方案**：
- 存储：MySQL关系表 + Redis缓存（图数据库暂不考虑）
- 算法：基于优先级的合并规则

---

### P1 — 分析能力

---

#### 4. 行为分析套件 [高复杂度 | 6.0人月]

**现状**：不存在任何分析功能

**需从零构建**：

| 分析类型 | 描述 | 后端 | 前端 |
|---------|------|------|------|
| 事件分析 | 按时间/维度的事件频次统计 | EventAnalyticsService | 事件分析页 |
| 漏斗分析 | 多步骤转化率 | FunnelService（Bitmap交集计算） | 漏斗图 |
| 留存分析 | 用户N日/周/月留存率 | RetentionService | 畜存曲线图 |
| 分布分析 | 事件属性值分布 | DistributionService | 直方图/饼图 |
| 路径分析 | 用户行为路径 | PathService（序列模式挖掘） | 路径图(Sankey) |
| 用户洞察 | 单用户行为时间线 | UserInsightService | 用户洞察页 |

**漏斗分析算法**：
```
步骤1: 览商品人群 Bitmap → step1_users
步骤2: 加购人群 Bitmap → step2_users
步骤3: 支付人群 Bitmap → step3_users

转化率：
  step1→step2: |step1 AND step2| / |step1|
  step2→step3: |step2 AND step3| / |step2|
  整体: |step1 AND step2 AND step3| / |step1|
```

---

#### 5. 数据目录 [中复杂度 | 2.0人月]

**现状**：DataSourceConfigController可读取JDBC数据源的表元数据

**需补齐**：

| 子功能 | 描述 |
|--------|------|
| 数据资产目录 | 统一管理所有数据源（MySQL/Redis/ES/对象存储） |
| 字段级血缘 | 画布节点→输出字段→下游节点的数据流转路径 |
| 标签分类体系 | 数据字段打业务标签（PII/金融/地址/行为） |
| 数据访问控制 | 控制谁可以访问哪些数据源 |

---

#### 6. 数据质量校验 [中复杂度 | 2.0人月]

**现状**：不存在

**需补齐**：

| 规则类型 | 描述 |
|---------|------|
| 完整性 | 必填字段空值率 |
| 一致性 | 跨表字段值一致性 |
| 时效性 | 数据更新是否及时 |
| 准确性 | 字段值是否符合业务规则 |
| 唯一性 | 主键/唯一键重复率 |

---

### P2 — 数据仓库直连

---

#### 7. 数据仓库直连 [中复杂度 | 3.0人月]

**描述**：支持从Snowflake/BigQuery/Doris直连查询数据

| 数据仓库 | 接入方式 | 适用场景 |
|---------|---------|---------|
| ClickHouse/Doris | JDBC直连 | 中国企业首选 |
| Snowflake | JDBC+OAuth | 海外企业 |
| BigQuery | JDBC+Service Account | Google生态 |
| Hive/Spark | Thrift/HTTP | 大数据平台 |

---

## 工作量估算

| 优先级 | 功能 | 后端人月 | 前端人月 | 数据人月 | 总计 |
|--------|------|---------|---------|---------|------|
| P0 | 数据采集SDK(JS+小程序) | 3.0 | 1.0 | — | 4.0 |
| P0 | 数据采集SDK(iOS+Android) | 2.0 | 0.5 | — | 2.5 |
| P0 | 实时计算管道 | 6.0 | 0.5 | 3.5 | 10.0 |
| P0 | 身份图 | 3.0 | 1.0 | — | 4.0 |
| P1 | 行为分析套件 | 4.0 | 2.0 | — | 6.0 |
| P1 | 数据目录 | 1.5 | 0.5 | — | 2.0 |
| P1 | 数据质量 | 1.5 | 0.5 | — | 2.0 |
| P2 | 数据仓库直连 | 2.5 | 0.5 | — | 3.0 |
| | **合计** | **19.5** | **5.5** | **3.5** | **29.5** |

---

## 不推荐独立做的原因

| 风险 | 说明 |
|------|------|
| 重资产 | OLAP+SDK+实时计算+数据工程团队，年度成本>50万 |
| 神策先发优势 | 神策4年+1000+客户+完整CDP+分析+MA链路 |
| 可组合CDP颠覆 | mParticle/Snowplow提供轻量CDP，客户不再买重CDP |
| ROI低 | 29.5人月投入后仍落后神策3-4年 |

**建议**：逐步增强现有CDP能力（身份图+行为分析），而非重建CDP。行为分析可与方向①归因引擎协同建设。