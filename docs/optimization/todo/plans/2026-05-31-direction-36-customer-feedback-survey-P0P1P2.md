# 方向㊱：客户反馈与调研引擎 — 功能清单

> 定位：从"单向推送触达"升级为"双向反馈闭环"——NPS/CSAT/调研问卷+反馈驱动个性化+闭环恢复+反馈分析+客户之声(VoC)中心
> 策略评估：客户反馈是2026营销自动化的"数据飞轮"——CoreMedia验证了"NPS=3→实时恢复体验"的闭环价值，反馈不再是事后报表而是实时触发器
> 竞品对标：CoreMedia(CSAT/NPS+反馈驱动个性化)、SurveyMonkey(500+模板+200+集成)、Outgrow(互动计算器+测验+评估+对话式调研)、GenZform(AI生成调研+分支逻辑)
> 建议：**P2建议做**，依赖⑨数据中台+⑬画像引擎+㉗偏好中心成熟后启动，反馈→洞察→行动闭环是营销智能化的关键数据源

---

## 参考出处

| 出处 | 核心观点 | 链接 |
|------|---------|------|
| CoreMedia: CSAT & NPS Surveys + Personalization 2026 | 反馈驱动个性化：NPS/CSAT→实时触发恢复/激活→反馈写入统一客户画像→分群按反馈定制体验 | https://www.coremedia.com/personalized-experiences/surveys-csat-nps |
| SurveyMonkey: Enterprise Survey Platform | 500+模板+25+题型+分支逻辑+200+集成+实时报表+GDPR/CCPA合规 | https://www.surveymonkey.com/ |
| Outgrow: Interactive Content Marketing 2026 | 互动计算器+测验+评估+投票+产品推荐引擎+对话机器人+支付/手机验证/优惠券集成 | https://www.linkedin.com/pulse/best-quiz-builders-2026-i-compared-10-tools-so-you-dont-vlad-gozman-kkkke |
| GenZform: AI Quiz, Calculator & Form Builder 2026 | AI自然语言生成调研+分支逻辑+计算字段+动态内容+免费永久计划 | https://genzform.com/ |
| FormGrid: 12 Best Customer Feedback Survey Tools 2026 | NPS/CSAT/购后调研/开放式反馈标准格式 | https://www.formgrid.com/blog/customer-feedback-survey-tools |
| Visme: Best Interactive Content Platforms 2026 | 互动内容平台对比：Outgrow/Involve.me/SurveyMonkey/Interacty/Genially | https://visme.co/blog/interactive-content-platforms/ |
| ClearlyRated: Customer Experience Survey Tools 2026 | CX调研工具对比：功能/价格/平台 | https://www.clearlyrated.ai/blog/customer-experience-survey-tools-to-use-in-2026 |
| InfluenceFlow: Customer Feedback Integration Strategies 2026 | 反馈集成策略：反馈→产品改进→客户体验闭环 | https://influenceflow.io/resources/customer-feedback-integration-strategies-build-better-products-in-2026/ |

---

## 现状盘点

| 功能 | 实现程度 | 核心现有代码 | 差距 |
|------|----------|-------------|------|
| 触达后效果追踪 | **部分** | EventLog: 触达/打开/点击事件记录 | 仅行为指标，无满意度/主观反馈 |
| 用户数据画像 | **部分** | CdpUserController: 用户详情+洞察 | 只有行为数据，无反馈/情感数据 |
| NPS调研 | **不存在** | — | 无NPS(净推荐值)调研创建和发送 |
| CSAT调研 | **不存在** | — | 无CSAT(客户满意度)调研 |
| 自定义调研问卷 | **不存在** | — | 无自定义问卷/表单/投票创建 |
| 反馈触发自动化 | **不存在** | — | 无法在画布中根据用户行为触发调研 |
| 反馈驱动个性化 | **不存在** | — | 无法根据NPS/CSAT结果动态调整体验 |
| 反馈分析 | **不存在** | — | 无情感分析/NPS趋势/词云/分群对比 |
| 闭环恢复 | **不存在** | — | Detractor→自动触达恢复流程(客服/补偿/跟进) |
| 客户之声(VoC)中心 | **不存在** | — | 无集中反馈管理+分析+洞察平台 |

### 关键洞察

