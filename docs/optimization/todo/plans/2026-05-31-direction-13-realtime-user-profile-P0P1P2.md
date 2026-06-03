# 方向⑬：实时用户画像引擎 — 功能清单

> 定位：从"静态标签存储"升级为"实时行为聚合+动态标签计算"——让用户画像随行为实时更新，而非仅靠手动/导入写入
> 策略评估：CdpUserProfileDO+TagDefinitionDO+EventLogDO已有基础骨架，缺实时聚合+计算标签+行为序列+分群联动；8-12人月可完成核心
> 竞品对标：神策用户画像(实时计算标签)、Braze User Profile(实时行为聚合)、Segment Personas(身份图+计算画像)
> 建议：**P1建议做**，与⑨数据中台协同，画像数据是归因+旅程的基础

---

## 现状盘点

| 功能 | 实现程度 | 核心现有代码 | 差距 |
|------|----------|-------------|------|
| 用户基础画像 | **完整** | CdpUserProfileDO(userId/displayName/phone/email/status/propertiesJson/firstSeenAt/lastSeenAt) | 基础画像完整 |
| 标签定义 | **完整** | TagDefinitionDO+TagValueDefinitionDO+TagDefinitionController(CRUD+分页) | 标签元数据管理完整 |
| 标签读写 | **完整** | CdpUserTagDO+TagService.setTag/removeTag/listCurrentTags | 手动标签读写完整 |
| 标签历史 | **完整** | CdpUserTagHistoryDO(oldValue/newValue/operation/sourceType/idempotencyKey) | 标签变更历史完整 |
| 用户洞察 | **部分** | CdpUserInsightService(画像+标签+画布参与汇总) | 仅聚合画布执行记录，无行为事件聚合 |
| 事件日志 | **部分** | EventLogDO(eventCode/userId/attributes/canvasTriggered) | 仅记录触发事件，无行为事件序列 |
| 实时聚合 | **不存在** | — | 无实时行为计数/最近行为/行为频次 |
| 计算标签 | **不存在** | — | 无基于规则的自动标签计算 |
| 行为序列 | **不存在** | — | 无用户行为时间线 |
| 身份图 | **不存在** | — | 无跨渠道用户ID合并 |
| 分群联动 | **部分** | CdpAudienceSourceService(人群→画布) | 人群→画布有，画像→人群缺 |

### 关键洞察

当前系统已有完整的**标签存储体系**（定义+值+历史），但标签值只能通过3种方式写入：
1. **手动**：CdpUserController.addTag()
2. **画布节点**：TagNodeHandler
3. **导入**：批量导入

缺的是**第4种方式**：基于用户行为自动计算标签。例如：
- 用户7天内打开邮件3次 → 自动打标签"邮件活跃"
- 用户30天未登录 → 自动打标签"流失风险"
- 用户累计消费>1000元 → 自动打标签"高价值"

---

## 功能清单

### P0 — 实时行为聚合

---

#### 1. 行为事件流 [高复杂度 | 3.0人月]

**现状**：EventLogDO仅记录画布触发事件，无通用行为事件流

**需补齐**：

| 子功能 | 描述 |
|--------|------|
| 行为事件定义 | 定义可追踪的行为事件（浏览/点击/购买/登录/分享等） |
| 行为事件上报 | SDK/API上报行为事件（复用EventReportAuthService签名机制） |
| 行为事件存储 | 行为事件写入时序存储（MySQL按日分表/ClickHouse远期） |
| 行为事件查询 | 按用户+时间范围+事件类型查询行为序列 |
| 批量事件上报 | 支持批量上报行为事件（降低网络开销） |

**行为事件结构**：

```json
{
  "userId": "u_12345",
  "event": "page_view",
  "timestamp": "2026-06-01T10:30:00+08:00",
  "properties": {
    "page": "/product/123",
    "source": "email_click",
    "device": "mobile",
    "os": "iOS"
  },
  "context": {
    "ip": "192.168.1.1",
    "userAgent": "Mozilla/5.0...",
    "sessionId": "sess_abc"
  }
}
```

