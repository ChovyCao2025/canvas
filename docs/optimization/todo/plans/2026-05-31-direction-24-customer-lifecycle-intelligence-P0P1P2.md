# 方向㉔：客户生命周期智能 — 功能清单

> 定位：从"规则驱动"升级为"智能驱动"——CLV计算+流失预警+Next Best Action+生命周期阶段自动推进
> 策略评估：CLV+流失预测AI市场2036年达107.4亿美元，Iterable Nova已落地AI Agent自动编排
> 竞品对标：CleverTap NBA Platform、Iterable Nova AI Agent、HighTouch Next Best Action、Comarch Predictive Loyalty
> 建议：**P2建议做**，依赖⑨回执数据+⑬用户画像，是AI能力的具体应用场景

---

## 参考出处

| 出处 | 核心观点 | 链接 |
|------|---------|------|
| CleverTap: Top 8 Next Best Action Platforms | NBA是全生命周期编排核心，需要行为数据+预测模型+实时决策 | https://clevertap.com/blog/next-best-action-platforms/ |
| Global CLV and Churn Prediction AI Market 2026-2036 | 市场规模2036年达107.4亿美元，2026-2030加速采用 | https://www.morningstar.com/news/accesswire/1162387msn/global-clv-and-churn-prediction-ai-market-to-reach-usd-1074-billion-by-2036 |
| Iterable Nova AI Agent | 定义目标→AI自动分析数据→识别人群→启动优化多渠道活动，数天→数分钟 | https://www.businesswire.com/news/home/20250402556728/en/Iterable-Unveils-Iterable-Nova-A-New-AI-Agent-to-Power-Moments-Based-Marketing |
| HighTouch: What is Next Best Action | NBA=用户行为+业务目标→最优下一步动作 | https://hightouch.com/blog/next-best-action |
| Future of CLV: AI Revolutionizing Lifecycle Marketing | 2025是AI+CLV分水岭，预测性CLV+AI推荐成标配 | https://web.superagi.com/future-of-clv-how-ai-is-revolutionizing-customer-lifecycle-marketing-in-2025-and-beyond/ |
| Best Customer Data Platform 2026 | SaaS-Native CDP聚焦产品使用数据→流失预测+扩展收入 | https://houseofmartech.com/blog/best-customer-data-platform-for-saas-companies-2025 |

---

## 现状盘点

| 功能 | 实现程度 | 核心现有代码 | 差距 |
|------|----------|-------------|------|
| 条件分流 | **完整** | IfConditionHandler + RuleEvaluator | 规则驱动，非预测驱动 |
| 人群筛选 | **完整** | AudienceHandler + CdpAudienceSourceService | 静态人群，无预测 |
| AI NBA节点 | **存根** | AiNextBestActionHandler(仅fallback) | 完全无AI能力 |
| 用户画像 | **部分** | CustomerProfileDO(lifecycleStage) | 有lifecycleStage字段但无自动推进 |
| CLV计算 | **不存在** | — | 完全缺失 |
| 流失预测 | **不存在** | — | 完全缺失 |
| 生命周期阶段 | **部分** | lifecycleStage=NEW/ACTIVE/CHURN_RISK | 3个阶段，无自动转换规则 |

### 关键洞察

AiNextBestActionHandler现状：
1. **纯占位实现**：只读取fallbackAction/fallbackRoute/fallbackNodeId，直接返回
2. **aiFallbackUsed=true**：标记"使用了fallback"，暗示AI不可用
3. **无ML模型调用**：无任何模型推理逻辑
4. **设计预留了扩展点**：NodeResult.routed(route, target, outputs)结构可承载NBA结果

CustomerProfileDO.lifecycleStage：
- 仅有3个阶段：NEW/ACTIVE/CHURN_RISK
- 无自动推进机制，需人工或规则更新
- 无时间维度(何时变为ACTIVE?何时变为CHURN_RISK?)

---

## 功能清单

### P0 — CLV与流失预测

---

#### 1. CLV(客户终身价值)计算引擎 [中复杂度 | 2.0人月]

**现状**：无CLV

**需补齐**：

| 模型类型 | 描述 | 适用场景 |
|---------|------|---------|
| 历史CLV | 基于历史消费数据计算 | 有足够历史数据 |
| 预测CLV | BG/NBD+Gamma-Gamma模型预测 | 新客户/成长期客户 |
| 简化CLV | 平均客单价×购买频率×客户寿命 | 快速估算 |

