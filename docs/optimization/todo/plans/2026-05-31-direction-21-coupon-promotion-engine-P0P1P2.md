# 方向㉑：优惠券与促销引擎 — 功能清单

> 定位：从"只能发券"升级为"完整促销引擎"——券模板+规则引擎+核销+社交裂变
> 策略评估：私域运营标配，有赞/微盟/CRMEB均内置50+促销工具；Canvas仅有CouponHandler调外部券系统
> 竞品对标：有赞营销工具(秒杀/拼团/砍价/优惠券/满减)、Braze Content Cards+Promotions、HubSpot Discounts
> 建议：**P1建议做**，私域运营无促销工具=无成交转化能力

---

## 参考出处

| 出处 | 核心观点 | 链接 |
|------|---------|------|
| 有赞全渠道私域客户管理 | 50+营销工具(秒杀/拼团/砍价/优惠券/满减/分销)，私域标配 | https://www.youzan.com/chanpin/siyudianshangyyxt |
| CRMEB商城系统 | 社交裂变：拼团/秒杀/砍价/分销，天然适合微信生态 | https://www.crmeb.com/ask/thread/75202 |
| 2026热门门店系统会员板块 | 全员裂变拓客：全民推广+员工分销双体系，老客推荐新客自动领券 | http://goodgq.com/h-nd-117976.html |
| Klaviyo 8 Marketing Automation Trends 2026 | AI copilots + 促销个性化是2026核心趋势 | https://www.klaviyo.com/blog/marketing-automation-trends |
| Marketing Automation Platform Comparison 2026 | HubSpot/Braze/Klaviyo均内置促销/优惠券管理 | https://www.digitalapplied.com/blog/marketing-automation-platform-comparison-2026 |

---

## 现状盘点

| 功能 | 实现程度 | 核心现有代码 | 差距 |
|------|----------|-------------|------|
| 发券 | **部分** | CouponHandler(@NodeHandlerType("COUPON")) | 仅调外部券系统/issue接口，无券模板管理 |
| 券模板 | **不存在** | — | 无券类型/面额/有效期/使用条件定义 |
| 券核销 | **不存在** | — | 无核销回调/核销记录 |
| 满减/折扣规则 | **不存在** | — | 无促销规则引擎 |
| 社交裂变 | **不存在** | — | 无推荐有礼/老带新/拼团/砍价 |
| 优惠券发放记录 | **不存在** | — | 无发券记录查询 |
| 促销效果统计 | **不存在** | — | 无核销率/ROI统计 |

### 关键洞察

CouponHandler现状：
1. **纯代理模式**：仅向`coupon-service-url`发POST `/issue`请求，自身无任何券逻辑
2. **无券模板**：couponTypeKey只是传给外部系统，本地无券定义
3. **无核销**：发出去的券无法追踪核销状态
4. **无促销规则**：满减/满折/买赠等规则引擎完全缺失
5. **isBenefitNode=true**：已标记为权益节点，但无权益管理能力

私域运营缺促销工具=无法闭环：
- 用户触达后无优惠刺激→转化率低
- 无裂变工具→只能靠广告获客，成本高
- 无核销追踪→无法衡量促销ROI

---

## 功能清单

### P0 — 优惠券核心

---

#### 1. 券模板管理 [中复杂度 | 2.0人月]

**现状**：无券模板

**需补齐**：

| 子功能 | 描述 |
|--------|------|
| 券类型 | 满减券/折扣券/兑换券/运费券/赠品券 |
| 券模板CRUD | 创建/编辑/复制/停用券模板 |
| 券规则 | 使用条件(满X元/指定商品/指定类目/新用户专享) |
| 有效期 | 固定时间段/领取后N天/领取后N小时 |
| 发放总量 | 总量控制+每日限额 |
| 叠加规则 | 是否可与其他券叠加使用 |
| 券码规则 | 系统自动生成/自定义前缀+随机码 |

**数据库DDL**：

