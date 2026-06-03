# 方向㉙：营销欺诈与滥用防护 — 功能清单

> 定位：从"频控+抑制"升级为"主动反欺诈"——虚假账号检测+优惠券滥用防护+刷单识别+渠道欺诈+营销滥用评分
> 策略评估：营销欺诈是2026新威胁(AI刷单/合成身份/券黄牛)，无防护=营销预算浪费+用户体验下降+合规风险
> 竞品对标：DataDome(Bot防护)、Sumsub(Fraud Trends 2026)、Friendly Captcha(电商反欺诈)、Braze(Suppression+欺诈防护)
> 建议：**P1建议做**，㉑优惠券促销+㉒会员积分上线后刚需，无反欺诈=促销/积分体系不可用

---

## 参考出处

| 出处 | 核心观点 | 链接 |
|------|---------|------|
| Sumsub: Fraud Trends 2026 | 2026欺诈趋势：AI深度伪造+合成身份+多渠道欺诈，需AI验证+生物识别+交易监控 | https://sumsub.com/blog/fraud-trends/ |
| DataDome: Bot Protection & Fraud Prevention | 实时AI检测+拦截恶意Bot流量，保护营销表单/API | https://idenfy.com/blog/best-fraud-prevention-solutions/ |
| Friendly Captcha: E-commerce Fraud Prevention 2026 | 10大电商反欺诈策略：Bot检测+支付欺诈+ATO+分层安全+不可见CAPTCHA | https://friendlycaptcha.com/insights/ecommerce-fraud-prevention/ |
| Kanerika: AI in Fraud Detection 2026 | AI欺诈检测：行为分析+异常检测+实时评分+预测模型 | https://kanerika.com/blogs/ai-in-fraud-detection/ |
| 纗享销客: 2026智能营销合规 | 频率控制合规+AI营销可能"轰炸"用户→反垃圾法规红线 | https://www.fxiaoke.com/crm/information-87779.html |
| iDenfy: Best Fraud Prevention Solutions 2026 | DataDome实时AI+Bot防护+自动欺诈防护 | https://idenfy.com/blog/best-fraud-prevention-solutions/ |

---

## 现状盘点

| 功能 | 实现程度 | 核心现有代码 | 差距 |
|------|----------|-------------|------|
| 频率控制 | **完整** | MarketingPolicyService.consumeFrequency() | 防过度触达，但不防欺诈 |
| 抑制名单 | **完整** | MarketingSuppressionDO + SuppressionCheckHandler | 手动抑制，不防自动滥用 |
| 静默时段 | **完整** | MarketingPolicyService.quietHoursAllowed() | 合规防护，不防欺诈 |
| 身份验证 | **部分** | JWT Auth(登录) | 仅登录验证，不防营销欺诈 |
| 虚假账号检测 | **不存在** | — | 无法识别合成身份/批量注册 |
| 优惠券滥用 | **不存在** | — | CouponHandler无滥用检测 |
| 刷单识别 | **不存在** | — | 无法识别虚假购买行为 |
| 渠道欺诈 | **不存在** | — | 无法识别虚假触达数据 |
| 营销滥用评分 | **不存在** | — | 无用户欺诈风险评分 |
| Bot防护 | **不存在** | — | 无Bot/Crawler识别 |

### 关键洞察

MarketingPolicyService vs 反欺诈的核心差异：
- **MarketingPolicyService**：合规视角(保护用户不被过度触达)
- **反欺诈**：业务视角(保护营销预算不被浪费/滥用)

CouponHandler(㉑方向)的滥用风险：
1. **券黄牛**：批量领券→倒卖/囤积→真实用户无法领券
2. **自刷券**：企业员工自刷优惠券→成本浪费
3. **新用户券滥用**：同一人多次注册新账号→领新用户券
4. **叠加滥用**：多券叠加→零元购

积分体系(㉒方向)的滥用风险：
1. **刷积分**：虚假行为刷积分→兑换高价值商品
2. **积分套现**：积分兑换商品→低价转卖→套现
3. **推荐奖励滥用**：自推荐/假推荐→骗取推荐奖励
4. **签到作弊**：脚本自动签到→刷积分

---

## 功能清单

### P0 — 营销滥用检测

---

#### 1. 用户欺诈评分引擎 [中复杂度 | 2.0人月]

**现状**：无欺诈评分

**需补齐**：

| 检测维度 | 描述 | 检测方法 |
|---------|------|---------|
| 虚假账号 | 批量注册/合成身份 | IP/设备指纹/行为模式异常 |
| 刷单识别 | 虚假购买行为 | 购买后立即退单/同一IP多账号购买 |
| 券滥用 | 优惠券领用异常 | 同一用户/IP/设备多次领券 |
| 积分滥用 | 积分获取异常 | 异常高频签到/虚假推荐 |
| 渠道欺诈 | 虚假触达数据 | 虚假手机号/邮箱/异常打开率 |
| Bot检测 | 自动化脚本行为 | 行为节奏异常/CAPTCHA识别 |

