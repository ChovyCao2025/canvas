# 方向㉖：功能开关与渐进式发布 — 功能清单

> 定位：从"画布级分流"升级为"功能级开关"——Feature Flag+渐进式发布+个性化规则+灰度管理
> 策略评估：Feature Flag + A/B测试 + 个性化引擎融合是2025-2026趋势；Canvas有KillSwitch和AbSplit但非Feature Flag
> 竞品对标：Kameleoon(Feature Flag+个性化+实验一体化)、Unleash(开源Feature Flag)、Amplitude Feature Management、LaunchDarkly
> 建议：**P2建议做**，与⑭A/B测试互补，开发团队>5人+频繁发布时刚需

---

## 参考出处

| 出处 | 核心观点 | 链接 |
|------|---------|------|
| Kameleoon: Top Feature Flag Management Tools 2026 | Feature Flag+实验+个性化融合是2026方向，Kameleoon一体化平台领先 | https://www.kameleoon.com/blog/top-feature-flag-management-tools |
| Amplitude Feature Management | 渐进式发布+百分比灰度+人群定向，与分析平台深度集成 | https://amplitude.com/compare/best-feature-flag-tools-for-startups |
| AI-Powered Progressive Delivery 2026 | AI驱动的Feature Flag：预测性部署+自动回滚+风险评分 | https://azati.ai/blog/ai-powered-progressive-delivery-feature-flags-2026/ |
| Unleash: Feature Flag Driven Development | FFDD方法论：开发→灰度→全量，解耦部署与发布 | https://www.getunleash.io/blog/feature-flag-driven-development-a-guide |
| Digital Applied: Feature Flag Rollout Strategies 2026 | 灰度策略工程指南：Canary/Ring/Percentage/Kill Switch | https://www.digitalapplied.com/blog/feature-flag-rollout-strategies-2026-engineering-playbook |

---

## 现状盘点

| 功能 | 实现程度 | 核心现有代码 | 差距 |
|------|----------|-------------|------|
| Kill Switch | **完整** | KillSwitchSubscriber(canvas:kill:* Pub/Sub) | 画布级紧急停止，非功能级开关 |
| 画布级灰度 | **部分** | CanvasDO.canaryVersionId | 版本级灰度，非Feature Flag |
| A/B分流 | **完整** | AbSplitHandler(Hash分桶) | 仅画布内分流，非功能开关 |
| 实验分组 | **完整** | ExperimentHandler(WeightedChoice) | 仅画布内实验，非功能级 |
| Feature Flag | **不存在** | — | 完全缺失 |
| 渐进式发布 | **不存在** | — | 无百分比灰度/环形部署 |
| 个性化规则 | **不存在** | — | 无基于用户属性的功能开关 |
| 功能级回滚 | **不存在** | — | Kill Switch是画布级，不是功能级 |

### 关键洞察

KillSwitch vs Feature Flag的核心差异：
- **Kill Switch**：紧急开关，画布级粒度，二元(开/关)，事后应急
- **Feature Flag**：渐进开关，功能级粒度，多值(百分比/人群/属性)，事前控制

当前"画布级分流"的局限：
1. **粒度粗**：只能控制整个画布的执行，不能控制画布内某个功能
2. **无灰度**：AbSplit只能分流画布流量，不能灰度发布新功能
3. **无持久化**：Kill Switch是Redis Pub/Sub，无持久化配置+审计日志
4. **无回滚策略**：关掉后无自动回滚或健康检查

Feature Flag的使用场景（区别于A/B测试）：
- **A/B测试**：验证哪个方案效果更好(统计驱动)
- **Feature Flag**：控制功能的可见性和行为(发布驱动)
- 交集：Feature Flag可用于A/B测试，但A/B测试不是Feature Flag

---

## 功能清单

### P0 — Feature Flag核心

---

#### 1. Feature Flag管理 [中复杂度 | 2.0人月]

**现状**：无Feature Flag

**需补齐**：

| 子功能 | 描述 |
|--------|------|
| Flag CRUD | 创建/编辑/归档功能开关 |
| 开关类型 | 布尔(开/关)/百分比(0-100%)/人群(特定用户)/属性(用户属性匹配) |
| 变体定义 | 多值变体(如: variant_a/variant_b/variant_c) |
| 目标规则 | 基于用户属性/人群/地区的开关规则 |
| 默认值 | Flag求值失败时返回的兜底值 |
| 生命周期 | DRAFT→STAGING→ACTIVE→INACTIVE→ARCHIVED |

**Feature Flag求值流程**：

```
1. 检查Flag是否存在+是否ACTIVE
2. 匹配目标规则(用户属性/人群/地区)
3. 如命中规则→返回规则对应的变体
4. 如未命中→返回默认变体(按百分比分配)
5. 如求值异常→返回fallback值
```

