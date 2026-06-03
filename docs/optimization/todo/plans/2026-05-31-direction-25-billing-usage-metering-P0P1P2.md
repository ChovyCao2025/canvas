# 方向㉕：计费与用量计量 — 功能清单

> 定位：从"免费使用"升级为"商业变现"——用量计量+套餐管理+账单+超额告警+自助升级
> 策略评估：SaaS多租户必备(⑫依赖)，无计费=无法商业化；usage-based billing是2025-2026趋势
> 竞品对标：Maxio Metering、Lago、Stripe Billing、Zuora、UniBee
> 建议：**P0必须做**，⑫多租户SaaS化的商业化前提

---

## 参考出处

| 出处 | 核心观点 | 链接 |
|------|---------|------|
| Maxio Metering | Adaptive billing：实时事件追踪+多属性评分+动态定价，2025年SaaS计费趋势 | https://www.maxio.com/blog/introducing-maxio-metering |
| 12 Best Usage-Based Billing Software 2025 | Usage-based billing成为SaaS主流计费模式 | https://unibee.dev/blog/12-best-usage-based-billing-software-for-saas-2025/ |
| Lago: Usage-Based Pricing Examples | 按API调用/消息数/存储计费的成功案例 | https://getlago.com/blog/usage-based-pricing-examples |
| LedgerUp: Evaluate Usage-Based Billing Platforms | Metering+定价逻辑分离是现代计费架构核心 | https://www.ledgerup.ai/resources/ledgerup-best-usage-billing-platforms-2025 |
| BillingPlatform: Usage-Based Billing Enterprise | Gartner Leader，实时评分+灵活定价+可扩展变现 | https://billingplatform.com/usage-based-billing |

---

## 现状盘点

| 功能 | 实现程度 | 核心现有代码 | 差距 |
|------|----------|-------------|------|
| 租户管理 | **部分** | TenantDO(id/tenantKey/name/status/planCode/quotaJson/remark) | 有planCode+quotaJson但无解析使用 |
| 用量记录 | **部分** | CanvasUserQuotaDO(canvasId/userId/dailyCount/totalCount) | 仅画布触发量，非通用计量 |
| 配额检查 | **部分** | TriggerPreCheckService(冷却期+频次) | 仅运行时检查，无计费逻辑 |
| 用量计量 | **不存在** | — | 无消息数/人数/API调用等计量 |
| 套餐管理 | **不存在** | — | 无套餐定义/定价/功能限制 |
| 账单 | **不存在** | — | 无月度账单+用量明细 |
| 超额告警 | **不存在** | — | 无用量接近限额告警 |
| 自助升级 | **不存在** | — | 无在线升级/降级 |
| 支付 | **不存在** | — | 无支付对接 |

### 关键洞察

TenantDO.planCode + quotaJson：
- **planCode**：有套餐编码字段，但无套餐定义表
- **quotaJson**：配额以JSON存储，但无标准化解析
- **无用量比对**：不知道当前用量是否接近配额

CanvasUserQuotaDO：
- 仅记录画布触发的dailyCount/totalCount
- 不是通用计量，无法计费
- 异步UPSERT，非实时

计费架构缺失的核心环节：
1. **无计量(Metering)**：不知道每个租户用了多少消息/API/存储
2. **无定价(Pricing)**：不知道每单位用量多少钱
3. **无账单(Billing)**：无法生成月度账单
4. **无支付(Payment)**：无法收款

---

## 功能清单

### P0 — 用量计量与套餐管理

---

#### 1. 用量计量引擎 [高复杂度 | 2.5人月]

**现状**：无通用计量

**需补齐**：

| 计量维度 | 描述 | 计量方式 |
|---------|------|---------|
| 消息数 | 按渠道统计消息发送量 | 每次Handler执行成功+1 |
| 触达人数 | 不重复用户触达量 | Redis HyperLogLog |
| 画布执行数 | 画布触发执行次数 | CanvasExecutionService已有数据 |
| API调用数 | 开放API调用次数 | API Gateway统计 |
| 存储空间 | 用户数据/事件数据存储量 | 定期统计表行数 |
| 人群数 | 人群规模 | AudienceHandler输出 |

**计量架构**：

```
事件产生(Harndler/Controller) → Redis INCR(实时) → MySQL(持久化)
                                    ↓
                              超额检查(实时)
                                    ↓
                              超额告警/阻断
```

**数据库DDL**：

