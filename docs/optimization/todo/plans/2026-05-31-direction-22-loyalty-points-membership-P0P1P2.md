# 方向㉒：会员积分与忠诚度体系 — 功能清单

> 定位：从"标签分层"升级为"忠诚度闭环"——积分累积+等级权益+积分兑换+游戏化
> 策略评估：私域运营"会员等级+积分+权益卡"是基础；Canvas已有CustomerPointsLedgerDO但无积分规则/等级/兑换
> 竞品对标：Comarch Loyalty、有赞CRM会员体系、Capillary Loyalty+、Kameleoon Gamification
> 建议：**P1建议做**，与㉑促销引擎互补，私域运营双引擎(促销拉新+忠诚度留存)

---

## 参考出处

| 出处 | 核心观点 | 链接 |
|------|---------|------|
| Comarch Loyalty Marketing Platform | AI驱动Next Best Offer+游戏化(挑战/里程碑/社交分享)+全渠道识别 | https://www.comarch.com/trade-and-services/loyalty-marketing/ |
| 11 Best Loyalty Management Software 2026 | 忠诚度管理软件市场爆发，游戏化+AI个性化奖励是趋势 | https://www.capillarytech.com/blog/11-best-loyalty-management-software-in-the-us/ |
| 有赞全渠道私域客户管理 | 会员等级+积分+权益卡，线上/门店/直播间统一会员号与积分 | https://www.youzan.com/chanpin/siyudianshangyyxt |
| 2026品牌私域电商6大趋势 | AI赋能私域，公私域联动，会员精细运营是核心 | https://zhuanlan.zhihu.com/p/1978789415313773636 |
| Customer Engagement Trends 2026 | AI个性化+全渠道策略+忠诚度游戏化是2026客户参与核心 | https://www.giftronaut.com/blog/customer-engagement-trends-for-2025 |
| 2026全流程私域电商平台排行榜 | 50+营销工具(秒杀/拼团/分销/优惠券)+积分体系+会员等级 | https://www.cnblogs.com/newjpz/p/19476703 |

---

## 现状盘点

| 功能 | 实现程度 | 核心现有代码 | 差距 |
|------|----------|-------------|------|
| 积分流水 | **部分** | CustomerPointsLedgerDO(userId/operation/points/pointsType/reason/idempotencyKey/expiresAt) | 有流水表，无积分规则/余额查询 |
| 积分Mapper | **部分** | CustomerPointsLedgerMapper(BaseMapper) | 仅CRUD，无余额聚合查询 |
| 用户画像 | **部分** | CustomerProfileDO(lifecycleStage=NEW/ACTIVE/CHURN_RISK) | 有生命周期阶段，无等级/积分字段 |
| 积分规则 | **不存在** | — | 无积分获取/消耗规则定义 |
| 会员等级 | **不存在** | — | 无等级定义/升级/降级规则 |
| 积分兑换 | **不存在** | — | 无积分商城/兑换商品 |
| 游戏化 | **不存在** | — | 无签到/任务/勋章/排行榜 |
| 权益卡 | **不存在** | — | 无付费会员卡/储值卡 |

### 关键洞察

CustomerPointsLedgerDO已具备积分流水基础：
1. **operation字段**：ADD/DEDUCT/EXPIRE三种操作，设计合理
2. **pointsType字段**：支持多种积分类型(普通/成长/活动)
3. **idempotencyKey**：防重复记账，设计到位
4. **expiresAt**：积分过期时间，支持积分有效期

但缺失的关键环节：
- **无积分规则引擎**：什么行为得多少积分？无配置化
- **无余额查询**：只有流水，无实时余额汇总
- **无等级体系**：积分≠等级，等级有独立权益
- **无兑换能力**：积分只能累积不能消费

---

## 功能清单

### P0 — 积分与等级核心

---

#### 1. 积分规则引擎 [中复杂度 | 2.0人月]

**现状**：有流水表，无规则引擎

**需补齐**：

| 规则类型 | 描述 | 示例 |
|---------|------|------|
| 行为积分 | 用户完成指定行为获得积分 | 注册+50/下单+10元=1分/签到+5 |
| 事件积分 | 画布触发事件获得积分 | 参与活动+100/完成问卷+30 |
| 消费积分 | 消费金额按比例获得积分 | 消费1元=1积分 |
| 邀请积分 | 邀请新用户获得积分 | 邀请1人+200/被邀请人首单+100 |
| 积分过期 | 积分到期自动清零 | 年度积分次年12月31日过期 |
| 积分上限 | 每日/每月积分获取上限 | 每日最多获取500积分 |