```sql
CREATE TABLE coupon_template (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(200) NOT NULL COMMENT '券模板名称',
    coupon_type VARCHAR(20) NOT NULL COMMENT 'FIXED/DISCOUNT/EXCHANGE/SHIPPING/GIFT',
    face_value DECIMAL(12,2) COMMENT '面额(FIXED/SHIPPING)',
    discount_rate DECIMAL(5,2) COMMENT '折扣率(DISCOUNT,如0.85=85折)',
    discount_cap DECIMAL(12,2) COMMENT '折扣上限',
    min_order_amount DECIMAL(12,2) COMMENT '最低消费金额',
    applicable_scope JSON COMMENT '适用范围(商品/类目/全店)',
    applicable_user_scope VARCHAR(20) NOT NULL DEFAULT 'ALL' COMMENT 'ALL/NEW_USER/VIP_ONLY',
    valid_type VARCHAR(20) NOT NULL COMMENT 'FIXED_PERIOD/AFTER_RECEIVE',
    valid_start DATETIME COMMENT '固定有效期开始',
    valid_end DATETIME COMMENT '固定有效期结束',
    valid_days_after_receive INT COMMENT '领取后有效天数',
    total_quantity INT NOT NULL COMMENT '发放总量',
    daily_limit INT COMMENT '每日发放上限',
    per_user_limit INT NOT NULL DEFAULT 1 COMMENT '每人限领',
    stackable TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否可叠加',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/ACTIVE/PAUSED/EXPIRED',
    tenant_id BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status (status),
    INDEX idx_tenant (tenant_id),
    INDEX idx_type (coupon_type)
) COMMENT '券模板';

CREATE TABLE coupon_instance (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    template_id BIGINT NOT NULL COMMENT '券模板ID',
    coupon_code VARCHAR(64) NOT NULL COMMENT '券码',
    user_id VARCHAR(64) NOT NULL COMMENT '持券用户',
    status VARCHAR(20) NOT NULL DEFAULT 'RECEIVED' COMMENT 'RECEIVED/USED/EXPIRED/REVOKED',
    received_at DATETIME NOT NULL COMMENT '领取时间',
    used_at DATETIME COMMENT '使用时间',
    order_id VARCHAR(64) COMMENT '关联订单',
    expires_at DATETIME NOT NULL COMMENT '过期时间',
    source_type VARCHAR(30) COMMENT 'CANVAS/MANUAL/REFERRAL/ACTIVITY',
    source_ref_id VARCHAR(64) COMMENT '来源关联ID(画布ID/活动ID)',
    idempotency_key VARCHAR(128) COMMENT '幂等键',
    tenant_id BIGINT NOT NULL DEFAULT 0,
    UNIQUE INDEX uk_code (coupon_code),
    INDEX idx_user (user_id),
    INDEX idx_template (template_id),
    INDEX idx_status (status),
    INDEX idx_expires (expires_at),
    INDEX idx_tenant (tenant_id)
) COMMENT '券实例';
```

---

#### 2. 促销规则引擎 [高复杂度 | 2.5人月]

**现状**：无促销规则

**需补齐**：

| 促销类型 | 描述 | 规则复杂度 |
|---------|------|-----------|
| 满减 | 满X元减Y元 | 低 |
| 满折 | 满X元打Y折 | 低 |
| 阶梯满减 | 满100减10/满200减30/满300减50 | 中 |
| 买赠 | 买X赠Y | 中 |
| 第N件半价 | 第N件商品5折 | 中 |
| 组合优惠 | A+B组合价 | 高 |
| 限时秒杀 | 限时+限量+固定价 | 中 |
| 拼团 | N人成团享优惠 | 高 |

**规则引擎设计**：