CoreMedia反馈驱动个性化的三层架构：
```
反馈采集 → 画像融合 → 实时行动
   ↓           ↓           ↓
NPS=3     用户画像     实时弹窗：
CSAT=1   打入标签     "抱歉未能满足
问卷答案  "Detractor"  您的期望..."
           ↓              ↓
        分群管理      触发恢复画布
                      (优惠券/客服/跟进)
```

Canvas画布中反馈的角色定位：
- **作为触发器**：用户提交NPS/CSAT→触发恢复或激活画布
- **作为条件节点**：NPS Score > 8 → 邀请推荐分支；NPS Score < 6 → 恢复分支
- **作为数据源**：反馈数据→写入用户画像→丰富分群条件

---

## 功能清单

### P0 — 调研创建与反馈采集

---

#### 1. 调研问卷构建器 [中复杂度 | 1.5人月]

**现状**：零调研创建

**需补齐**：

| 子功能 | 描述 | 参考出处 |
|--------|------|---------|
| 模板库 | NPS/CSAT/CES/购后调研/注册调研/流失调研预设模板 | SurveyMonkey: 500+ templates |
| 题型支持 | NPS评分(0-10)/CSAT(1-5星)/多选/单选/文本/NPS Follow-up/矩阵/排序 | SurveyMonkey: 25+ question types |
| 分页与分支逻辑 | 根据答案跳转不同问题(if NPS≤6 → "请告诉我们原因") | SurveyMonkey/GenZform: branching logic |
| 品牌定制 | 问卷品牌化(Logo/颜色/字体)+自定义Thank You页 | CoreMedia: on-brand surveys |
| AI生成调研 | 自然语言描述调研目的→AI自动生成完整问卷 | GenZform: AI builds from description |
| 多语言问卷 | 同一问卷多语言版本+自动适配用户语言 | ㉛i18n联动 |
| 预览与测试 | 发布前预览+测试数据+评分计算验证 | SurveyMonkey: preview mode |

**问卷DDL**：

```sql
CREATE TABLE survey (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    survey_key VARCHAR(50) NOT NULL,
    name VARCHAR(200) NOT NULL COMMENT '问卷名称',
    survey_type VARCHAR(20) NOT NULL COMMENT 'NPS/CSAT/CES/CUSTOM/POST_PURCHASE/ONBOARDING/CHURN',
    description TEXT,
    pages JSON NOT NULL COMMENT '问卷页面和问题定义',
    branding JSON COMMENT '品牌定制(logo/colors/fonts/thank_you_page)',
    settings JSON COMMENT '设置(anonymous/required_login/multiple_submissions/language)',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PUBLISHED/CLOSED/ARCHIVED',
    total_responses INT NOT NULL DEFAULT 0,
    avg_completion_seconds INT COMMENT '平均完成时间',
    completion_rate DECIMAL(5,2) COMMENT '完成率',
    created_by VARCHAR(64),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_key (tenant_id, survey_key),
    INDEX idx_type (survey_type),
    INDEX idx_tenant (tenant_id)
) COMMENT '调研问卷';

CREATE TABLE survey_response (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    survey_id BIGINT NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    answers JSON NOT NULL COMMENT '答案({question_id: answer})',
    nps_score INT COMMENT 'NPS评分(0-10)',
    csat_score INT COMMENT 'CSAT评分(1-5)',
    ces_score INT COMMENT 'CES评分(1-7)',
    sentiment VARCHAR(10) COMMENT 'POSITIVE/NEUTRAL/NEGATIVE(AI分析)',
    keywords JSON COMMENT 'AI提取的关键词',
    completed TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否完成全部问题',
    completion_seconds INT COMMENT '填写耗时',
    source VARCHAR(20) COMMENT 'EMAIL/SMS/INAPP/WEB/API',
    source_canvas_id BIGINT COMMENT '触发的画布ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_survey (survey_id),
    INDEX idx_user (user_id),
    INDEX idx_nps (nps_score),
    INDEX idx_created (created_at)
) COMMENT '调研问卷回复';
```

---

#### 2. 反馈触发与采集引擎 [中复杂度 | 1.5人月]

**现状**：无反馈采集

**需补齐**：