**欺诈评分模型**：

```
FraudScore(user) = Σ(维度权重 × 维度评分)

维度评分:
  account_risk:    合成身份概率(设备/IP/行为聚类)
  behavior_risk:   异常行为模式(领券频率/购买后退单率)
  device_risk:     设备指纹聚类(同一设备多账号)
  ip_risk:         IP聚类(同一IP多账号/VPN/Tor)
  social_risk:     社交图谱异常(无社交关系的推荐)

阈值:
  0-30:  低风险 → 正常处理
  31-60: 中风险 → 限制高频操作+加强验证
  61-80: 高风险 → 阻断领券/积分兑换+人工审核
  81-100: 极高风险 → 自动阻断+标记黑名单
```

**数据库DDL**：

```sql
CREATE TABLE fraud_score (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(64) NOT NULL,
    total_score INT NOT NULL COMMENT '欺诈总分0-100',
    risk_level VARCHAR(10) NOT NULL COMMENT 'LOW/MEDIUM/HIGH/CRITICAL',
    dimension_scores JSON NOT NULL COMMENT '各维度评分',
    top_factors JSON COMMENT 'Top欺诈因素',
    model_version VARCHAR(20),
    scored_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    UNIQUE INDEX uk_user (user_id),
    INDEX idx_risk (risk_level),
    INDEX idx_tenant (tenant_id)
) COMMENT '用户欺诈评分';

CREATE TABLE fraud_event (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(30) NOT NULL COMMENT 'BULK_SIGNUP/COUPON_ABUSE/POINTS_ABUSE/FAKE_ORDER/FAKE_CHANNEL/BOT_DETECTED',
    event_detail JSON NOT NULL COMMENT '事件详情',
    detected_method VARCHAR(20) NOT NULL COMMENT 'RULE/MODEL/MANUAL',
    action_taken VARCHAR(20) NOT NULL COMMENT 'BLOCK/FLAG/REVIEW/WARN',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    INDEX idx_user (user_id),
    INDEX idx_type (event_type),
    INDEX idx_tenant (tenant_id)
) COMMENT '欺诈事件记录';
```

---

#### 2. 优惠券滥用防护 [中复杂度 | 1.5人月]

**现状**：CouponHandler无滥用检测

**需补齐**：

| 子功能 | 描述 | 参考出处 |
|--------|------|---------|
| 领券限制 | 每人/每IP/每设备领券次数上限 | Friendly Captcha: E-commerce fraud |
| 新客券防刷 | 同一设备/IP不能多次领新客券 | 合成身份检测 |
| 券叠加限制 | 券叠加上限+互斥组 | Braze: Promotion rules |
| 核销验证 | 券核销时验证订单真实性(非刷单) | Sumsub: Transaction monitoring |
| 券转售检测 | 同一券被不同用户核销(黄牛倒卖) | 订单-券关联分析 |
| 黑名单机制 | 滥用用户自动进券黑名单 | Fraud Score阈值 |

**优惠券滥用检测规则**：

```
领券阶段:
  同一user_id领同一券模板 ≤ 1次(默认)
  同一device_fingerprint领同一券模板 ≤ 3次(新客券≤1)
  同一ip_address领同一券模板 ≤ 5次/天

核销阶段:
  核销订单金额 ≥ 券面额 × 2(防止零元购)
  核销用户 ≠ 领券用户时需风控审核(防转售)
  核销后24h内退单率 > 50% → 券失效+标记滥用

叠加阶段:
  最多叠加 2 张券(可配置)
  叠加后折扣 ≤ 70%(防止过度折扣)
```

**数据库DDL**：

```sql
CREATE TABLE coupon_abuse_rule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    rule_type VARCHAR(30) NOT NULL COMMENT 'CLAIM_LIMIT/DEVICE_LIMIT/IP_LIMIT/STACK_LIMIT/VERIFICATION',
    conditions JSON NOT NULL COMMENT '规则条件',
    action VARCHAR(20) NOT NULL COMMENT 'BLOCK/FLAG/WARN',
    priority INT NOT NULL DEFAULT 0,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    INDEX idx_type (rule_type),
    INDEX idx_tenant (tenant_id)
) COMMENT '优惠券滥用防护规则';
```

---

### P1 — 渠道与积分防护

---

#### 3. 积分滥用防护 [中复杂度 | 1.0人月]

**现状**：无积分滥用检测

**需补齐**：

| 子功能 | 描述 |
|--------|------|
| 获取异常检测 | 异常高频积分获取行为(签到刷分/虚假推荐) |
| 兑换异常检测 | 大额兑换+短时间多兑换+兑换后转卖 |
| 推荐奖励防刷 | 同一IP/设备多新注册+推荐人集中获利 |
| 积分冻结 | 异常行为→积分暂时冻结+人工审核 |
| 兑换限制 | 每日/每周兑换上限+单次兑换上限 |
| 异常回溯 | 发现滥用后回溯扣减非法获取的积分 |