```sql
CREATE TABLE promotion_rule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(200) NOT NULL COMMENT '促销名称',
    rule_type VARCHAR(30) NOT NULL COMMENT 'FULL_REDUCTION/DISCOUNT/LADDER/BUY_GIFT/FLASH_SALE/GROUP_BUY',
    priority INT NOT NULL DEFAULT 0 COMMENT '优先级(越高越优先)',
    conditions JSON NOT NULL COMMENT '触发条件(金额/商品/时间/用户)',
    actions JSON NOT NULL COMMENT '优惠动作(减额/折扣/赠品)',
    stackable TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否可与其他促销叠加',
    exclusive_group VARCHAR(50) COMMENT '互斥组(同组促销只能用一个)',
    start_at DATETIME NOT NULL COMMENT '开始时间',
    end_at DATETIME NOT NULL COMMENT '结束时间',
    usage_limit INT COMMENT '总使用次数限制',
    per_user_limit INT NOT NULL DEFAULT 1 COMMENT '每人使用次数',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/ACTIVE/PAUSED/ENDED',
    tenant_id BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_status (status),
    INDEX idx_tenant (tenant_id),
    INDEX idx_time (start_at, end_at)
) COMMENT '促销规则';
```

---

### P1 — 核销与裂变

---

#### 3. 券核销与追踪 [中复杂度 | 1.5人月]

**现状**：无核销能力

**需补齐**：

| 子功能 | 描述 |
|--------|------|
| 核销回调 | 外部系统核销后回调更新券状态 |
| 主动核销 | 扫码/输入券码核销 |
| 核销记录 | 记录核销时间/订单/金额 |
| 过期清理 | 过期券自动标记EXPIRED |
| 券回收 | 管理员撤回已发未用券 |
| 核销统计 | 核销率/核销金额/核销趋势 |

**核销流程**：

```
1. 外部订单系统完成支付 → 回调 /coupons/redeem
2. 验证券码有效性(未使用+未过期+用户匹配)
3. 更新券状态为USED + 记录order_id
4. 写入核销记录
5. 触发事件(券核销事件可用于画布触发)
```

---

#### 4. 社交裂变促销 [高复杂度 | 2.0人月]

**现状**：无裂变工具

**需补齐**：

| 裂变类型 | 描述 | 参考出处 |
|---------|------|---------|
| 推荐有礼 | 老客推荐新客，双方获券/积分 | 有赞/CRMEB全民推广 |
| 老带新返利 | 新客首单后老客获得返利 | 2026门店系统报告 |
| 拼团 | N人成团享优惠价 | CRMEB拼团 |
| 砍价 | 邀请好友帮砍价 | 有赞砍价 |
| 分销员推广 | 员工/KOC推广获佣金 | CRMEB分销 |
| 签到打卡 | 每日签到得积分/券 | 有赞签到 |

**数据库DDL**：

```sql
CREATE TABLE referral_rule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(200) NOT NULL COMMENT '裂变规则名称',
    rule_type VARCHAR(30) NOT NULL COMMENT 'INVITE/GROUP_BUY/BARGAIN/DISTRIBUTION',
    inviter_reward JSON NOT NULL COMMENT '邀请人奖励(券/积分/余额)',
    invitee_reward JSON COMMENT '被邀请人奖励',
    conditions JSON NOT NULL COMMENT '参与条件',
    start_at DATETIME NOT NULL,
    end_at DATETIME NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    tenant_id BIGINT NOT NULL DEFAULT 0,
    INDEX idx_status (status),
    INDEX idx_tenant (tenant_id)
) COMMENT '裂变规则';

CREATE TABLE referral_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    rule_id BIGINT NOT NULL,
    inviter_user_id VARCHAR(64) NOT NULL COMMENT '邀请人',
    invitee_user_id VARCHAR(64) NOT NULL COMMENT '被邀请人',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/REGISTERED/CONVERTED/REWARDED',
    invite_code VARCHAR(32) COMMENT '邀请码',
    converted_at DATETIME COMMENT '转化时间',
    rewarded_at DATETIME COMMENT '奖励发放时间',
    tenant_id BIGINT NOT NULL DEFAULT 0,
    INDEX idx_inviter (inviter_user_id),
    INDEX idx_invitee (invitee_user_id),
    INDEX idx_rule (rule_id),
    INDEX idx_tenant (tenant_id)
) COMMENT '裂变记录';
```