```sql
-- 计量维度定义
CREATE TABLE metering_dimension (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    dimension_key VARCHAR(50) NOT NULL COMMENT 'MESSAGES/REACH_USERS/CANVAS_EXECUTIONS/API_CALLS/STORAGE/AUDIENCE_SIZE',
    name VARCHAR(100) NOT NULL COMMENT '维度名称',
    unit VARCHAR(20) NOT NULL COMMENT '次/人/MB/Gb',
    description VARCHAR(500),
    tenant_id BIGINT NOT NULL DEFAULT 0,
    UNIQUE INDEX uk_key (dimension_key),
    INDEX idx_tenant (tenant_id)
) COMMENT '计量维度定义';

-- 用量快照(每日)
CREATE TABLE usage_daily (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    dimension_key VARCHAR(50) NOT NULL,
    usage_date DATE NOT NULL,
    quantity BIGINT NOT NULL DEFAULT 0 COMMENT '用量',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_tenant_dim_date (tenant_id, dimension_key, usage_date),
    INDEX idx_tenant (tenant_id),
    INDEX idx_date (usage_date)
) COMMENT '每日用量快照';

-- 用量事件流(用于精确计费)
CREATE TABLE usage_event (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    dimension_key VARCHAR(50) NOT NULL,
    event_time DATETIME NOT NULL,
    quantity INT NOT NULL DEFAULT 1 COMMENT '本次用量(通常为1)',
    source_type VARCHAR(30) COMMENT 'CANVAS/API/MANUAL',
    source_ref_id VARCHAR(64) COMMENT '来源关联ID',
    user_id VARCHAR(64) COMMENT '关联用户',
    attributes JSON COMMENT '附加属性(渠道/画布ID等)',
    INDEX idx_tenant_time (tenant_id, event_time),
    INDEX idx_dimension (dimension_key)
) COMMENT '用量事件流';
```

---

#### 2. 套餐与定价管理 [中复杂度 | 2.0人月]

**现状**：planCode存在但无套餐表

**需补齐**：

| 子功能 | 描述 |
|--------|------|
| 套餐定义 | 定义套餐名称/编码/价格/功能列表 |
| 计量配额 | 每个计量维度的配额限制 |
| 超额策略 | 超额后阻断/超额计费/自动升级 |
| 功能权限 | 套餐包含的功能开关(如高级分析/多渠道) |
| 套餐对比 | 用户可对比不同套餐 |
| 定价模型 | 固定月费/阶梯价/按量计费/混合 |

**套餐示例**：

| 套餐 | 月费 | 消息数 | 触达人数 | 画布数 | 渠道 |
|------|------|--------|---------|--------|------|
| 免费版 | 0 | 1,000 | 5,000 | 3 | 短信+邮件 |
| 基础版 | 2,999 | 50,000 | 100,000 | 20 | +Push+InApp |
| 专业版 | 9,999 | 500,000 | 1,000,000 | 100 | +企微+WhatsApp |
| 企业版 | 面议 | 无限 | 无限 | 无限 | 全渠道 |

**数据库DDL**：

```sql
CREATE TABLE pricing_plan (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    plan_code VARCHAR(30) NOT NULL COMMENT 'FREE/BASIC/PRO/ENTERPRISE',
    name VARCHAR(100) NOT NULL,
    price_monthly DECIMAL(12,2) NOT NULL COMMENT '月费',
    price_annual DECIMAL(12,2) COMMENT '年费(优惠)',
    trial_days INT COMMENT '试用天数',
    is_public TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否公开',
    sort_order INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    tenant_id BIGINT NOT NULL DEFAULT 0,
    UNIQUE INDEX uk_code (plan_code),
    INDEX idx_tenant (tenant_id)
) COMMENT '套餐定义';

CREATE TABLE plan_quota (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    plan_code VARCHAR(30) NOT NULL,
    dimension_key VARCHAR(50) NOT NULL,
    quota_limit BIGINT NOT NULL COMMENT '配额限制(-1=无限)',
    overage_price DECIMAL(12,4) COMMENT '超额单价',
    overage_policy VARCHAR(20) NOT NULL DEFAULT 'BLOCK' COMMENT 'BLOCK/OVERAGE_CHARGE/AUTO_UPGRADE',
    INDEX uk_plan_dim (plan_code, dimension_key)
) COMMENT '套餐配额';

CREATE TABLE plan_feature (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    plan_code VARCHAR(30) NOT NULL,
    feature_key VARCHAR(50) NOT NULL COMMENT 'ADVANCED_ANALYTICS/MULTI_CHANNEL/AI/BATCH_EXPORT/SANDBOX',
    included TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否包含',
    INDEX uk_plan_feature (plan_code, feature_key)
) COMMENT '套餐功能权限';
```

---

### P1 — 账单与支付

---

#### 3. 月度账单 [中复杂度 | 1.5人月]

**现状**：无账单

**需补齐**：

| 子功能 | 描述 |
|--------|------|
| 账单生成 | 每月1日自动生成上月账单 |
| 账单明细 | 按维度展示用量+单价+小计 |
| 超额费用 | 超额用量按超额单价计费 |
| 账单通知 | 账单生成后通知租户管理员 |
| 历史账单 | 历史账单查询+导出 |
| 账单争议 | 租户可对账单提出异议 |

**数据库DDL**：