---

#### 4. 渠道欺诈防护 [低复杂度 | 0.5人月]

**现状**：无渠道欺诈检测

**需补齐**：

| 子功能 | 描述 |
|--------|------|
| 虚假手机号 | 检测虚拟号码/一次性号码(短信触达浪费) |
| 虚假邮箱 | 检测 disposable email(邮件触达浪费) |
| 异常打开率 | 同一IP批量打开邮件→刷打开率数据 |
| 点击欺诈 | 短链接点击异常(同一IP高频点击) |
| 渠道评分 | 用户渠道可信度评分(影响触达优先级) |

---

### P2 — 高级反欺诈能力

---

#### 5. AI欺诈检测 [中复杂度 | 1.0人月]

**描述**：AI驱动的欺诈检测

| 子功能 | 描述 |
|--------|------|
| 行为模式学习 | 学习正常用户行为模式→识别异常 |
| 图谱分析 | 用户-设备-IP-行为关联图谱→识别团伙 |
| 预测性拦截 | 预测即将发生的滥用行为→提前拦截 |
| 自适应阈值 | 根据业务变化自动调整检测阈值 |
| 实时评分 | 关键操作(领券/兑换)时实时评分 |

---

#### 6. Bot防护与CAPTCHA [低复杂度 | 0.5人月]

**描述**：自动化脚本识别与拦截

| 子功能 | 描述 |
|--------|------|
| 行为指纹 | 用户行为节奏+轨迹识别Bot vs Human |
| 不可见CAPTCHA | Friendly Captcha式无感验证 |
| 频率异常 | 异常高频操作(1秒签到5次)→Bot标记 |
| 设备指纹 | 识别模拟器/脚本运行环境 |

---

## 工作量估算

| 优先级 | 功能 | 后端人月 | 前端人月 | 测试人月 | 总计 |
|--------|------|---------|---------|---------|------|
| P0 | 用户欺诈评分引擎 | 1.5 | 0.5 | 0.2 | 2.2 |
| P0 | 优惠券滥用防护 | 1.2 | 0.3 | 0.2 | 1.7 |
| P1 | 积分滥用防护 | 0.7 | 0.3 | 0.1 | 1.1 |
| P1 | 渠道欺诈防护 | 0.3 | 0.2 | 0.1 | 0.6 |
| P2 | AI欺诈检测 | 0.7 | 0.3 | 0.1 | 1.1 |
| P2 | Bot防护与CAPTCHA | 0.3 | 0.2 | 0.1 | 0.6 |
| | **合计** | **4.7** | **1.8** | **0.7** | **7.2** |

---

## 执行顺序

```
Sprint 1 (P0-评分): 用户欺诈评分引擎 — 2.2人月
  → 产出：欺诈评分模型+维度评分+风险分级+事件记录

Sprint 2 (P0-券防护): 优惠券滥用防护 — 1.7人月
  → 产出：领券限制+核销验证+叠加限制+黑名单

Sprint 3 (P1-积分): 积分滥用防护 — 1.1人月
  → 产出：获取/兑换异常检测+推荐防刷+积分冻结

Sprint 4 (P1-渠道): 渠道欺诈防护 — 0.6人月
  → 产出：虚假号码检测+异常打开率+渠道评分

Sprint 5 (P2-高级): AI欺诈+Bot防护 — 1.7人月
  → 产出：行为模式学习+图谱分析+不可见CAPTCHA
```

---

## 风险评估

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| 误判 | 正常用户被标记为欺诈→体验下降 | 分级处理+人工审核通道+误判申诉 |
| 遗漏 | 欺诈行为未被识别→营销预算浪费 | 多维度检测+模型定期更新+团伙识别 |
| 用户隐私 | 欺诈检测收集设备/IP等数据→隐私争议 | 最小化收集+脱敏存储+合规声明 |
| 规则冲突 | 多规则交叉导致不确定行为 | 优先级排序+规则一致性检查+模拟测试 |
| 误伤合作 | 代理商/经销商批量操作被误判 | 白名单机制+代理商认证+批量操作审批 |

---

## 与其他方向的关系

| 方向 | 与㉙的关系 |
|------|----------|
| ㉑ 优惠券与促销引擎 | 券滥用防护是促销引擎的必备配套 |
| ㉒ 会员积分体系 | 积分滥用防护是积分体系的必备配套 |
| ⑫ 多租户SaaS化 | 欺诈防护需按租户配置不同规则 |
| ⑦ 合规渠道护城河 | 渠道欺诈检测+虚假号码检测是合规的一部分 |
| ④ AI原生平台 | AI欺诈检测是AI平台的应用场景 |
| ㉗ 偏好与同意管理 | 偏好数据+设备指纹用于欺诈评分 |