**数据库DDL**：

```sql
CREATE TABLE user_behavior_event (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(64) NOT NULL,
    event_code VARCHAR(100) NOT NULL COMMENT '事件标识',
    event_name VARCHAR(200) COMMENT '事件名称',
    properties JSON COMMENT '事件属性',
    context JSON COMMENT '上下文(ip/userAgent/sessionId)',
    occurred_at DATETIME(3) NOT NULL COMMENT '事件发生时间(毫秒精度)',
    received_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '接收时间',
    tenant_id BIGINT NOT NULL DEFAULT 0,
    INDEX idx_user_time (user_id, occurred_at),
    INDEX idx_event (event_code, occurred_at),
    INDEX idx_tenant_time (tenant_id, occurred_at)
) COMMENT '用户行为事件';

-- 按月分表策略（应用层路由）
-- user_behavior_event_202606
-- user_behavior_event_202607
```

---

#### 2. 实时行为聚合 [高复杂度 | 3.0人月]

**现状**：无任何实时聚合能力

**需补齐**：

| 聚合类型 | 描述 | 示例 |
|---------|------|------|
| 计数聚合 | N天内某事件发生次数 | 7天内打开邮件3次 |
| 最近行为 | 最近一次某事件的时间 | 最近一次登录时间 |
| 频次聚合 | 某事件的日均/周均频次 | 日均浏览5次 |
| 去重计数 | N天内某属性去重数 | 7天浏览不同商品数 |
| 首次/末次 | 首次/末次某事件的时间+属性 | 首次购买时间+金额 |
| 累计聚合 | 全量累计值 | 累计消费金额 |

**聚合计算架构**：

```
行为事件 → Redis实时聚合(滑动窗口) → 定时落盘MySQL
                                        ↓
                              CdpUserProfileDO.propertiesJson更新
                                        ↓
                              触发计算标签规则
```

**Redis聚合结构**：

```
# 计数聚合：7天内email_opened次数
HSET agg:{userId}:email_opened:7d count 3

# 最近行为：最近一次login时间
HSET agg:{userId}:login:latest timestamp 1717200000

# 累计聚合：累计purchase金额
HSET agg:{userId}:purchase:total amount 1250.50

# 去重计数：7天浏览不同商品
PFCOUNT agg:{userId}:page_view:7d:products 15
```

**数据库DDL**：

```sql
CREATE TABLE user_behavior_aggregation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(64) NOT NULL,
    agg_key VARCHAR(200) NOT NULL COMMENT '聚合键 user_id:event:window',
    agg_type VARCHAR(20) NOT NULL COMMENT 'COUNT/LATEST/FREQUENCY/DISTINCT_COUNT/FIRST/LAST/TOTAL',
    event_code VARCHAR(100) NOT NULL COMMENT '事件标识',
    window_type VARCHAR(20) NOT NULL COMMENT 'DAY/7D/30D/ALL',
    numeric_value DECIMAL(18,4) COMMENT '数值结果',
    string_value VARCHAR(500) COMMENT '字符串结果',
    timestamp_value DATETIME(3) COMMENT '时间结果',
    computed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    UNIQUE INDEX uk_agg (agg_key),
    INDEX idx_user (user_id),
    INDEX idx_event (event_code),
    INDEX idx_tenant (tenant_id)
) COMMENT '用户行为聚合结果';
```

---

### P1 — 计算标签与身份图

---

#### 3. 计算标签引擎 [高复杂度 | 4.0人月]

**现状**：标签只能手动/画布/导入写入，无自动计算

**需补齐**：

| 子功能 | 描述 |
|--------|------|
| 计算规则定义 | 定义标签的计算规则（条件+聚合+阈值） |
| 规则触发 | 行为事件到达时触发规则评估 |
| 标签写入 | 规则匹配后自动写入CdpUserTagDO |
| 标签过期 | 条件不再满足时自动移除/过期标签 |
| 规则优先级 | 多条规则冲突时按优先级处理 |
| 规则测试 | 预览规则对现有用户的匹配结果 |