**CLV计算逻辑**：

```
历史CLV = SUM(订单金额) - SUM(退货金额) - SUM(营销成本)
预测CLV = BG/NBD(购买频率, 流失率) × Gamma-Gamma(客单价分布)
简化CLV = AOV × Freq × Lifespan
```

**数据库DDL**：

```sql
CREATE TABLE customer_clv (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(64) NOT NULL,
    clv_type VARCHAR(20) NOT NULL COMMENT 'HISTORICAL/PREDICTED/SIMPLIFIED',
    clv_value DECIMAL(12,2) NOT NULL COMMENT 'CLV金额',
    confidence DECIMAL(5,2) COMMENT '置信度(0-1)',
    aov DECIMAL(12,2) COMMENT '平均客单价',
    purchase_frequency DECIMAL(8,4) COMMENT '购买频率(次/月)',
    predicted_lifespan_months INT COMMENT '预测生命周期(月)',
    churn_probability DECIMAL(5,4) COMMENT '流失概率(0-1)',
    model_version VARCHAR(20) COMMENT '模型版本',
    calculated_at DATETIME NOT NULL,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    UNIQUE INDEX uk_user_type (user_id, clv_type),
    INDEX idx_tenant (tenant_id)
) COMMENT '客户终身价值';

-- CLV分群(自动分群)
CREATE TABLE clv_segment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT '如: 高价值/中价值/低价值/沉睡',
    min_clv DECIMAL(12,2) COMMENT '最小CLV',
    max_clv DECIMAL(12,2) COMMENT '最大CLV',
    segment_order INT NOT NULL COMMENT '排序',
    color VARCHAR(10) COMMENT '展示颜色',
    tenant_id BIGINT NOT NULL DEFAULT 0,
    INDEX idx_tenant (tenant_id)
) COMMENT 'CLV分群定义';
```

---

#### 2. 流失预测引擎 [中复杂度 | 2.0人月]

**现状**：无流失预测

**需补齐**：

| 子功能 | 描述 |
|--------|------|
| 特征工程 | 从用户行为/消费/交互中提取流失相关特征 |
| 模型训练 | 逻辑回归/随机森林/XGBoost分类模型 |
| 批量预测 | 每日定时对所有活跃用户预测流失概率 |
| 实时评分 | 关键行为发生时实时更新流失概率 |
| 流失原因 | 输出Top-N影响流失的特征 |
| 预警通知 | 高流失概率用户触发预警(接入画布) |

**流失特征清单**：

| 特征类别 | 特征 | 描述 |
|---------|------|------|
| 活跃度 | days_since_last_login | 距上次登录天数 |
| 活跃度 | login_frequency_30d | 30天登录频率 |
| 消费 | days_since_last_purchase | 距上次购买天数 |
| 消费 | purchase_amount_trend | 消费金额趋势(递减=危险) |
| 交互 | email_open_rate_30d | 30天邮件打开率 |
| 交互 | push_click_rate_30d | 30天Push点击率 |
| 负面信号 | unsubscribe_event | 退订事件 |
| 负面信号 | complaint_event | 投诉事件 |

**数据库DDL**：

```sql
CREATE TABLE churn_prediction (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(64) NOT NULL,
    churn_probability DECIMAL(5,4) NOT NULL COMMENT '流失概率(0-1)',
    risk_level VARCHAR(10) NOT NULL COMMENT 'LOW/MEDIUM/HIGH/CRITICAL',
    top_factors JSON COMMENT 'Top流失因素',
    model_version VARCHAR(20),
    predicted_at DATETIME NOT NULL,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    UNIQUE INDEX uk_user_time (user_id, predicted_at),
    INDEX idx_risk (risk_level),
    INDEX idx_tenant (tenant_id)
) COMMENT '流失预测';
```

---

### P1 — Next Best Action

---

#### 3. Next Best Action引擎 [高复杂度 | 2.5人月]

**现状**：AiNextBestActionHandler仅fallback

**需补齐**：

| 子功能 | 描述 |
|--------|------|
| 动作库 | 定义可选动作(发券/推送/电话/邮件/推荐) |
| 动作评分 | 给定用户+上下文，评分每个动作的预期收益 |
| 动作选择 | 选择最高评分动作(或Top-N) |
| 约束满足 | 频次控制+疲劳度+互斥约束 |
| 上下文感知 | 根据当前时间/渠道/会话状态选择动作 |
| A/B验证 | 与⑭集成，验证NBA是否优于规则策略 |

