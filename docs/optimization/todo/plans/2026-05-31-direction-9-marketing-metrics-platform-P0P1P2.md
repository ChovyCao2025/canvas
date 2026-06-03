# 方向⑨：营销数据中台 — 功能清单

> 定位：不是重建CDP（方向⑥），而是构建"营销效果闭环"——把画布执行产生的触达→打开→点击→转化数据，形成指标体系+归因分析+ROI计算
> 策略评估：画布执行数据天然绑定本系统，神策/Braze做不了"画布效果闭环"；8-12人月可完成核心功能
> 竞品对标：Braze(原生Canvas效果分析)、Iterable(原生Journey分析)、神策(独立分析，无画布关联)
> 建议：**P1建议做**，是"效果闭环"的核心

---

## 现状盘点

| 功能 | 实现程度 | 核心现有代码 | 差距 |
|------|----------|-------------|------|
| 执行统计 | **完整** | CanvasExecutionStatsDO(日聚合)+CanvasStatsController(stats/funnel/trend) | 有基础统计，缺渠道级+转化级指标 |
| 节点漏斗 | **完整** | CanvasExecutionTraceMapper.selectFunnelByCanvasId() | 有节点级漏斗，缺跨画布漏斗 |
| 触达记录 | **部分** | AbstractSendMessageHandler(发送+渠道) | 仅记录发送，无打开/点击/转化回执 |
| 渠道回执 | **不存在** | — | 完全缺失 |
| 归因分析 | **不存在** | — | 完全缺失 |
| ROI计算 | **不存在** | — | 完全缺失 |
| 效果看板 | **部分** | CanvasStatsController(stats/funnel/trend) | 基础看板有，缺渠道对比/AB效果/趋势分析 |

---

## 功能清单

### P0 — 渠道回执与触达闭环

---

#### 1. 渠道回执采集 [高复杂度 | 4.0人月]

**现状**：消息发送后无任何回执，不知道是否送达/打开/点击

**需补齐**：

| 渠道 | 回执类型 | 接入方式 |
|------|---------|---------|
| EMAIL | 送达/退回/打开/点击 | Webhook(SES/SendGrid/阿里邮件) |
| SMS | 送达/失败 | 回调(阿里短信/腾讯短信) |
| PUSH | 送达/点击 | FCM/APNs回调 |
| WECHAT | 送达/点击/关注/取关 | 企微回调 |
| IN_APP | 展示/点击 | SDK上报 |

**回执处理架构**：

```
外部渠道回调 → ReceiptWebhookController → 验签 → 异步入库
                                                     ↓
                                              ReceiptProcessor
                                                     ↓
                                    更新 MessageDelivery 记录状态
                                                     ↓
                                    触发事件（message.opened/clicked）
                                                     ↓
                                    更新实时指标（Redis INCR）
                                                     ↓
                                    更新日聚合统计
```

**数据库DDL**：

```sql
CREATE TABLE message_delivery (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    delivery_id VARCHAR(64) NOT NULL COMMENT '投递ID(唯一)',
    execution_id VARCHAR(64) NOT NULL COMMENT '执行ID',
    canvas_id BIGINT NOT NULL COMMENT '画布ID',
    node_id VARCHAR(64) NOT NULL COMMENT '节点ID',
    user_id VARCHAR(64) NOT NULL COMMENT '用户ID',
    channel VARCHAR(20) NOT NULL COMMENT '渠道 EMAIL/SMS/PUSH/WECHAT/IN_APP',
    status VARCHAR(20) NOT NULL DEFAULT 'SENT' COMMENT 'SENT/DELIVERED/BOUNCED/OPENED/CLICKED/COMPLAINED/UNSUBSCRIBED',
    external_id VARCHAR(128) COMMENT '渠道侧消息ID(用于回执匹配)',
    sent_at DATETIME NOT NULL COMMENT '发送时间',
    delivered_at DATETIME COMMENT '送达时间',
    opened_at DATETIME COMMENT '首次打开时间',
    clicked_at DATETIME COMMENT '首次点击时间',
    click_url VARCHAR(500) COMMENT '点击URL',
    bounce_reason VARCHAR(500) COMMENT '退回原因',
    tenant_id BIGINT NOT NULL DEFAULT 0,
    INDEX idx_execution (execution_id),
    INDEX idx_canvas_user (canvas_id, user_id),
    INDEX idx_status (status),
    INDEX idx_external (external_id),
    INDEX idx_sent_time (sent_at),
    INDEX idx_tenant (tenant_id)
) COMMENT '消息投递记录';

CREATE TABLE delivery_receipt_raw (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    channel VARCHAR(20) NOT NULL,
    provider VARCHAR(30) NOT NULL COMMENT '服务商 SENDGRID/ALI_SMS/TENCENT_SMS',
    raw_payload JSON NOT NULL COMMENT '原始回调Payload',
    processed TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_processed (processed),
    INDEX idx_created (created_at)
) COMMENT '回执原始记录(容错+排查)';
```