**数据库DDL**：

```sql
CREATE TABLE feature_flag (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    flag_key VARCHAR(100) NOT NULL COMMENT '功能开关标识',
    name VARCHAR(200) NOT NULL COMMENT '开关名称',
    description VARCHAR(500),
    flag_type VARCHAR(20) NOT NULL COMMENT 'BOOLEAN/PERCENTAGE/VARIANTS',
    default_variant VARCHAR(50) NOT NULL DEFAULT 'off' COMMENT '默认变体',
    fallback_variant VARCHAR(50) NOT NULL DEFAULT 'off' COMMENT '兜底变体',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/STAGING/ACTIVE/INACTIVE/ARCHIVED',
    is_permanent TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否永久开关(不归档)',
    owner VARCHAR(64) COMMENT '负责人',
    expires_at DATETIME COMMENT '临时开关过期时间',
    tenant_id BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_key (flag_key),
    INDEX idx_status (status),
    INDEX idx_tenant (tenant_id)
) COMMENT '功能开关';

CREATE TABLE flag_variant (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    flag_id BIGINT NOT NULL,
    variant_key VARCHAR(50) NOT NULL COMMENT '变体标识',
    variant_name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    sort_order INT NOT NULL DEFAULT 0,
    INDEX idx_flag (flag_id)
) COMMENT '功能开关变体';

CREATE TABLE flag_targeting_rule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    flag_id BIGINT NOT NULL,
    rule_type VARCHAR(20) NOT NULL COMMENT 'USER_LIST/USER_ATTRIBUTE/PERCENTAGE/SCHEDULE',
    conditions JSON NOT NULL COMMENT '规则条件',
    variant_key VARCHAR(50) NOT NULL COMMENT '命中后返回的变体',
    priority INT NOT NULL DEFAULT 0 COMMENT '规则优先级(越高越优先)',
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    INDEX idx_flag (flag_id),
    INDEX idx_priority (priority)
) COMMENT '功能开关目标规则';
```

---

#### 2. 渐进式发布 [中复杂度 | 1.5人月]

**现状**：无渐进式发布

**需补齐**：

| 策略 | 描述 | 参考出处 |
|------|------|---------|
| 百分比灰度 | 按百分比逐步放量(1%→5%→10%→25%→50%→100%) | Digital Applied: Rollout Strategies |
| 环形部署 | 内部用户→Beta用户→全部用户 | Unleash: FFDD |
| 金丝雀发布 | 先1%流量观察指标→自动扩量或回滚 | AI-Powered Progressive Delivery |
| 按地区灰度 | 先开放特定地区/国家 | Kameleoon: Geo-targeting |
| 按平台灰度 | 先开放iOS后开放Android | Amplitude: Platform targeting |
| 自动回滚 | 指标异常(错误率>1%)→自动关停 | AI-Powered Feature Flags |

**自动回滚设计**：

```
发布策略:
  type: CANARY
  stages: [1%, 5%, 10%, 25%, 50%, 100%]
  observation_minutes: 30
  rollback_conditions:
    error_rate > 1%: ROLLBACK
    p99_latency > 500ms: PAUSE
    conversion_rate < baseline: NOTIFY
```

**数据库DDL**：

```sql
CREATE TABLE flag_rollout (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    flag_id BIGINT NOT NULL,
    strategy VARCHAR(20) NOT NULL COMMENT 'PERCENTAGE/RING/CANARY/GEO/PLATFORM',
    stages JSON NOT NULL COMMENT '灰度阶段配置',
    current_stage INT NOT NULL DEFAULT 0 COMMENT '当前阶段',
    observation_minutes INT NOT NULL DEFAULT 30 COMMENT '观察时间(分钟)',
    rollback_conditions JSON COMMENT '自动回滚条件',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/RUNNING/PAUSED/COMPLETED/ROLLED_BACK',
    started_at DATETIME,
    completed_at DATETIME,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    INDEX idx_flag (flag_id),
    INDEX idx_status (status)
) COMMENT '功能开关灰度发布';
```

---

### P1 — 个性化与集成

---

#### 3. 个性化规则引擎 [中复杂度 | 1.5人月]

**现状**：无个性化规则

**需补齐**：

| 子功能 | 描述 |
|--------|------|
| 用户属性匹配 | 基于用户画像属性(地区/等级/偏好)决定功能变体 |
| 人群匹配 | 基于CDP人群决定功能变体(与⑬集成) |
| 上下文匹配 | 基于设备/浏览器/时间/来源决定变体 |
| 互斥组 | 同一互斥组内的Flag只能命中一个 |
| 依赖关系 | Flag A依赖Flag B为true时才生效 |

---

#### 4. 画布集成 [低复杂度 | 0.5人月]