**NBA评分模型**：

```
Score(action|user,context) =
    P(conversion|action, user) × Value(conversion) × Margin(action)
  - Cost(action)
  - FatiguePenalty(user, action, recent_history)
  + ContextBonus(context, action)

其中:
  P(conversion|action, user): 给定用户执行给定动作的转化概率
  Value(conversion): 转化价值
  Margin(action): 动作毛利率
  Cost(action): 动作成本(消息费/券面额/人工成本)
  FatiguePenalty: 疲劳度惩罚(近期已触达则扣分)
  ContextBonus: 上下文加成(如夜间不发Push)
```

**改造AiNextBestActionHandler**：

```java
@NodeHandlerType(NodeType.AI_NEXT_BEST_ACTION)
public class AiNextBestActionHandler implements NodeHandler {
    private final NbaEngine nbaEngine;

    @Override
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        String goal = string(config, "goal", "MAXIMIZE_REVENUE");
        List<String> availableActions = (List<String>) config.getOrDefault("availableActions",
                List.of("COUPON", "PUSH", "EMAIL", "SMS"));

        return nbaEngine.computeBestAction(ctx.getUserId(), goal, availableActions)
                .map(nba -> NodeResult.routed(nba.getActionRoute(), nba.getNextNodeId(),
                        Map.of("nextBestAction", nba.getActionType(),
                               "nbaScore", nba.getScore(),
                               "nbaReason", nba.getReason())));
    }
}
```

**数据库DDL**：

```sql
CREATE TABLE nba_action (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    action_type VARCHAR(30) NOT NULL COMMENT 'COUPON/PUSH/EMAIL/SMS/CALL/RECOMMEND',
    action_name VARCHAR(100) NOT NULL,
    action_config JSON NOT NULL COMMENT '动作配置(关联券模板/消息模板)',
    expected_value DECIMAL(12,2) COMMENT '预期转化价值',
    cost DECIMAL(12,4) COMMENT '动作成本',
    channel VARCHAR(20) NOT NULL COMMENT '触达渠道',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    tenant_id BIGINT NOT NULL DEFAULT 0,
    INDEX idx_channel (channel),
    INDEX idx_tenant (tenant_id)
) COMMENT 'NBA动作库';

CREATE TABLE nba_decision_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(64) NOT NULL,
    goal VARCHAR(30) NOT NULL,
    chosen_action VARCHAR(30) NOT NULL,
    chosen_score DECIMAL(10,4) NOT NULL,
    all_scores JSON COMMENT '所有动作评分',
    context JSON COMMENT '决策上下文',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    INDEX idx_user (user_id),
    INDEX idx_tenant (tenant_id)
) COMMENT 'NBA决策日志';
```

---

#### 4. 生命周期阶段自动推进 [中复杂度 | 1.5人月]

**现状**：lifecycleStage=NEW/ACTIVE/CHURN_RISK，无自动推进

**需补齐**：

| 生命周期阶段 | 推进条件 | 推进方向 |
|-------------|---------|---------|
| NEW→ACTIVE | 注册后7天内完成首购 | 向上 |
| ACTIVE→ENGAGED | 30天内购买≥2次 | 向上 |
| ENGAGED→LOYAL | 累计消费>1000元 | 向上 |
| LOYAL→VIP | 年消费>5000元+推荐3人 | 向上 |
| ACTIVE→CHURN_RISK | 30天无任何交互 | 向下 |
| CHURN_RISK→WINBACK | 挽回成功(首购) | 向上 |
| CHURN_RISK→LOST | 90天无交互 | 向下 |

**阶段推进引擎**：

```
每日定时任务:
1. 扫描所有用户lifecycleStage
2. 对每个用户计算推进条件
3. 满足条件→更新lifecycleStage+记录变更
4. 触发事件(生命周期变更事件)
5. 事件可被画布消费→触发自动化营销
```

**数据库DDL**：