---

### P2 — 高级促销能力

---

#### 5. 促销效果分析 [中复杂度 | 1.0人月]

**描述**：促销ROI与效果分析

| 子功能 | 描述 |
|--------|------|
| 券核销率 | 按券模板统计领取→核销转化 |
| 促销ROI | 促销优惠金额 vs 带来GMV |
| 裂变效果 | 邀请→注册→转化漏斗 |
| A/B对比 | 不同券面额/类型的效果对比(与⑭集成) |
| 智能发券 | 基于用户画像推荐最优券(与⑬集成) |

---

#### 6. 智能优惠券推荐 [中复杂度 | 1.0人月]

**描述**：基于用户画像和行为自动推荐最优优惠券

| 子功能 | 描述 |
|--------|------|
| 优惠券敏感度 | 计算用户对优惠的敏感度(高/中/低) |
| 最优面额 | 根据敏感度推荐最优面额(避免过度让利) |
| 最佳时机 | 在用户最可能转化的时刻推送优惠 |
| 动态定价 | 根据用户价值动态调整优惠力度 |

---

## 工作量估算

| 优先级 | 功能 | 后端人月 | 前端人月 | 测试人月 | 总计 |
|--------|------|---------|---------|---------|------|
| P0 | 券模板管理 | 1.5 | 0.5 | 0.3 | 2.3 |
| P0 | 促销规则引擎 | 2.0 | 0.5 | 0.3 | 2.8 |
| P1 | 券核销与追踪 | 1.0 | 0.5 | 0.2 | 1.7 |
| P1 | 社交裂变促销 | 1.5 | 0.5 | 0.2 | 2.2 |
| P2 | 促销效果分析 | 0.7 | 0.3 | 0.1 | 1.1 |
| P2 | 智能优惠券推荐 | 0.7 | 0.3 | 0.1 | 1.1 |
| | **合计** | **7.4** | **2.6** | **1.2** | **11.2** |

---

## 执行顺序

```
Sprint 1 (P0-券模板): 券模板管理 — 2.3人月
  → 产出：券模板CRUD+券实例发放+CouponHandler改造

Sprint 2 (P0-规则): 促销规则引擎 — 2.8人月
  → 产出：满减/满折/阶梯/买赠规则+画布促销节点

Sprint 3 (P1-核销): 券核销+追踪 — 1.7人月
  → 产出：核销回调+核销记录+过期清理

Sprint 4 (P1-裂变): 社交裂变促销 — 2.2人月
  → 产出：推荐有礼+拼团+分销员推广

Sprint 5 (P2-分析): 促销效果+智能推荐 — 2.2人月
  → 产出：促销ROI+智能发券
```

---

## 风险评估

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| 券超发 | 超出总量导致亏损 | Redis原子计数+总量预扣 |
| 薅羊毛 | 同一用户多账号领券 | 设备指纹+手机号去重 |
| 核销不一致 | 外部系统延迟回调 | 主动查询+对账任务 |
| 规则冲突 | 多促销规则叠加计算复杂 | 互斥组+优先级+最高优惠限制 |
| 裂变作弊 | 虚假邀请/刷单 | 邀请码唯一+转化条件(首单) |

---

## 与其他方向的关系

| 方向 | 与㉑的关系 |
|------|----------|
| ② 私域运营中台 | 促销引擎是私域转化核心工具 |
| ⑬ 实时用户画像 | 画像驱动智能发券 |
| ⑭ A/B测试平台 | A/B测试不同优惠策略 |
| ⑮ 营销资源中心 | 券模板是营销素材的一种 |
| ㉒ 会员积分体系 | 券+积分联合运营(积分兑换券) |