**计算规则示例**：

```json
{
  "tagCode": "email_active",
  "tagName": "邮件活跃",
  "rule": {
    "conditions": [{
      "aggType": "COUNT",
      "event": "email_opened",
      "window": "7D",
      "operator": "GTE",
      "value": 3
    }],
    "logic": "AND"
  },
  "onUnmatch": "REMOVE_TAG",
  "priority": 10,
  "ttlDays": 7
}
```

**计算规则类型**：

| 规则类型 | 描述 | 示例 |
|---------|------|------|
| 行为频次 | N天内某事件发生≥N次 | 7天打开邮件≥3次 → "邮件活跃" |
| 行为衰减 | N天内某事件未发生 | 30天未登录 → "流失风险" |
| 累计阈值 | 累计值超过阈值 | 累计消费>1000 → "高价值" |
| 行为序列 | 事件A后N天内发生事件B | 注册后7天内购买 → "快速转化" |
| 属性匹配 | 用户属性满足条件 | 城市=北京 AND VIP等级≥3 → "北京VIP" |
| 组合规则 | 多条件AND/OR组合 | (7天打开邮件≥3次 OR 7天点击≥2次) AND 未退订 → "高互动" |

**数据库DDL**：

```sql
CREATE TABLE computed_tag_rule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tag_code VARCHAR(100) NOT NULL COMMENT '目标标签编码',
    tag_name VARCHAR(200) NOT NULL COMMENT '目标标签名称',
    rule_type VARCHAR(30) NOT NULL COMMENT 'FREQUENCY/DECAY/THRESHOLD/SEQUENCE/ATTRIBUTE/COMPOSITE',
    conditions JSON NOT NULL COMMENT '计算条件',
    logic VARCHAR(10) NOT NULL DEFAULT 'AND' COMMENT 'AND/OR',
    on_unmatch VARCHAR(20) NOT NULL DEFAULT 'KEEP' COMMENT 'KEEP/REMOVE_TAG/EXPIRE_TAG',
    ttl_days INT COMMENT '标签有效期(天)',
    priority INT NOT NULL DEFAULT 0 COMMENT '优先级(高优先)',
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    last_computed_at DATETIME COMMENT '最近计算时间',
    match_count BIGINT NOT NULL DEFAULT 0 COMMENT '匹配用户数',
    tenant_id BIGINT NOT NULL DEFAULT 0,
    INDEX idx_tag (tag_code),
    INDEX idx_enabled (enabled),
    INDEX idx_tenant (tenant_id)
) COMMENT '计算标签规则';

CREATE TABLE computed_tag_rule_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    rule_id BIGINT NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    event_code VARCHAR(100) COMMENT '触发事件',
    matched TINYINT(1) NOT NULL COMMENT '是否匹配',
    old_tag_value VARCHAR(200) COMMENT '变更前标签值',
    new_tag_value VARCHAR(200) COMMENT '变更后标签值',
    computed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_rule (rule_id),
    INDEX idx_user (user_id),
    INDEX idx_time (computed_at)
) COMMENT '计算标签执行日志';
```

---

#### 4. 身份图（ID Mapping） [中复杂度 | 2.0人月]

**现状**：userId是单一标识，无跨渠道ID合并

**需补齐**：

| 子功能 | 描述 |
|--------|------|
| 多ID关联 | 同一自然人的不同ID关联（手机号/邮箱/设备ID/企微ID/微信OpenID） |
| ID合并 | 识别到两个ID属于同一人时自动合并 |
| 主ID选择 | 合并后选择主ID（优先手机号>邮箱>设备ID） |
| 合并历史 | 记录ID合并历史 |
| 画像合并 | 合并时合并两个ID的标签+行为数据 |

**身份图结构**：

```
自然人 #1001
  ├── 手机号: 138xxxx1234 (PRIMARY)
  ├── 邮箱: user@example.com
  ├── 设备ID: device_abc
  ├── 企微ID: ww_xxx
  └── 微信OpenID: o_xxx
```