```sql
CREATE TABLE lifecycle_transition_rule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    from_stage VARCHAR(20) NOT NULL,
    to_stage VARCHAR(20) NOT NULL,
    direction VARCHAR(4) NOT NULL COMMENT 'UP/DOWN',
    conditions JSON NOT NULL COMMENT '推进条件',
    evaluation_period_days INT NOT NULL COMMENT '评估周期(天)',
    priority INT NOT NULL DEFAULT 0,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    INDEX idx_from (from_stage),
    INDEX idx_tenant (tenant_id)
) COMMENT '生命周期推进规则';

CREATE TABLE lifecycle_transition_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(64) NOT NULL,
    from_stage VARCHAR(20) NOT NULL,
    to_stage VARCHAR(20) NOT NULL,
    rule_id BIGINT NOT NULL,
    triggered_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    INDEX idx_user (user_id),
    INDEX idx_tenant (tenant_id)
) COMMENT '生命周期变更记录';
```

---

### P2 — 高级智能能力

---

#### 5. 智能最优路径推荐 [中复杂度 | 1.0人月]

**描述**：基于用户画像和历史数据推荐最优营销路径

| 子功能 | 描述 |
|--------|------|
| 路径模板 | 常见生命周期路径模板(新客→首购→复购→忠诚) |
| 路径评分 | 对每个用户评分不同路径的预期效果 |
| 动态调整 | 根据执行效果动态调整路径 |
| 路径对比 | A/B对比不同路径效果 |

---

#### 6. 预测性画布触发 [中复杂度 | 1.0人月]

**描述**：预测性事件驱动画布执行(行为发生前触发)

| 子功能 | 描述 |
|--------|------|
| 预测事件 | 如"3天后可能流失""7天后可能大额消费" |
| 画布联动 | 预测事件触发画布执行(提前干预) |
| 置信度过滤 | 仅高置信度预测触发画布 |
| 效果回测 | 对比干预组vs对照组的实际流失率 |

---

##工作量估算

| 优先级 | 功能 | 后端人月 | 前端人月 | 测试人月 | 总计 |
|--------|------|---------|---------|---------|------|
| P0 | CLV计算引擎 | 1.5 | 0.5 | 0.2 | 2.2 |
| P0 | 流失预测引擎 | 1.5 | 0.5 | 0.2 | 2.2 |
| P1 | Next Best Action引擎 | 2.0 | 0.5 | 0.3 | 2.8 |
| P1 | 生命周期阶段自动推进 | 1.0 | 0.5 | 0.2 | 1.7 |
| P2 | 智能最优路径推荐 | 0.7 | 0.3 | 0.1 | 1.1 |
| P2 | 预测性画布触发 | 0.7 | 0.3 | 0.1 | 1.1 |
| | **合计** | **7.4** | **2.6** | **1.1** | **11.1** |

---

## 执行顺序

```
Sprint 1 (P0-CLV): CLV计算引擎 — 2.2人月
  → 产出：CLV计算+分群+与⑬画像联动

Sprint 2 (P0-流失): 流失预测引擎 — 2.2人月
  → 产出：特征工程+模型训练+批量预测+预警

Sprint 3 (P1-NBA): Next Best Action — 2.8人月
  → 产出：AiNextBestActionHandler改造+动作库+决策引擎

Sprint 4 (P1-生命周期): 生命周期自动推进 — 1.7人月
  → 产出：阶段推进规则+变更事件+画布联动

Sprint 5 (P2-智能): 路径推荐+预测触发 — 2.2人月
  → 产出：最优路径+预测性画布触发
```

---

## 风险评估

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| 数据不足 | 新租户无历史数据，CLV/流失模型不准 | 冷启动模板+简单模型兜底 |
| 模型漂移 | 用户行为变化导致模型效果下降 | 定期重训+效果监控+自动告警 |
| NBA误推 | 推荐动作不合适(如给VIP推大额券) | 约束规则+人工审核+A/B验证 |
| 过度干预 | 预测触发画布过于频繁 | 置信度阈值+频次控制+对照实验 |
| 隐私合规 | 用户行为数据用于预测需授权 | 数据脱敏+合规声明+用户选择退出 |

---

## 与其他方向的关系

| 方向 | 与㉔的关系 |
|------|----------|
| ⑨ 营销数据中台 | 回执数据是CLV/流失模型的训练数据 |
| ⑬ 实时用户画像 | 画像特征→模型输入，模型输出→画像标签 |
| ④ AI原生平台 | NBA和预测能力是AI平台的核心应用 |
| ㉒ 会员积分体系 | CLV与会员等级联动，高CLV→高等级 |
| ⑭ A/B测试平台 | NBA效果验证需要A/B对照实验 |