**数据库DDL**：

```sql
CREATE TABLE points_rule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(200) NOT NULL COMMENT '规则名称',
    rule_type VARCHAR(30) NOT NULL COMMENT 'BEHAVIOR/EVENT/PURCHASE/INVITE',
    trigger_event VARCHAR(50) NOT NULL COMMENT '触发事件(REGISTER/ORDER/SIGN_IN/CANVAS_EVENT)',
    points_amount INT NOT NULL COMMENT '获得积分数',
    points_type VARCHAR(20) NOT NULL DEFAULT 'STANDARD' COMMENT 'STANDARD/GROWTH/ACTIVITY',
    calculation_mode VARCHAR(20) NOT NULL DEFAULT 'FIXED' COMMENT 'FIXED/RATIO',
    ratio_base VARCHAR(20) COMMENT 'RATIO模式的基数(ORDER_AMOUNT/QUANTITY)',
    ratio_rate DECIMAL(10,4) COMMENT 'RATIO模式的比率',
    daily_cap INT COMMENT '每日获取上限',
    monthly_cap INT COMMENT '每月获取上限',
    lifetime_cap INT COMMENT '终身获取上限',
    expires_after_days INT COMMENT '获得后N天过期',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/PAUSED',
    tenant_id BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_event (trigger_event),
    INDEX idx_status (status),
    INDEX idx_tenant (tenant_id)
) COMMENT '积分规则';

-- 积分余额表(实时汇总，避免每次查流水聚合)
CREATE TABLE points_balance (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(64) NOT NULL,
    points_type VARCHAR(20) NOT NULL DEFAULT 'STANDARD',
    available_points INT NOT NULL DEFAULT 0 COMMENT '可用积分',
    total_earned INT NOT NULL DEFAULT 0 COMMENT '累计获得',
    total_spent INT NOT NULL DEFAULT 0 COMMENT '累计消耗',
    total_expired INT NOT NULL DEFAULT 0 COMMENT '累计过期',
    last_earned_at DATETIME COMMENT '最近获得时间',
    tenant_id BIGINT NOT NULL DEFAULT 0,
    UNIQUE INDEX uk_user_type (user_id, points_type),
    INDEX idx_tenant (tenant_id)
) COMMENT '积分余额';
```

---

#### 2. 会员等级体系 [中复杂度 | 2.0人月]

**现状**：无等级体系

**需补齐**：

| 子功能 | 描述 |
|--------|------|
| 等级定义 | 定义等级名称/图标/升级条件/保级条件 |
| 升级规则 | 按积分/消费金额/指定行为升级 |
| 降级规则 | 保级期内未达标自动降级 |
| 等级权益 | 每个等级对应的权益(折扣/免邮/专属客服/生日礼) |
| 等级变更通知 | 升级/降级时触发通知(可接入画布) |
| 等级有效期 | 等级有效期+保级期 |

**数据库DDL**：

```sql
CREATE TABLE membership_tier (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tier_code VARCHAR(30) NOT NULL COMMENT '等级编码(BRONZE/SILVER/GOLD/PLATINUM/DIAMOND)',
    name VARCHAR(100) NOT NULL COMMENT '等级名称',
    icon_url VARCHAR(500) COMMENT '等级图标',
    level INT NOT NULL COMMENT '等级序号(越高越高级)',
    upgrade_condition JSON NOT NULL COMMENT '升级条件(积分/消费额/行为)',
    maintain_condition JSON COMMENT '保级条件(不满足则降级)',
    validity_days INT COMMENT '等级有效天数',
    maintain_days INT COMMENT '保级评估周期(天)',
    benefits JSON NOT NULL COMMENT '等级权益(折扣率/免邮/专属客服/生日礼等)',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    tenant_id BIGINT NOT NULL DEFAULT 0,
    INDEX idx_level (level),
    INDEX idx_tenant (tenant_id)
) COMMENT '会员等级定义';

CREATE TABLE membership_user_tier (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(64) NOT NULL,
    tier_id BIGINT NOT NULL COMMENT '当前等级ID',
    tier_code VARCHAR(30) NOT NULL COMMENT '当前等级编码',
    achieved_at DATETIME NOT NULL COMMENT '达到时间',
    expires_at DATETIME COMMENT '等级过期时间',
    next_eval_at DATETIME COMMENT '下次保级评估时间',
    tenant_id BIGINT NOT NULL DEFAULT 0,
    UNIQUE INDEX uk_user (user_id),
    INDEX idx_tier (tier_code),
    INDEX idx_expires (expires_at),
    INDEX idx_tenant (tenant_id)
) COMMENT '用户会员等级';
```

