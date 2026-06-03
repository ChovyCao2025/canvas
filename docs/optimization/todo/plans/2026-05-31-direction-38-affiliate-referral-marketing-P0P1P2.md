# 方向㊳：联盟营销与推荐裂变引擎 — 功能清单

> 定位：从"自有渠道触达"升级为"用户/合作伙伴驱动的增长引擎"——推荐计划+联盟追踪+佣金管理+裂变活动+防欺诈+归因分析
> 策略评估：2026年联盟营销市场规模持续增长，推荐裂变(Referral)是SaaS增长最有效的获客渠道之一(Dropbox/Uber/Airbnb经典模式)，Affiliate是电商标配
> 竞品对标：Affise(联盟网络平台)、Tremendous(推荐营销工具)、Trakaff(联盟追踪+防欺诈)、Track360(S2S Postback+Privacy Sandbox)
> 建议：**P2建议做**，依赖㉑优惠券+㉒积分体系+㉙欺诈防护成熟后启动，"用户推荐"是私域裂变的核心增长引擎

---

## 参考出处

| 出处 | 核心观点 | 链接 |
|------|---------|------|
| FintelConnect: 7 Affiliate Marketing Tracking Software 2026 | 追踪选项：轻量Pixel→自动化Postback API(Webhook)→批量文件，多种集成方式 | https://www.fintelconnect.com/blog/affiliate-marketing-tracking-software-2026/ |
| Tremendous: 12 Referral Marketing Software Tools 2026 | 自动追踪+验证推荐归属+防欺诈+奖励兑现 | https://www.tremendous.com/blog/referral-marketing-tools/ |
| Track360: 8 Affiliate Tracking Platforms 2026 | S2S Postback+欺诈检测+Privacy Sandbox就绪 | https://track360.io/blog/affiliate-tracking-software-operator-buyer-framework-2026 |
| Trakaff: 12 Best Affiliate Tracking Software 2026 | Affise: 联盟网络+大流量+实时统计 | https://trakaff.com/best-affiliate-tracking-software-in-the-usa |
| Jotform: Top 7 Affiliate Tracking Platforms 2026 | 联盟追踪平台使衡量和分析联盟营销变得更容易 | https://www.jotform.com/blog/affiliate-tracking-platforms/ |
| Klaviyo: Marketing Automation Trends 2026 | 推荐和口碑是2026增长营销的核心——推荐奖励自动化 | https://www.klaviyo.com/blog/marketing-automation-trends |

---

## 现状盘点

| 功能 | 实现程度 | 核心现有代码 | 差距 |
|------|----------|-------------|------|
| 优惠券 | **规划中** | ㉑方向规划 | 用于奖励发放，但无推荐/联盟逻辑 |
| 积分 | **规划中** | ㉒方向规划 | 用于积分奖励，但无推荐追踪 |
| 推荐计划 | **不存在** | — | 无推荐链接/推荐码/分享追踪 |
| 联盟管理 | **不存在** | — | 无联盟注册/审核/佣金/结算 |
| 裂变活动 | **不存在** | — | 无拼团/砍价/分享有礼等裂变机制 |
| 归因追踪 | **不存在** | — | 无推荐归因(Last Click/First Click/Linear) |
| 防欺诈 | **规划中** | ㉙方向规划 | 推荐欺诈需专项防护 |

---

## 功能清单

### P0 — 推荐裂变引擎

#### 1. 推荐计划管理 [中复杂度 | 1.5人月]

| 子功能 | 描述 |
|--------|------|
| 推荐链接生成 | 每个用户生成唯一推荐链接/推荐码+社交分享 |
| 奖励规则配置 | 双向奖励(推荐人+被推荐人)/单边奖励+阶梯奖励 |
| 多计划管理 | 多推荐计划并行(拉新/促活/升级/续费) |
| 落地页 | 推荐专用落地页+品牌定制+社交分享卡片 |

#### 2. 裂变活动引擎 [中复杂度 | 1.5人月]