| 子功能 | 描述 | 参考出处 |
|--------|------|---------|
| 事件触发调研 | 画布中添加SEND_SURVEY节点→在特定时刻自动发送调研 | CoreMedia: auto-trigger post-purchase |
| 多渠道分发 | Email(嵌入/链接)/SMS(短链接)/InApp(弹窗)/Web(嵌入式)/API | CoreMedia: omnichannel collection |
| 时机智能 | AI推荐最佳发送时机(购后24h→CSAT; 使用30天后→NPS) | CoreMedia: timing increases accuracy |
| 频率控制 | 同一用户不重复收到同一问卷(冷却期可配) | 体验保护 |
| 渐进式调研 | 先问NPS评分→根据分数追问原因→减少用户负担 | 微调研(Micro-survey) |
| 匿名/实名 | 支持匿名反馈+可选实名(留联系方式跟进) | 灵活性 |

**画布节点扩展示例**：

```
[SEND_EMAIL: 订单确认邮件+满意度入口]
      ↓
[WAIT: 24h]
      ↓
[SEND_SURVEY: CSAT购后满意度调研]
      ↓
[WAIT: 7d]
      ↓
[SEND_SURVEY: NPS净推荐值调研]
      ↓
[CONDITION: NPS Score?]
   ├─ ≥9 → [SEND_EMAIL: 邀请评价+推荐奖励]
   ├─ 7-8 → [END: 标准体验]
   └─ ≤6 → [SEND_EMAIL: 客服跟进+优惠券补偿]
               ↓
           [WAIT: 48h]
               ↓
           [CONDITION: 客户是否回复?]
              ├─ YES → [标签: Recovered Detractor]
              └─ NO  → [标签: At Risk Churn] → [加入流失预警旅程]
```

---

### P1 — 反馈分析与闭环恢复

---

#### 3. NPS/CSAT分析仪表盘 [中复杂度 | 1.0人月]

**现状**：零反馈分析

**需补齐**：

| 子功能 | 描述 |
|--------|------|
| NPS趋势 | 总体NPS趋势+按时间(周/月/季度)+按分群(地区/产品/会员等级) |
| CSAT趋势 | CSAT趋势+按触点(购买/客服/产品/物流) |
| 情感分析 | AI自动分析开放式反馈→正面/中性/负面+关键词提取 |
| 词云与主题 | 反馈关键词词云+主题聚类(价格/质量/服务/体验) |
| 分群对比 | Detractor vs Promoter vs Passive的行为差异分析 |
| 驱动因素分析 | 哪些因素(产品/价格/服务/物流)最影响NPS/CSAT |
| 预警仪表盘 | Detractor数量激增→自动告警+趋势预测 |

---

#### 4. 闭环恢复工作流 [中复杂度 | 1.0人月]

**现状**：无闭环恢复

**需补齐**：

| 子功能 | 描述 | 参考出处 |
|--------|------|---------|
| Detractor自动恢复 | NPS≤6→自动触发恢复画布(客服联系+优惠券+问题跟进) | CoreMedia: Recover Detractors |
| Promoter激活 | NPS≥9→自动触发推荐奖励+评价邀请+社交分享 | CoreMedia: Activate Promoters |
| 低CSAT恢复 | CSAT≤2→即时弹窗："抱歉，需要客服吗？"+实时客服路由 | CoreMedia: support redirect |
| 跟进闭环追踪 | 恢复动作→跟进→问题解决→重新调研→闭环验证 | CoreMedia: close the loop |
| 恢复效果分析 | 恢复成功率+恢复后NPS变化+恢复周期+成本分析 | 恢复ROI |

---

### P2 — 客户之声(VoC)中心

---

#### 5. 客户之声(VoC)中心 [低复杂度 | 0.8人月]

**描述**：集中反馈管理+洞察+行动

| 子功能 | 描述 |
|--------|------|
| 反馈统一视图 | 所有渠道反馈汇总(调研+评论+客服+社交)统一视图 |
| 反馈分类标签 | 自动/手动标签(产品建议/Bug报告/服务投诉/功能需求) |
| 反馈→需求 | 高价值反馈→自动创建产品需求工单(对接飞书/项目管理系统) |
| 反馈影响力评分 | AI评估每条反馈的影响力(用户价值×提及频率×可行性) |
| VoC报表 | 月度/季度VoC报告+Top反馈+Top建议+改进进度 |
| 客户反馈画像 | 客户反馈历史+情感曲线+关键事件时间线→写入用户画像 |

---

#### 6. 互动式调研体验 [低复杂度 | 0.5人月]

**描述**：互动计算器+测验+评估（提升调研趣味性和完成率）