---

### P1 — 积分兑换与游戏化

---

#### 3. 积分兑换商城 [中复杂度 | 1.5人月]

**现状**：无兑换能力

**需补齐**：

| 子功能 | 描述 |
|--------|------|
| 兑换商品 | 定义可兑换商品(券/实物/虚拟商品) |
| 兑换规则 | 积分价格+库存+每人限兑 |
| 兑换流程 | 扣减积分→发放商品→记录 |
| 兑换记录 | 兑换历史查询 |
| 积分抵现 | 下单时积分抵扣现金(1积分=X元) |

**数据库DDL**：

```sql
CREATE TABLE points_product (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(200) NOT NULL COMMENT '商品名称',
    product_type VARCHAR(20) NOT NULL COMMENT 'COUPON/PHYSICAL/VIRTUAL/CASH',
    points_price INT NOT NULL COMMENT '积分价格',
    stock INT COMMENT '库存(null=无限)',
    per_user_limit INT NOT NULL DEFAULT 1 COMMENT '每人限兑',
    product_config JSON COMMENT '商品配置(券模板ID/实物SKU等)',
    image_url VARCHAR(500) COMMENT '商品图片',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    tenant_id BIGINT NOT NULL DEFAULT 0,
    INDEX idx_status (status),
    INDEX idx_tenant (tenant_id)
) COMMENT '积分兑换商品';

CREATE TABLE points_exchange_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(64) NOT NULL,
    product_id BIGINT NOT NULL,
    points_cost INT NOT NULL COMMENT '消耗积分',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/FULFILLED/CANCELLED',
    fulfilled_at DATETIME,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    INDEX idx_user (user_id),
    INDEX idx_tenant (tenant_id)
) COMMENT '积分兑换记录';
```

---

#### 4. 游戏化系统 [中复杂度 | 1.5人月]

**现状**：无游戏化

**需补齐**：

| 游戏化元素 | 描述 | 参考出处 |
|-----------|------|---------|
| 每日签到 | 签到得积分，连续签到奖励递增 | 有赞签到打卡 |
| 任务系统 | 完成任务得积分/勋章 | Comarch Gamified Engagement |
| 勋章/成就 | 收集勋章展示成就 | Capillary Gamification |
| 排行榜 | 积分/消费/邀请排行榜 | 社交竞争激励 |
| 里程碑 | 达到指定里程碑触发奖励 | Comarch Tier-based Milestones |
| 社交分享 | 分享成就到社交平台 | Comarch Social Sharing |

**数据库DDL**：

```sql
CREATE TABLE gamification_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(200) NOT NULL COMMENT '任务名称',
    task_type VARCHAR(30) NOT NULL COMMENT 'DAILY_SIGN_IN/DAILY_TASK/ACHIEVEMENT/MILESTONE',
    conditions JSON NOT NULL COMMENT '完成条件',
    reward_points INT NOT NULL COMMENT '奖励积分',
    reward_badge_id BIGINT COMMENT '奖励勋章ID',
    repeatable TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否可重复完成',
    start_at DATETIME,
    end_at DATETIME,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    tenant_id BIGINT NOT NULL DEFAULT 0,
    INDEX idx_type (task_type),
    INDEX idx_tenant (tenant_id)
) COMMENT '游戏化任务';

CREATE TABLE gamification_badge (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT '勋章名称',
    icon_url VARCHAR(500) NOT NULL COMMENT '勋章图标',
    description VARCHAR(500) COMMENT '勋章描述',
    rarity VARCHAR(20) NOT NULL DEFAULT 'COMMON' COMMENT 'COMMON/RARE/EPIC/LEGENDARY',
    tenant_id BIGINT NOT NULL DEFAULT 0
) COMMENT '勋章定义';

CREATE TABLE gamification_user_progress (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(64) NOT NULL,
    task_id BIGINT COMMENT '任务ID',
    badge_id BIGINT COMMENT '勋章ID',
    progress INT NOT NULL DEFAULT 0 COMMENT '进度',
    completed TINYINT(1) NOT NULL DEFAULT 0,
    completed_at DATETIME,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    INDEX idx_user (user_id),
    INDEX idx_tenant (tenant_id)
) COMMENT '用户游戏化进度';
```