| 子功能 | 描述 |
|--------|------|
| 拼团 | N人成团→优惠；团长激励+团员优惠 |
| 砍价 | 分享→好友助力砍价→低价购买 |
| 分享有礼 | 分享内容/活动→达到条件→自动发放奖励 |
| 邀请PK | 邀请排行榜+阶段奖励+团队竞赛 |

**数据库DDL**：

```sql
CREATE TABLE referral_program (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    program_key VARCHAR(50) NOT NULL,
    name VARCHAR(200) NOT NULL,
    reward_type VARCHAR(20) NOT NULL COMMENT 'COUPON/POINTS/CASH/CREDIT/FREE_TRIAL',
    reward_config JSON NOT NULL COMMENT '{referrer:{type,value}, referee:{type,value}, tiers:[...]}',
    conversion_event VARCHAR(30) NOT NULL COMMENT 'SIGNUP/FIRST_PURCHASE/SUBSCRIPTION/UPGRADE',
    attribution_window_days INT NOT NULL DEFAULT 30 COMMENT '归因窗口(天)',
    fraud_rules JSON COMMENT '防欺诈规则(同IP/同设备限制)',
    status VARCHAR(20) DEFAULT 'ACTIVE',
    tenant_id BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_tenant (tenant_id)
) COMMENT '推荐计划';

CREATE TABLE referral_tracking (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    referrer_user_id VARCHAR(64) NOT NULL,
    referral_code VARCHAR(20) NOT NULL COMMENT '推荐码',
    referee_user_id VARCHAR(64) COMMENT '被推荐人(转化后填充)',
    conversion_status VARCHAR(20) NOT NULL DEFAULT 'CLICKED' COMMENT 'CLICKED/SIGNED_UP/CONVERTED/REWARDED/CANCELLED',
    conversion_event VARCHAR(30) COMMENT '转化事件',
    source_channel VARCHAR(30) COMMENT 'WECHAT/WEIBO/SMS/COPY_LINK/EMAIL',
    reward_issued TINYINT(1) NOT NULL DEFAULT 0,
    fraud_flagged TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    converted_at DATETIME,
    INDEX idx_referrer (referrer_user_id),
    INDEX idx_code (referral_code),
    INDEX idx_status (conversion_status)
) COMMENT '推荐追踪';
```

### P1 — 联盟营销平台

#### 3. 联盟伙伴管理 [中复杂度 | 1.0人月]

| 子功能 | 描述 |
|--------|------|
| 联盟注册 | 自主注册+审核+入驻 |
| 联盟门户 | 联盟伙伴Portal: 绩效面板+素材下载+佣金查看+提现 |
| 佣金配置 | 按产品/按联盟等级差异化佣金+阶梯佣金+bonus |
| 结算管理 | 佣金计算+审核+批量结算+提现记录 |

#### 4. 归因与追踪 [中复杂度 | 1.0人月]

| 子功能 | 描述 |
|--------|------|
| 多触点归因 | Last Click/First Click/Linear/Time Decay/Position Based |
| 跨设备追踪 | 同一用户跨设备归因 |
| Pixel追踪 | 联盟网站嵌入追踪Pixel |
| S2S Postback | 服务端到服务端Postback通知(无Cookie方案) |

### P2 — 高级能力

#### 5. 联盟市场 [低复杂度 | 0.5人月]
联盟伙伴可上架推广素材+联盟层级管理(普通/银牌/金牌/钻石)

#### 6. 推荐分析 [低复杂度 | 0.5人月]
推荐ROI分析+联盟绩效排行+裂变病毒系数(K-factor)+渠道效果对比

---

## 工作量估算

| 优先级 | 功能 | 总计 |
|--------|------|------|
| P0 | 推荐计划管理 | 1.7人月 |
| P0 | 裂变活动引擎 | 1.7人月 |
| P1 | 联盟伙伴管理 | 1.1人月 |
| P1 | 归因与追踪 | 1.1人月 |
| P2 | 联盟市场+推荐分析 | 1.2人月 |
| | **合计** | **6.8人月** |

## 关键依赖

㉑优惠券促销(奖励载体) + ㉒积分体系(积分奖励) + ㉙欺诈防护(推荐欺诈) + ⑨数据中台(归因分析)