| 子功能 | 描述 | 参考出处 |
|--------|------|---------|
| 互动计算器 | ROI计算器/节省估算器/定价对比→用户输入→即时个性化结果 | Outgrow: interactive calculators |
| 产品推荐测验 | "哪个产品适合你？"→互动问答→AI推荐→直接转化 | Outgrow: product recommendation |
| 评估工具 | 成熟度评估/健康度检查→即时得分→个性化建议 | GenZform: branching assessments |
| 游戏化调研 | 进度条/即时反馈/得分排行榜/徽章 | Interacty: gamified surveys |
| 对话式调研 | 聊天式一问一答→替代传统表单→更高完成率 | Outgrow: conversational chatbots |

---

## 工作量估算

| 优先级 | 功能 | 后端人月 | 前端人月 | AI/ML人月 | 测试人月 | 总计 |
|--------|------|---------|---------|----------|---------|------|
| P0 | 调研问卷构建器 | 0.7 | 0.5 | 0.3 | 0.2 | 1.7 |
| P0 | 反馈触发与采集引擎 | 0.8 | 0.5 | 0.2 | 0.2 | 1.7 |
| P1 | NPS/CSAT分析仪表盘 | 0.5 | 0.3 | 0.2 | 0.1 | 1.1 |
| P1 | 闭环恢复工作流 | 0.7 | 0.2 | 0.1 | 0.1 | 1.1 |
| P2 | 客户之声中心 | 0.5 | 0.2 | 0.1 | 0.1 | 0.9 |
| P2 | 互动式调研体验 | 0.3 | 0.2 | 0.0 | 0.1 | 0.6 |
| | **合计** | **3.5** | **1.9** | **0.9** | **0.8** | **7.1** |

---

## 执行顺序

```
Sprint 1 (P0-Builder): 调研问卷构建器 — 1.7人月
  → 产出：NPS/CSAT/自定义模板+25+题型+分支逻辑+AI生成+品牌定制

Sprint 2 (P0-Collect): 反馈触发与采集引擎 — 1.7人月
  → 产出：SEND_SURVEY节点+多渠道分发+时机智能+频率控制+闭环画布

Sprint 3 (P1-Analysis): NPS/CSAT分析+闭环恢复 — 2.2人月
  → 产出：NPS/CSAT仪表盘+情感分析+Detractor恢复工作流+Promoter激活

Sprint 4 (P2-VoC): VoC中心+互动调研 — 1.5人月
  → 产出：统一反馈视图+反馈→需求+影响力评分+互动计算器+产品推荐测验
```

---

## 风险评估

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| 调研疲劳 | 用户收到太多调研→无视/反感→完成率低 | 全局频率控制+微调研(1-2题)+时机智能+进度保存 |
| 反馈偏差 | 只有极端用户愿意填→数据不具代表性 | 随机抽样+多触点分布+权重校正+匿名选项 |
| 闭环不力 | 收到反馈但不行动→客户二次失望→更差体验 | SLA自动追责+闭环追踪+恢复效果上报+管理看板 |
| 隐私合规 | 调研回复含个人敏感信息 | 匿名选项+数据脱敏+合规声明+GDPR/CCPA合规 |
| 情感分析不准 | AI中文情感分析准确率不足→错误分类 | 人工校验机制+持续训练+多模型融合+置信度过滤 |

---

## 与其他方向的关系

| 方向 | 与㊱的关系 |
|------|----------|
| ① 营销自动化深度 | SEND_SURVEY作为新节点类型+反馈触发画布(Detractor→恢复/ Promoter→推荐) |
| ⑨ 营销数据中台 | NPS/CSAT数据纳入全渠道指标+反馈驱动归因 |
| ⑬ 实时用户画像引擎 | 反馈数据写入用户画像(NPS Score/情感/关键词标签) |
| ⑭ A/B测试平台 | A/B测试不同调研时机/渠道/题型对完成率的影响 |
| ㉗ 偏好与同意管理 | 调研发送需检查Consent状态(是否接受调研类消息) |
| ㉞ 个性化引擎 | 反馈驱动个性化：NPS≤6→降级体验(不推促销)+恢复体验；NPS≥9→激活体验 |
| ㉑ 优惠券与促销引擎 | Detractor恢复: 自动发放优惠券补偿 |
| ㉔ 客户生命周期智能 | 反馈数据是流失预警的重要输入特征(NPS趋势下降→流失风险升高) |
| ⑮ 营销资源中心 | 调研模板+Thank You页面+奖励素材管理 |