---

#### 2. 营销指标体系 [中复杂度 | 2.0人月]

**现状**：CanvasExecutionStatsDO仅有触发/成功/失败/挂起/超频/用户数/耗时

**需补齐**：

| 指标层级 | 指标 | 计算 |
|---------|------|------|
| 触达层 | 发送数 | COUNT(status >= SENT) |
| 触达层 | 送达数 | COUNT(status >= DELIVERED) |
| 触达层 | 送达率 | 送达数 / 发送数 |
| 触达层 | 退回数 | COUNT(status = BOUNCED) |
| 触达层 | 退回率 | 退回数 / 发送数 |
| 互动层 | 打开数 | COUNT(status >= OPENED) |
| 互动层 | 打开率 | 打开数 / 送达数 |
| 互动层 | 点击数 | COUNT(status >= CLICKED) |
| 互动层 | 点击率 | 点击数 / 送达数 |
| 互动层 | 点击打开比 | 点击数 / 打开数 |
| 转化层 | 转化数 | 外部回传(需事件绑定) |
| 转化层 | 转化率 | 转化数 / 送达数 |
| 价值层 | ROI | 转化收入 / 触达成本 |
| 价值层 | 单用户收入 | 转化收入 / 转化用户数 |
| 退订层 | 退订数 | COUNT(status = UNSUBSCRIBED) |
| 退订层 | 退订率 | 退订数 / 送达数 |

**数据库DDL**：

```sql
CREATE TABLE canvas_metrics_daily (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    canvas_id BIGINT NOT NULL,
    channel VARCHAR(20) NOT NULL COMMENT '渠道(ALL=汇总)',
    stat_date DATE NOT NULL,
    sent_count INT NOT NULL DEFAULT 0,
    delivered_count INT NOT NULL DEFAULT 0,
    bounced_count INT NOT NULL DEFAULT 0,
    opened_count INT NOT NULL DEFAULT 0,
    clicked_count INT NOT NULL DEFAULT 0,
    converted_count INT NOT NULL DEFAULT 0,
    unsubscribed_count INT NOT NULL DEFAULT 0,
    complained_count INT NOT NULL DEFAULT 0,
    revenue DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '转化收入',
    cost DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '触达成本',
    unique_opens INT NOT NULL DEFAULT 0 COMMENT '去重打开数',
    unique_clicks INT NOT NULL DEFAULT 0 COMMENT '去重点击数',
    tenant_id BIGINT NOT NULL DEFAULT 0,
    UNIQUE INDEX uk_canvas_channel_date (canvas_id, channel, stat_date),
    INDEX idx_date (stat_date),
    INDEX idx_tenant (tenant_id)
) COMMENT '画布日指标聚合';
```

---

### P1 — 归因与ROI

---

#### 3. 归因引擎 [高复杂度 | 4.0人月]

**现状**：不存在任何归因能力

**需补齐**：

| 归因模型 | 描述 | 适用场景 |
|---------|------|---------|
| 首次触达 | 用户第一次收到消息归因 | 品牌认知阶段 |
| 末次触达 | 用户最后一次收到消息归因 | 促转化阶段 |
| 线性归因 | 所有触达均分贡献 | 整体评估 |
| 时间衰减 | 越接近转化的触达权重越高 | 短决策周期 |
| U型归因 | 首次+末次各40%，中间20% | 平衡认知+转化 |

**归因计算流程**：

```
1. 用户完成转化事件（如"购买"）
2. 查找该用户近期(7/14/30天)收到的所有营销消息
3. 按选定归因模型分配贡献
4. 将归因结果写入 attribution_record
5. 更新画布/渠道的归因收入指标
```

**数据库DDL**：

```sql
CREATE TABLE attribution_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(64) NOT NULL,
    conversion_event VARCHAR(100) NOT NULL COMMENT '转化事件',
    conversion_value DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '转化金额',
    conversion_time DATETIME NOT NULL,
    attributed_canvas_id BIGINT COMMENT '归因画布',
    attributed_channel VARCHAR(20) COMMENT '归因渠道',
    attributed_node_id VARCHAR(64) COMMENT '归因节点',
    attribution_model VARCHAR(20) NOT NULL COMMENT 'FIRST/LAST/LINEAR/DECAY/U_SHAPE',
    attribution_weight DECIMAL(5,4) NOT NULL DEFAULT 1.0 COMMENT '归因权重(0-1)',
    attributed_value DECIMAL(12,2) GENERATED ALWAYS AS (conversion_value * attribution_weight) STORED,
    touchpoints JSON COMMENT '触达路径 [{canvasId,nodeId,channel,time}]',
    lookback_days INT NOT NULL DEFAULT 7 COMMENT '回溯窗口(天)',
    tenant_id BIGINT NOT NULL DEFAULT 0,
    INDEX idx_user (user_id),
    INDEX idx_canvas (attributed_canvas_id),
    INDEX idx_channel (attributed_channel),
    INDEX idx_conversion_time (conversion_time),
    INDEX idx_tenant (tenant_id)
) COMMENT '归因记录';
```