**现状**：画布与Feature Flag无关联

**需补齐**：

| 子功能 | 描述 |
|--------|------|
| Feature Flag节点 | 画布中新增Feature Flag判断节点 |
| 条件分支 | Flag=true走A分支，Flag=false走B分支 |
| 实时更新 | Flag变更后画布行为实时生效(无需重新发布) |
| SDK暴露 | 前端通过SDK获取Flag状态控制UI |

**Feature Flag Handler设计**：

```java
@NodeHandlerType("FEATURE_FLAG")
public class FeatureFlagHandler implements NodeHandler {
    private final FeatureFlagService flagService;

    @Override
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        String flagKey = string(config, "flagKey", "");
        return flagService.evaluate(flagKey, ctx.getUserId())
                .map(variant -> NodeResult.routed(variant, switchRoute(variant, config), Map.of(
                        "flagKey", flagKey,
                        "variant", variant
                )));
    }
}
```

---

### P2 — 高级能力

---

#### 5. Flag分析与审计 [中复杂度 | 1.0人月]

**描述**：Feature Flag使用分析与审计

| 子功能 | 描述 |
|--------|------|
| 求值日志 | 记录每次Flag求值结果(采样) |
| 使用分析 | Flag命中分布/变体分布/规则命中率 |
| 变更审计 | Flag配置变更记录+审批流 |
| 过期清理 | 过期临时Flag自动告警+归档 |
| 依赖图 | Flag间依赖关系可视化 |

---

#### 6. AI驱动渐进式发布 [低复杂度 | 0.5人月]

**描述**：AI自动决策灰度节奏

| 子功能 | 描述 |
|--------|------|
| 健康评分 | 基于错误率/延迟/转化率计算功能健康分 |
| 自动扩量 | 健康分>90→自动推进下一阶段 |
| 自动回滚 | 健康分<50→自动回滚上一阶段 |
| 风险预测 | 发布前预测功能风险等级 |

---

## 工作量估算

| 优先级 | 功能 | 后端人月 | 前端人月 | 测试人月 | 总计 |
|--------|------|---------|---------|---------|------|
| P0 | Feature Flag管理 | 1.5 | 0.5 | 0.2 | 2.2 |
| P0 | 渐进式发布 | 1.2 | 0.3 | 0.2 | 1.7 |
| P1 | 个性化规则引擎 | 1.0 | 0.5 | 0.2 | 1.7 |
| P1 | 画布集成 | 0.3 | 0.2 | 0.1 | 0.6 |
| P2 | Flag分析与审计 | 0.7 | 0.3 | 0.1 | 1.1 |
| P2 | AI渐进式发布 | 0.3 | 0.2 | 0.1 | 0.6 |
| | **合计** | **5.0** | **2.0** | **0.9** | **7.9** |

---

## 执行顺序

```
Sprint 1 (P0-Flag): Feature Flag管理 — 2.2人月
  → 产出：Flag CRUD+求值引擎+SDK

Sprint 2 (P0-发布): 渐进式发布 — 1.7人月
  → 产出：百分比灰度+金丝雀+自动回滚

Sprint 3 (P1-个性化): 个性化规则引擎 — 1.7人月
  → 产出：属性匹配+人群匹配+互斥组

Sprint 4 (P1-集成): 画布集成 — 0.6人月
  → 产出：FeatureFlagHandler+前端SDK

Sprint 5 (P2-高级): 分析+AI发布 — 1.7人月
  → 产出：求值分析+审计日志+AI灰度
```

---

## 风险评估

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| Flag泛滥 | 大量未清理Flag导致代码复杂度爆炸 | 过期机制+归档+代码扫描+定期Review |
| 求值延迟 | Flag求值链路过长影响画布性能 | 本地缓存+批量求值+异步日志 |
| 误操作 | 误关关键Flag导致功能不可用 | 变更审批+灰度观察+自动回滚 |
| 规则冲突 | 多规则命中导致不确定行为 | 优先级排序+规则验证+模拟测试 |
| 依赖循环 | Flag间循环依赖 | 依赖图检测+编译时校验 |

---

## 与其他方向的关系

| 方向 | 与㉖的关系 |
|------|----------|
| ⑭ A/B测试平台 | Flag可用于A/B分流，但A/B更关注统计验证 |
| ⑫ 多租户SaaS化 | Flag按租户隔离，不同租户可有不同Flag状态 |
| ⑬ 实时用户画像 | 画像属性作为Flag求值的输入 |
| ④ AI原生平台 | AI驱动的渐进式发布是AI平台的应用场景 |
| ⑪ 开放平台 | Flag SDK作为开放API暴露给第三方 |
| ⑲ 沙箱测试 | 沙箱环境Flag独立于生产 |