```sql
CREATE TABLE billing_invoice (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    invoice_no VARCHAR(30) NOT NULL COMMENT 'INV-202601-0001',
    period_start DATE NOT NULL COMMENT '账期开始',
    period_end DATE NOT NULL COMMENT '账期结束',
    base_amount DECIMAL(12,2) NOT NULL COMMENT '基础月费',
    overage_amount DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '超额费用',
    total_amount DECIMAL(12,2) NOT NULL COMMENT '总金额',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/ISSUED/PAID/OVERDUE/DISPUTED',
    issued_at DATETIME,
    due_at DATETIME COMMENT '付款截止日',
    paid_at DATETIME,
    UNIQUE INDEX uk_no (invoice_no),
    INDEX idx_tenant (tenant_id),
    INDEX idx_status (status)
) COMMENT '月度账单';

CREATE TABLE billing_invoice_line (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    invoice_id BIGINT NOT NULL,
    dimension_key VARCHAR(50) NOT NULL,
    quantity BIGINT NOT NULL COMMENT '用量',
    quota_limit BIGINT COMMENT '配额',
    included_quantity BIGINT COMMENT '含在月费内的量',
    overage_quantity BIGINT COMMENT '超额量',
    unit_price DECIMAL(12,4) COMMENT '超额单价',
    line_amount DECIMAL(12,2) NOT NULL COMMENT '小计',
    INDEX idx_invoice (invoice_id)
) COMMENT '账单明细行';
```

---

#### 4. 超额告警与自助升降级 [中复杂度 | 1.5人月]

**现状**：无超额告警

**需补齐**：

| 子功能 | 描述 |
|--------|------|
| 用量告警 | 用量达80%/90%/100%时通知 |
| 超额阻断 | 超额后阻断新操作(可配置) |
| 自助升级 | 租户管理员可在线升级套餐 |
| 降级申请 | 降级需申请(防止误操作) |
| 按量计费模式 | 超额部分按量计费而非阻断 |
| 用量仪表盘 | 实时查看各维度用量+配额 |

---

### P2 — 高级计费能力

---

#### 5. 支付集成 [中复杂度 | 1.0人月]

**描述**：在线支付对接

| 子功能 | 描述 |
|--------|------|
| 支付渠道 | 支付宝/微信支付/Stripe/PayPal |
| 自动扣款 | 月度自动扣款(需授权签约) |
| 支付记录 | 支付历史查询+发票 |
| 退款 | 退款处理+退款记录 |
| 对账 | 支付记录与账单对账 |

---

#### 6. 智能定价建议 [低复杂度 | 0.5人月]

**描述**：基于用量数据给出定价建议

| 子功能 | 描述 |
|--------|------|
| 用量分析 | 分析租户用量分布(多少租户超额) |
| 定价模拟 | 模拟不同定价下的收入变化 |
| 套餐优化 | 建议调整配额/价格以优化转化 |
| 竞品定价 | 竞品定价对比 |

---

## 工作量估算

| 优先级 | 功能 | 后端人月 | 前端人月 | 测试人月 | 总计 |
|--------|------|---------|---------|---------|------|
| P0 | 用量计量引擎 | 2.0 | 0.5 | 0.3 | 2.8 |
| P0 | 套餐与定价管理 | 1.5 | 0.5 | 0.2 | 2.2 |
| P1 | 月度账单 | 1.0 | 0.5 | 0.2 | 1.7 |
| P1 | 超额告警与升降级 | 1.0 | 0.5 | 0.2 | 1.7 |
| P2 | 支付集成 | 0.7 | 0.3 | 0.1 | 1.1 |
| P2 | 智能定价建议 | 0.3 | 0.2 | 0.1 | 0.6 |
| | **合计** | **6.5** | **2.5** | **1.1** | **10.1** |

---

## 执行顺序

```
Sprint 1 (P0-计量): 用量计量引擎 — 2.8人月
  → 产出：计量维度+实时计数+每日快照+超额检查

Sprint 2 (P0-套餐): 套餐与定价 — 2.2人月
  → 产出：套餐定义+配额+功能权限+套餐对比

Sprint 3 (P1-账单): 月度账单 — 1.7人月
  → 产出：自动生成+明细+超额费用+通知

Sprint 4 (P1-升降级): 超额告警+自助升降级 — 1.7人月
  → 产出：用量告警+在线升级+降级申请

Sprint 5 (P2-支付): 支付+定价建议 — 1.7人月
  → 产出：在线支付+自动扣款+定价优化
```

---

## 风险评估

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| 计量不准 | 用量统计偏差导致账单争议 | Redis+MySQL双写+对账任务+争议流程 |
| 计费争议 | 租户不认可账单金额 | 明细透明+异议流程+审计日志 |
| 超额阻断 | 阻断导致正常营销中断 | 80%预警+可选超额计费模式+紧急额度 |
| 支付安全 | 支付信息泄露 | PCI DSS合规+Token化+不存储卡号 |
| 定价不合理 | 定价过高转化低/过低亏损 | A/B定价实验+竞品对比+用量分析 |

---

## 与其他方向的关系

| 方向 | 与㉕的关系 |
|------|----------|
| ⑫ 多租户SaaS化 | 计费是多租户商业化的核心 |
| ⑪ 开放平台 | API调用计量是计费的重要维度 |
| ⑰ 运营工作台 | 用量仪表盘是工作台的重要部分 |
| ⑲ 沙箱测试 | 沙箱用量是否计费需定义 |
| ㉑ 优惠券与促销 | 优惠券可作为套餐赠送 |