---

#### 4. ROI仪表盘 [中复杂度 | 2.0人月]

**现状**：CanvasStatsController仅有基础统计

**需补齐**：

| 看板 | 描述 | 前端组件 |
|------|------|---------|
| 画布效果总览 | 发送→送达→打开→点击→转化漏斗 | 漏斗图+指标卡片 |
| 渠道对比 | 各渠道送达率/打开率/点击率/退订率 | 分组柱状图 |
| 画布ROI | 触达成本 vs 转化收入 | 散点图+ROI排名 |
| 归因对比 | 不同归因模型下的收入分配 | 堆叠柱状图 |
| AB效果 | 实验组vs对照组的转化差异 | 对比柱状图+显著性检验 |
| 趋势分析 | 关键指标30天趋势 | 折线图 |

---

### P2 — 高级分析

---

#### 5. 转化事件绑定 [低复杂度 | 1.0人月]

**描述**：运营可配置"哪些事件算转化+转化金额字段"

```json
{
  "conversionEvents": [
    {"event": "purchase", "valueField": "orderAmount", "name": "购买"},
    {"event": "subscribe", "valueField": "planPrice", "name": "订阅"},
    {"event": "register", "value": 10, "name": "注册(固定值10元)"}
  ]
}
```

---

#### 6. 同期群分析 [中复杂度 | 1.5人月]

**描述**：按画布触达日期分群，观察N日后的转化率

| 同期群 | 触达人数 | 1日转化 | 3日转化 | 7日转化 | 14日转化 |
|--------|---------|--------|--------|--------|---------|
| 6/1触达 | 5000 | 2.1% | 3.5% | 4.2% | 4.8% |
| 6/2触达 | 4500 | 2.3% | 3.8% | 4.5% | — |

---

#### 7. 实时效果监控 [中复杂度 | 1.5人月]

**描述**：画布发布后实时展示效果指标（发送→打开→点击→转化）

| 子功能 | 描述 |
|--------|------|
| 实时计数 | Redis实时聚合，每秒刷新 |
| 效果预警 | 打开率低于历史均值2个标准差→告警 |
| 自动暂停 | 退订率超过阈值→自动暂停画布 |

---

## 工作量估算

| 优先级 | 功能 | 后端人月 | 前端人月 | 测试人月 | 总计 |
|--------|------|---------|---------|---------|------|
| P0 | 渠道回执采集 | 3.0 | 0.5 | 0.5 | 4.0 |
| P0 | 营销指标体系 | 1.5 | 0.5 | 0.3 | 2.3 |
| P1 | 归因引擎 | 3.0 | 1.0 | 0.5 | 4.5 |
| P1 | ROI仪表盘 | 1.0 | 1.0 | 0.2 | 2.2 |
| P2 | 转化事件绑定 | 0.7 | 0.3 | 0.1 | 1.1 |
| P2 | 同期群分析 | 1.0 | 0.5 | 0.2 | 1.7 |
| P2 | 实时效果监控 | 1.0 | 0.5 | 0.2 | 1.7 |
| | **合计** | **11.2** | **4.3** | **2.0** | **17.5** |

---

## 执行顺序

```
Sprint 1 (P0-回执): 渠道回执采集 — 4.0人月
  → 产出：消息投递记录+回执Webhook

Sprint 2 (P0-指标): 营销指标体系 — 2.3人月
  → 产出：日指标聚合+核心看板

Sprint 3 (P1-归因): 归因引擎 — 4.5人月
  → 产出：5种归因模型+归因看板

Sprint 4 (P1-ROI): ROI仪表盘 — 2.2人月
  → 产出：画布/渠道ROI对比

Sprint 5 (P2-高级): 转化+同期群+实时 — 4.5人月
  → 产出：高级分析能力
```

---

## 风险评估

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| 渠道回执接入复杂 | 每个渠道API不同 | 先做EMAIL+SMS(覆盖80%场景) |
| 归因窗口争议 | 不同业务归因窗口不同 | 支持7/14/30天可配置 |
| 数据量增长 | message_delivery表数据量大 | 按月分表+历史归档 |
| 转化事件需外部回传 | 依赖客户端/服务端上报 | 复用方向⑥SDK |

---

## 与其他方向的关系

| 方向 | 与⑨的关系 |
|------|----------|
| ① 营销深度 | 归因引擎方向①已规划，此处是详细落地 |
| ④ AI-Native | AI模型依赖回执数据训练 |
| ⑥ CDP+MA | 回执数据是行为事件的重要来源 |
| ⑧ 营销审批 | 审批预览需要预算估算（复用ROI数据） |
| ⑪ 开放平台 | 渠道回执通过Webhook推送 |