**数据库DDL**：

```sql
CREATE TABLE identity_graph (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    canonical_id VARCHAR(64) NOT NULL COMMENT '主ID(自然人ID)',
    id_type VARCHAR(20) NOT NULL COMMENT 'PHONE/EMAIL/DEVICE/WEWORK/WECHAT/USER_ID',
    id_value VARCHAR(200) NOT NULL COMMENT 'ID值',
    is_primary TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否主ID',
    merged_from VARCHAR(64) COMMENT '合并来源ID',
    merged_at DATETIME COMMENT '合并时间',
    confidence DECIMAL(3,2) NOT NULL DEFAULT 1.0 COMMENT '匹配置信度',
    tenant_id BIGINT NOT NULL DEFAULT 0,
    UNIQUE INDEX uk_id (id_type, id_value),
    INDEX idx_canonical (canonical_id),
    INDEX idx_tenant (tenant_id)
) COMMENT '身份图(ID Mapping)';

CREATE TABLE identity_merge_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    source_canonical_id VARCHAR(64) NOT NULL COMMENT '被合并的主ID',
    target_canonical_id VARCHAR(64) NOT NULL COMMENT '合并到的主ID',
    merge_reason VARCHAR(50) NOT NULL COMMENT 'SAME_PHONE/SAME_EMAIL/MANUAL/RULE',
    merged_tags INT NOT NULL DEFAULT 0 COMMENT '合并标签数',
    merged_events INT NOT NULL DEFAULT 0 COMMENT '合并事件数',
    merged_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    INDEX idx_source (source_canonical_id),
    INDEX idx_target (target_canonical_id)
) COMMENT '身份合并日志';
```

---

#### 5. 用户行为时间线 [中复杂度 | 1.5人月]

**现状**：CdpUserInsightService仅聚合画布执行记录，无行为事件时间线

**需补齐**：

| 子功能 | 描述 |
|--------|------|
| 行为时间线 | 用户所有行为事件按时间排列 |
| 事件筛选 | 按事件类型/时间范围筛选 |
| 画布事件叠加 | 在时间线上叠加画布触达事件 |
| 标签变更叠加 | 在时间线上叠加标签变更事件 |
| 时间线导出 | 导出用户行为时间线 |

**时间线API**：

```
GET /cdp/users/{userId}/timeline?from=2026-06-01&to=2026-06-30&events=page_view,email_opened

Response:
[
  {"time": "2026-06-01 10:30", "type": "CANVAS_TRIGGER", "data": {"canvas": "欢迎邮件", "node": "SendEmail"}},
  {"time": "2026-06-01 10:35", "type": "EVENT", "data": {"event": "email_opened", "properties": {"campaign": "welcome"}}},
  {"time": "2026-06-01 11:00", "type": "TAG_CHANGE", "data": {"tag": "email_active", "old": null, "new": "true"}},
  {"time": "2026-06-02 09:00", "type": "EVENT", "data": {"event": "page_view", "properties": {"page": "/product/123"}}}
]
```

---

### P2 — 高级画像能力

---

#### 6. 用户分群联动 [中复杂度 | 1.5人月]

**描述**：画像标签→人群→画布的联动

| 子功能 | 描述 |
|--------|------|
| 标签人群 | 基于标签条件创建动态人群 |
| 实时人群 | 标签变更时实时更新人群成员 |
| 人群预览 | 预览标签条件匹配的用户数 |
| 人群→画布 | 人群变更触发画布（新增用户进入画布） |

**标签人群规则**：

```json
{
  "name": "高价值流失风险用户",
  "rules": {
    "AND": [
      {"tag": "user_value", "op": "IN", "values": ["HIGH", "MEDIUM"]},
      {"tag": "churn_risk", "op": "EQ", "value": "HIGH"},
      {"tag": "email_active", "op": "NE", "value": "true"}
    ]
  },
  "refreshMode": "REALTIME"
}
```