---

### P2 — 高级忠诚度能力

---

#### 5. 付费会员卡 [中复杂度 | 1.0人月]

**描述**：付费会员/储值卡体系

| 子功能 | 描述 |
|--------|------|
| 会员卡类型 | 月卡/季卡/年卡/终身卡 |
| 会员卡权益 | 购卡后享受的专属权益 |
| 储值卡 | 充值余额+赠送金额 |
| 自动续费 | 到期自动续费(与支付系统对接) |
| 卡片管理 | 开卡/续费/退卡/冻结 |

---

#### 6. 忠诚度智能分析 [中复杂度 | 1.0人月]

**描述**：忠诚度体系效果分析

| 子功能 | 描述 |
|--------|------|
| 会员价值分析 | 不同等级会员的LTV/复购率/客单价 |
| 积分ROI | 积分发放成本 vs 带来的GMV |
| 升降级分析 | 升级/降级转化率+原因分析 |
| 游戏化效果 | 签到/任务对活跃度的影响 |
| 流失预警 | 会员活跃度下降预警(与㉔集成) |

---

## 工作量估算

| 优先级 | 功能 | 后端人月 | 前端人月 | 测试人月 | 总计 |
|--------|------|---------|---------|---------|------|
| P0 | 积分规则引擎 | 1.5 | 0.5 | 0.2 | 2.2 |
| P0 | 会员等级体系 | 1.5 | 0.5 | 0.2 | 2.2 |
| P1 | 积分兑换商城 | 1.0 | 0.5 | 0.2 | 1.7 |
| P1 | 游戏化系统 | 1.0 | 0.5 | 0.2 | 1.7 |
| P2 | 付费会员卡 | 0.7 | 0.3 | 0.1 | 1.1 |
| P2 | 忠诚度智能分析 | 0.7 | 0.3 | 0.1 | 1.1 |
| | **合计** | **6.4** | **2.6** | **1.0** | **10.0** |

---

## 执行顺序

```
Sprint 1 (P0-积分): 积分规则引擎+余额 — 2.2人月
  → 产出：积分规则CRUD+余额查询+画布积分节点

Sprint 2 (P0-等级): 会员等级体系 — 2.2人月
  → 产出：等级定义+升级/降级+等级权益

Sprint 3 (P1-兑换): 积分兑换商城 — 1.7人月
  → 产出：兑换商品+兑换流程+积分抵现

Sprint 4 (P1-游戏化): 签到+任务+勋章 — 1.7人月
  → 产出：每日签到+任务系统+勋章+排行榜

Sprint 5 (P2-高级): 付费卡+分析 — 2.2人月
  → 产出：付费会员卡+忠诚度分析
```

---

## 风险评估

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| 积分通胀 | 积分发放过多导致贬值 | 积分获取上限+过期机制+积分价值锚定 |
| 等级通胀 | 所有人都是高等级 | 保级机制+等级有效期+降级规则 |
| 兑换库存 | 热门商品瞬间抢空 | 限兑+预约+库存预警 |
| 游戏化疲劳 | 用户对签到/任务失去兴趣 | 任务轮换+限时挑战+社交竞争 |
| 积分安全 | 积分被恶意刷取 | 行为风控+异常检测+积分冻结 |

---

## 与其他方向的关系

| 方向 | 与㉒的关系 |
|------|----------|
| ㉑ 优惠券与促销 | 券+积分联合运营(积分兑换券/积分抵现) |
| ② 私域运营中台 | 忠诚度体系是私域留存核心 |
| ⑬ 实时用户画像 | 画像+积分+等级联动 |
| ㉔ 客户生命周期智能 | 流失预警+等级降级联动 |
| ⑮ 营销资源中心 | 积分商品/勋章是营销素材 |