---

#### 7. 画像评分模型 [低复杂度 | 1.0人月]

**描述**：基于标签+行为计算用户评分

| 评分类型 | 描述 | 示例 |
|---------|------|------|
| RFM评分 | 最近购买/购买频次/消费金额 | R=5,F=3,M=4 → 总分12 |
| 流失风险评分 | 基于行为衰减计算流失概率 | 0.85(高风险) |
| 生命周期评分 | 用户所处生命周期阶段 | "活跃期"/"衰退期"/"流失期" |
| 自定义评分 | 运营自定义评分公式 | score = purchase_count * 10 + email_open_rate * 5 |

---

#### 8. 画像对比分析 [低复杂度 | 0.5人月]

**描述**：对比不同人群的画像差异

| 子功能 | 描述 |
|--------|------|
| 人群画像对比 | 两个人群的标签分布对比 |
| 标签分布 | 单个标签在人群中的分布 |
| 显著差异 | 自动识别两个人群差异最大的标签 |

---

## 工作量估算

| 优先级 | 功能 | 后端人月 | 前端人月 | 测试人月 | 总计 |
|--------|------|---------|---------|---------|------|
| P0 | 行为事件流 | 2.5 | 0.5 | 0.3 | 3.3 |
| P0 | 实时行为聚合 | 2.5 | 0.5 | 0.3 | 3.3 |
| P1 | 计算标签引擎 | 3.0 | 1.0 | 0.5 | 4.5 |
| P1 | 身份图(ID Mapping) | 1.5 | 0.5 | 0.3 | 2.3 |
| P1 | 用户行为时间线 | 1.0 | 0.5 | 0.2 | 1.7 |
| P2 | 用户分群联动 | 1.0 | 0.5 | 0.2 | 1.7 |
| P2 | 画像评分模型 | 0.7 | 0.3 | 0.1 | 1.1 |
| P2 | 画像对比分析 | 0.3 | 0.2 | 0.1 | 0.6 |
| | **合计** | **12.5** | **4.0** | **2.0** | **18.5** |

---

## 执行顺序

```
Sprint 1 (P0-事件): 行为事件流 — 3.3人月
  → 产出：行为事件定义+上报+存储+查询

Sprint 2 (P0-聚合): 实时行为聚合 — 3.3人月
  → 产出：Redis实时聚合+MySQL落盘+聚合查询API

Sprint 3 (P1-计算标签): 计算标签引擎 — 4.5人月
  → 产出：规则定义+自动计算+标签写入+过期

Sprint 4 (P1-身份): 身份图+时间线 — 4.0人月
  → 产出：ID Mapping+行为时间线

Sprint 5 (P2-高级): 分群+评分+对比 — 3.4人月
  → 产出：标签人群联动+RFM评分+画像对比
```

---

## 风险评估

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| 行为事件量大 | user_behavior_event表数据膨胀 | 按月分表+ClickHouse远期迁移 |
| Redis内存压力 | 实时聚合占用Redis内存 | 仅聚合高频指标+TTL自动过期 |
| 计算标签延迟 | 事件→标签计算链路长 | 异步计算+批量处理+延迟容忍5分钟 |
| 身份合并误判 | 不同人同手机号导致误合并 | 置信度评分+人工确认机制 |
| 事件上报丢失 | 客户端网络问题导致事件丢失 | 本地缓存+重试+服务端幂等 |

---

## 与其他方向的关系

| 方向 | 与⑬的关系 |
|------|----------|
| ① 营销深度 | 画布节点可读取计算标签做条件路由 |
| ② 私域中台 | 企微用户画像=身份图+行为聚合 |
| ④ AI-Native | AI模型依赖行为数据训练 |
| ⑨ 营销数据中台 | 回执事件是行为事件的重要来源 |
| ⑩ 客户旅程 | 旅程时间线依赖行为事件序列 |
| ⑪ 开放平台 | 行为事件通过SDK上报+Webhook推送 |
