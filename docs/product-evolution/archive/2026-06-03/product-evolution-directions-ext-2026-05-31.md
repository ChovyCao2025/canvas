# 产品演进方向扩展（11-15）（2026-05-31）

> 方向11-15：生态合作/客户旅程/运营知识/体验设计/数据驱动
> 原则：**全做优先 → 配置项 → 脑暴选最优**

---

## 总览

| # | 演进方向 | 已有能力 | 缺失能力 | 配置项数 | 阶段 |
|---|----------|----------|----------|----------|------|
| 11 | 生态与合作伙伴 | SpringDoc+API定义+30+模板 | ISV门户/渠道伙伴/数据伙伴/社区 | 7 | 2-4 |
| 12 | 客户旅程全链路 | lifecycleStage+模板+克隆 | 新手引导/成功体系/扩展推荐/续费/口碑 | 8 | 1-4 |
| 13 | 运营知识体系 | 30+模板(11行业×50场景)+SpringDoc | 最佳实践库/运营手册/效果基准/FAQ/案例 | 5 | 2-3 |
| 14 | 产品体验设计 | antd+通知系统+部分空状态 | 设计系统/ErrorBoundary/引导/无障碍/动效 | 6 | 0-3 |
| 15 | 数据驱动迭代 | 事件追踪+A/B实验+Metrics+KPI看板 | 行为分析/功能追踪/反馈闭环/北极星 | 6 | 1-3 |

---

## 方向11：生态与合作伙伴战略

### 代码扫描现状

| 能力 | 实现位置 | 状态 |
|------|----------|------|
| API文档 | SpringDoc + api-docs前端页 | ✅ 完整 |
| API定义管理 | ApiDefinitionController (CRUD+限流+SSRF) | ✅ 完整 |
| 官方模板 | CanvasTemplateDO + V55(30+模板/11行业) | ✅ 完整 |
| 事件签名 | EventReportAuthService (HMAC-SHA256) | ✅ 完整 |
| 画布克隆 | CanvasOpsService.clone() | ✅ 完整 |
| ISV门户 | — | ❌ 无 |
| 渠道伙伴管理 | — | ❌ 无 |
| 数据伙伴 | — | ❌ 无 |
| 开发者社区 | — | ❌ 无 |
| Webhook出站 | — | ❌ 无(仅入站回调) |

### 解决方案（全做）

#### 11.1 ISV生态（3层全做，可配置）

| 层级 | 说明 | 配置项 |
|------|------|--------|
| **认证ISV** | 审核准入+技术对接+分成 | `isv.tier=CERTIFIED` |
| **金牌ISV** | 深度合作+优先支持+联合营销 | `isv.tier=GOLD` |
| **战略ISV** | 联合产品+共享客户+战略分成 | `isv.tier=STRATEGIC` |

**技术方案**：
- 新增 `isv_partner` 表（partnerId/name/tier/contactEmail/techContact/businessContact/commissionRate/status）
- 新增 `isv_application` 表（applicationId/partnerId/name/description/appType/handlerClass/configSchema/screenshots/reviewStatus）
- 新增 `IsvPartnerService` — ISV准入+审核+分级
- 新增 `IsvApplicationService` — 应用提交+审核+上架
- 新增 `IsvPortalController` — ISV门户API
- 前端新增"ISV管理"后台页面 + "应用市场"用户页面
- 配置项：`isv.auto-approve`（自动审核）、`isv.commission-rate.{tier}`（按层级分佣）

#### 11.2 渠道伙伴（4伙伴全做，可配置）

| 伙伴 | 合作模式 | 配置项 |
|------|----------|--------|
| **企微** | 官方ISV+API对接+联合方案 | `partner.wecom.mode=OFFICIAL_ISV` |
| **抖音** | 开放平台+私信API+服务商 | `partner.douyin.mode=SERVICE_PROVIDER` |
| **小红书** | 开放平台+商业API | `partner.xiaohongshu.mode=API_PARTNER` |
| **快手** | 开放平台+私域API | `partner.kuaishou.mode=API_PARTNER` |

**技术方案**：
- 新增 `channel_partner_config` 表（partnerType/appId/appSecret/accessToken/refreshToken/tokenExpiresAt/callbackUrl/scopes）
- 新增 `ChannelPartnerTokenManager` — OAuth Token管理+自动刷新
- 每个渠道伙伴一个 `ChannelPartner{X}Service` — 封装API调用
- 配置项：`partner.{channel}.auto-refresh-token`（自动刷新Token）

#### 11.3 数据伙伴（3类全做，可配置）

| 伙伴类型 | 说明 | 配置项 |
|----------|------|--------|
| **CDP供应商** | 神策/GrowingIO/诸葛IO数据接入 | `data-partner.cdp.provider=SHENCE/GROWINGIO/ZHUGE` |
| **DMP供应商** | 数据管理平台接入 | `data-partner.dmp.enabled` |
| **第三方数据** | 征信/运营商/电商数据 | `data-partner.third-party.enabled` |

**技术方案**：
- 新增 `data_partner_config` 表（partnerType/provider/apiEndpoint/apiKey/syncMode/dataMappingJson）
- 新增 `DataPartnerAdapter` 接口 → ShenceAdapter/GrowingioAdapter/ZhugeAdapter
- 新增 `DataPartnerSyncService` — 统一数据同步
- 扩展 CdpAudienceSourceService → 新增CDP供应商数据源

#### 11.4 技术伙伴（4伙伴全做，可配置）

| 伙伴 | 合作模式 | 配置项 |
|------|----------|--------|
| **AI厂商** | OpenAI/通义/文心/智谱模型接入 | `tech-partner.ai.provider=OPENAI/TONGYI/WENXIN/ZHIPU` |
| **云厂商** | 阿里云/腾讯云/AWS基础设施 | `tech-partner.cloud.provider=ALIYUN/TENCENT/AWS` |
| **消息厂商** | 极光/个推/信鸽推送 | `tech-partner.push.provider=JPUSH/GETUI/XINGE` |
| **支付厂商** | 支付宝/微信/Stripe | `tech-partner.payment.provider=ALIPAY/WECHAT/STRIPE` |

**技术方案**：
- 复用方向4 AI Gateway → 多模型适配
- 复用方向8 多云部署 → 云适配器
- 新增 `PushProviderAdapter` 接口 → JPushAdapter/GetuiAdapter/XingeAdapter
- 扩展 ReachDeliveryService → 推送厂商路由

#### 11.5 开发者社区（4功能全做）

| 功能 | 说明 | 技术方案 |
|------|------|----------|
| **开发者门户** | API文档+SDK+示例+沙箱 | 扩展现有api-docs页 → 完整开发者门户 |
| **SDK** | Java/Python/Node.js/Go | 自动生成OpenAPI SDK |
| **示例代码** | 每个API的调用示例 | 扩展apiDocOverrides.ts → 完整示例库 |
| **社区论坛** | 问答+分享+投票 | 集成第三方（Discourse/Flarum）或自建 |

#### 11.6 Webhook出站（3事件类型全做，可配置）

| 事件类型 | 说明 | 配置项 |
|----------|------|--------|
| **执行事件** | 画布开始/完成/失败/节点完成 | `webhook.events=EXECUTION` |
| **业务事件** | 触达/点击/转化/归因 | `webhook.events=BUSINESS` |
| **系统事件** | 画布创建/更新/发布/下线 | `webhook.events=SYSTEM` |

**技术方案**：
- 新增 `webhook_subscription` 表（subscriptionId/tenantId/eventTypes/callbackUrl/secret/retryPolicy/maxRetries/active）
- 新增 `WebhookDispatcher` — 事件匹配+分发+重试
- 新增 `WebhookSignatureUtil` — HMAC-SHA256签名
- 新增 `WebhookDeliveryLog` 表 — 投递记录+响应码+重试次数
- 配置项：`webhook.max-retries`、`webhook.retry-interval-ms`、`webhook.timeout-ms`

---

## 方向12：客户旅程全链路

### 代码扫描现状

| 能力 | 实现位置 | 状态 |
|------|----------|------|
| 生命周期阶段 | CustomerProfileDO.lifecycleStage (NEW/ACTIVE/CHURN_RISK) | ✅ 3阶段 |
| 模板种子 | V55含新手引导/流失挽回/交叉销售模板 | ✅ 但无UI |
| 画布克隆 | CanvasOpsService.clone() | ✅ 完整 |
| 推荐节点 | RecommendationHandler | ⚠️ stub |
| AI下一步 | AiNextBestActionHandler | ⚠️ stub |
| 新手引导UI | — | ❌ 无 |
| 首次体验 | — | ❌ 无 |
| 功能发现 | — | ❌ 无 |
| 健康度评分 | — | ❌ 无(方向1已规划) |
| 扩展推荐 | — | ❌ 无 |

### 解决方案（全做）

#### 12.1 获客旅程（4阶段全做，可配置）

| 阶段 | 说明 | 关键动作 | 配置项 |
|------|------|----------|--------|
| **认知** | 用户了解产品 | 内容营销+SEO+广告 | `journey.acquisition.channel=CONTENT/SEO/ADS` |
| **试用** | 用户注册试用 | 免费试用+沙箱+Demo | `journey.trial.days=14` |
| **激活** | 用户首次成功使用 | 引导创建第一个画布 | `journey.activation.auto-guide=true` |
| **付费** | 用户转为付费 | 用量提醒+版本来推荐 | `journey.conversion.auto-prompt=true` |

**技术方案**：
- 新增 `user_journey_state` 表（userId/tenantId/stage/enteredAt/metadataJson）
- 新增 `UserJourneyService` — 旅程状态管理+阶段转换
- 新增 `UserJourneyEventPublisher` — 旅程事件发布
- 配置项：`journey.trial.days`（试用期天数）、`journey.activation.required-actions`（激活所需操作列表）

#### 12.2 成功旅程（3级全做，可配置）

| 级别 | 名称 | 标准 | 配置项 |
|------|------|------|--------|
| **L1: 新手** | 完成基础教程 | 创建1个画布+执行1次 | `success.level1.required=1_CANVAS_1_EXECUTION` |
| **L2: 熟练** | 掌握核心功能 | 5+画布+3+渠道+A/B测试 | `success.level2.required=5_CANVAS_3_CHANNEL_AB` |
| **L3: 专家** | 全功能精通 | 10+画布+AI+归因+API | `success.level3.required=10_CANVAS_AI_ATTRIBUTION` |

**技术方案**：
- 扩展 `user_journey_state` → 新增 `proficiencyLevel` 字段
- 新增 `ProficiencyTracker` — 跟踪用户行为，自动升级
- 新增 `AchievementService` — 成就系统（解锁徽章+功能）
- 前端新增"我的成长"页面
- 配置项：`success.gamification.enabled`（游戏化开关）

#### 12.3 扩展旅程（4触发全做，可配置）

| 触发 | 说明 | 配置项 |
|------|------|--------|
| **用量触发** | 接近套餐上限时推荐升级 | `expansion.trigger=USAGE_THRESHOLD` |
| **功能触发** | 使用基础版功能时展示专业版 | `expansion.trigger=FEATURE_DISCOVERY` |
| **效果触发** | 效果好时推荐更多功能 | `expansion.trigger=PERFORMANCE_MILESTONE` |
| **场景触发** | 发现新使用场景时推荐 | `expansion.trigger=SCENARIO_MATCH` |

**技术方案**：
- 新增 `expansion_opportunity` 表（opportunityId/tenantId/triggerType/triggerDetail/recommendedPlan/status）
- 新增 `ExpansionDetectionService` — 检测扩展机会
- 扩展 TenantService → 扩展推荐
- 配置项：`expansion.usage-threshold-percent`（用量阈值百分比，默认80%）

#### 12.4 续费旅程（4阶段全做，可配置）

| 阶段 | 时间点 | 动作 | 配置项 |
|------|--------|------|--------|
| **使用中** | 正常使用 | 健康度监控+增值服务推荐 | `renewal.health-monitor.enabled=true` |
| **到期前30天** | 提前提醒 | 续费优惠+新功能预告 | `renewal.remind-days-before=30` |
| **到期前7天** | 紧急提醒 | 限时优惠+客户成功介入 | `renewal.urgent-remind-days=7` |
| **到期后** | 挽留 | 数据保留+降级方案+人工跟进 | `renewal.grace-period-days=14` |

**技术方案**：
- 新增 `subscription` 表（subscriptionId/tenantId/planCode/startDate/endDate/status/autoRenew）
- 新增 `SubscriptionService` — 订阅生命周期管理
- 新增 `RenewalReminderJob` — 定时检查+提醒
- 配置项：`renewal.auto-downgrade`（到期自动降级）

#### 12.5 口碑旅程（3机制全做，可配置）

| 机制 | 说明 | 配置项 |
|------|------|--------|
| **推荐奖励** | 推荐新客户获奖励 | `referral.reward.type=DISCOUNT/CREDIT/FEATURE` |
| **案例共创** | 成功客户参与案例制作 | `referral.case-study.enabled` |
| **社区贡献** | 贡献模板/插件获积分 | `referral.community-reward.enabled` |

**技术方案**：
- 新增 `referral_code` 表（codeId/tenantId/code/rewardType/rewardValue/useCount/maxUses/validUntil）
- 新增 `ReferralService` — 推荐码生成+验证+奖励
- 新增 `referral_redemption` 表（redemptionId/codeId/newTenantId/redeemedAt）
- 前端新增"邀请有礼"页面

---

## 方向13：运营知识体系

### 代码扫描现状

| 能力 | 实现位置 | 状态 |
|------|----------|------|
| 官方模板 | CanvasTemplateDO + V55(30+模板) | ✅ 数据完整无UI |
| 模板分类 | companyType(11行业) + marketingScenario(50场景) + difficulty(3级) | ✅ 元数据丰富 |
| API文档 | SpringDoc + api-docs前端 | ✅ 完整 |
| 画布克隆 | CanvasOpsService.clone() | ✅ 完整 |
| 模板搜索/筛选API | — | ❌ 无 |
| 最佳实践库 | — | ❌ 无 |
| 运营手册 | — | ❌ 无 |
| 效果基准 | — | ❌ 无 |
| FAQ系统 | — | ❌ 无 |
| 案例库 | — | ❌ 无 |
| 模板市场UI | — | ❌ 无 |
| 评分/评价 | CanvasTemplateDO.useCount | ⚠️ 仅使用次数 |

### 解决方案（全做）

#### 13.1 模板市场（3角色全做，可配置）

| 角色 | 说明 | 配置项 |
|------|------|--------|
| **官方模板** | 平台出品，质量保证 | `template.source=OFFICIAL` |
| **行业模板** | 行业专家/ISV贡献 | `template.source=INDUSTRY` |
| **社区模板** | 用户共享，评分排序 | `template.source=COMMUNITY` |

**技术方案**：
- 新增 `CanvasTemplateController` — 模板CRUD+搜索+筛选API
  - `GET /canvas/templates` — 列表（支持companyType/marketingScenario/difficulty/keyword筛选）
  - `GET /canvas/templates/{key}` — 详情
  - `POST /canvas/templates/{key}/clone` — 从模板创建画布
  - `POST /canvas/templates/{key}/rate` — 评分
- 新增 `template_rating` 表（templateKey/userId/rating/review/createdAt）
- 新增 `TemplateSearchService` — 全文搜索+推荐
- 前端新增"模板市场"页面（分类浏览+搜索+评分+一键创建）
- 配置项：`template.community-submit.enabled`（社区提交开关）

#### 13.2 最佳实践库（3维度全做）

| 维度 | 说明 | 组织方式 |
|------|------|----------|
| **行业×场景** | 11行业×50场景=550个最佳实践 | 矩阵式组织 |
| **目标导向** | 拉新/促活/留存/转化/变现 | 按目标索引 |
| **难度递进** | 入门→进阶→复杂 | 分级学习路径 |

**技术方案**：
- 新增 `best_practice` 表（practiceId/industry/scenario/goal/difficulty/title/description/canvasTemplateKey/metricsJson/tipsJson）
- 新增 `BestPracticeService` — 最佳实践推荐
- 前端新增"最佳实践"页面（行业选择→场景推荐→一键创建）
- 配置项：`best-practice.auto-suggest.enabled`（编辑器中自动推荐）

#### 13.3 运营手册（5模块全做）

| 模块 | 内容 | 格式 |
|------|------|------|
| **功能手册** | 每个节点/功能的详细说明 | 图文+视频 |
| **场景手册** | 常见营销场景的操作指南 | 步骤式 |
| **合规手册** | 各行业合规要求和操作 | 检查清单 |
| **FAQ** | 常见问题+解决方案 | Q&A |
| **术语表** | 营销/技术术语解释 | 字典式 |

**技术方案**：
- 新增 `knowledge_article` 表（articleId/category/title/contentMd/tagsJson/sortOrder/publishedAt）
- 新增 `KnowledgeArticleController` — 文章CRUD+搜索
- 前端新增"帮助中心"页面（搜索+分类+推荐）
- 集成到编辑器：节点右键→"查看帮助"
- 配置项：`knowledge.auto-translate`（多语言自动翻译）

#### 13.4 效果基准（3维度全做，可配置）

| 维度 | 说明 | 数据源 | 配置项 |
|------|------|--------|--------|
| **行业基准** | 各行业平均效果指标 | 匿名化聚合数据 | `benchmark.industry.enabled` |
| **渠道基准** | 各渠道平均打开/点击/转化率 | 匿名化聚合数据 | `benchmark.channel.enabled` |
| **场景基准** | 各场景平均效果 | 匿名化聚合数据 | `benchmark.scenario.enabled` |

**技术方案**：
- 新增 `industry_benchmark` 表（industry/metric/period/p50/p75/p90/p99/sampleSize）
- 新增 `channel_benchmark` 表（channel/metric/period/p50/p75/p90/p99/sampleSize）
- 新增 `BenchmarkAggregationJob` — 定期匿名化聚合
- 新增 `BenchmarkService` — 基准查询+对比
- 扩展 CanvasStatsController → 对标行业/渠道基准
- 配置项：`benchmark.anonymization-threshold`（最小样本量，低于此不展示）

#### 13.5 案例库（4维度全做）

| 维度 | 说明 | 内容 |
|------|------|------|
| **客户案例** | 成功客户故事 | 背景+挑战+方案+效果+引言 |
| **效果案例** | 效果数据展示 | 前后对比+ROI计算 |
| **操作案例** | 操作步骤录屏 | 步骤拆解+关键配置 |
| **模板案例** | 模板使用案例 | 模板→定制→效果 |

**技术方案**：
- 新增 `case_study` 表（caseId/industry/scenario/customerName/anonymized/backgroundMd/challengeMd/solutionMd/resultsJson/quoteMd/templateKey）
- 新增 `CaseStudyController` — 案例CRUD
- 前端新增"成功案例"页面
- 配置项：`case-study.anonymize-by-default`（默认匿名化）

---

## 方向14：产品体验设计体系

### 代码扫描现状

| 能力 | 实现位置 | 状态 |
|------|----------|------|
| 组件库 | Antd 5 | ✅ 完整 |
| 通知系统 | NotificationBell + WebSocket + 4分类 | ✅ 完整 |
| 消息反馈 | message.success/error/warning/info | ✅ 全面 |
| 确认弹窗 | Modal.confirm + Popconfirm | ✅ 完整 |
| 部分空状态 | Empty组件(5处) + BranchPlaceholderNode | ⚠️ 不全面 |
| CSS过渡 | 多处transition(0.15-0.2s) | ⚠️ 仅基础 |
| 表单验证 | antd Form rules | ✅ 标准但不一致 |
| 设计系统 | — | ❌ 无 |
| ErrorBoundary | — | ❌ 无 |
| 404/500页面 | — | ❌ 无 |
| 新手引导 | — | ❌ 无 |
| 无障碍 | aria-label(少量) | ⚠️ 极简 |
| 动效系统 | — | ❌ 无 |

### 解决方案（全做）

#### 14.1 设计系统（4模块全做，可配置）

| 模块 | 说明 | 配置项 |
|------|------|--------|
| **Design Tokens** | 色/字/间距/圆角/阴影/动效token | `design.theme=LIGHT/DARK` |
| **组件库** | 基于antd的统一封装 | `design.component-version` |
| **图标库** | 统一图标体系 | `design.icon-pack=DEFAULT` |
| **动效库** | 统一过渡/动画/微交互 | `design.motion.enabled=true` |

**技术方案**：
- 新增 `src/design/` 目录结构：
  - `tokens/` — CSS自定义属性（--canvas-primary、--canvas-radius等）
  - `components/` — 统一封装的CanvasButton、CanvasCard、CanvasTable等
  - `icons/` — 统一图标SVG
  - `motion/` — framer-motion动画预设
- 扩展 `main.tsx` ConfigProvider → 自定义theme tokens
- 新增 `ThemeSwitcher` 组件 → 亮/暗模式切换
- 配置项：`design.compact-mode`（紧凑模式，表格/表单间距缩小）

#### 14.2 错误处理体系（5场景全做）

| 场景 | 当前 | 目标 |
|------|------|------|
| **React ErrorBoundary** | 无 | 全局+页面级+组件级3层 |
| **404页面** | 无路由兜底 | 品牌404页+推荐入口 |
| **403页面** | 纯文字 | 品牌403页+申请权限入口 |
| **500页面** | 无 | 品牌500页+重试+反馈 |
| **网络错误** | 部分处理 | 统一拦截+离线提示+重试 |

**技术方案**：
- 新增 `ErrorBoundary` 组件（3层：GlobalErrorBoundary > PageErrorBoundary > WidgetErrorBoundary）
- 新增 `NotFoundPage` — 404页面（推荐画布/模板入口）
- 新增 `ForbiddenPage` — 403页面（权限说明+申请入口）
- 新增 `ServerErrorPage` — 500页面（重试+反馈表单）
- 扩展 App.tsx 路由 → 新增 `*` 通配路由 → 404
- 扩展 api.ts Axios拦截器 → 统一401/403/500处理

#### 14.3 空状态体系（4类型全做，可配置）

| 类型 | 说明 | 适用场景 | 配置项 |
|------|------|----------|--------|
| **首次空** | 引导创建+CTA按钮 | 新用户无画布/模板 | `empty-state.type=FIRST_TIME` |
| **无数据空** | 说明+筛选重置 | 搜索无结果/筛选无匹配 | `empty-state.type=NO_DATA` |
| **错误空** | 错误说明+重试按钮 | 加载失败/接口报错 | `empty-state.type=ERROR` |
| **权限空** | 权限说明+申请入口 | 无权限访问 | `empty-state.type=NO_PERMISSION` |

**技术方案**：
- 新增 `EmptyState` 组件（type/illustration/title/description/action）
- 新增 `empty-illustrations/` — 4类空状态插画
- 替换所有现有 `Empty` 和 `暂无xxx` 为统一组件
- 配置项：`empty-state.illustration-style`（插画风格）

#### 14.4 引导体系（3级全做，可配置）

| 级别 | 说明 | 技术方案 | 配置项 |
|------|------|----------|--------|
| **L1: 全局引导** | 首次使用的产品Tour | antd Tour组件 | `guide.level=GLOBAL` |
| **L2: 功能引导** | 新功能发布时的Tooltip | Feature Flag+Tooltip | `guide.level=FEATURE` |
| **L3: 操作引导** | 具体操作的步骤指引 | antd Tour+步骤高亮 | `guide.level=STEP` |

**技术方案**：
- 新增 `guide_config` 表（guideKey/pagePath/targetSelector/title/description/placement/dismissed/required）
- 新增 `GuideService` — 引导配置+状态管理
- 新增 `GuideController` — 引导API
- 前端新增 `ProductGuide` 组件 — antd Tour封装
- 配置项：`guide.auto-trigger`（自动触发）、`guide.dismissible`（可关闭）、`guide.required`（必须完成）

#### 14.5 无障碍（3级全做，可配置）

| 级别 | 标准 | 要求 | 配置项 |
|------|------|------|--------|
| **A** | WCAG 2.1 A | 基本可访问性 | `a11y.level=A` |
| **AA** | WCAG 2.1 AA | 主流标准 | `a11y.level=AA` |
| **AAA** | WCAG 2.1 AAA | 最高标准 | `a11y.level=AAA` |

**AA级实施方案**（推荐默认）：
- 键盘导航：所有交互元素Tab可访问+焦点可见
- 颜色对比度：4.5:1（普通文本）/ 3:1（大文本）
- 屏幕阅读器：aria-label/aria-describedby/role完善
- 表单关联：label+errorMessage关联
- 跳转链接："跳到主内容"
- 减少动效：`prefers-reduced-motion` 媒体查询
- 配置项：`a11y.focus-indicator-style`（焦点指示器样式）、`a11y.announcements`（实时区域公告）

#### 14.6 表单验证体系（3层全做，可配置）

| 层级 | 说明 | 配置项 |
|------|------|--------|
| **字段级** | 即时验证+错误提示 | `validation.mode=FIELD` |
| **表单级** | 提交时全量验证 | `validation.mode=FORM` |
| **业务级** | 跨字段/服务端验证 | `validation.mode=BUSINESS` |

**技术方案**：
- 新增 `ValidationRules` — 统一验证规则库（手机号/邮箱/URL/正整数/范围等）
- 新增 `FormErrorMessage` 组件 — 统一错误消息样式
- 新增 `useFormValidation` hook — 统一表单验证逻辑
- 服务端：扩展 ErrorCode → 前端友好的验证错误消息
- 配置项：`validation.real-time`（实时验证开关）

---

## 方向15：数据驱动的产品迭代

### 代码扫描现状

| 能力 | 实现位置 | 状态 |
|------|----------|------|
| 事件追踪 | EventDefinitionController + EventLogDO + TrackEventHandler | ✅ 完整 |
| 事件触发 | EventTriggerHandler + TriggerRouteService | ✅ 完整 |
| A/B实验 | AbExperimentController + AbSplitHandler + ExperimentHandler | ✅ 完整 |
| 执行指标 | CanvasMetrics (Micrometer) | ✅ 完整 |
| 执行统计 | CanvasExecutionStatsDO (日聚合) | ✅ 完整 |
| KPI看板 | HomeOverviewController + CanvasStatsController | ✅ 完整 |
| 漏斗分析 | CanvasStatsController.funnel() | ✅ 完整 |
| 通知系统 | NotificationEventService + NotificationBell | ✅ 完整 |
| 行为分析 | — | ❌ 无 |
| 功能使用追踪 | — | ❌ 无(只有画布级统计) |
| 反馈收集 | — | ❌ 无 |
| 北极星指标 | — | ❌ 无 |
| 产品A/B测试 | — | ❌ 无(只有画布内A/B) |
| 告警规则 | — | ❌ 无(只有事件通知) |

### 解决方案（全做）

#### 15.1 功能使用追踪（3层全做，可配置）

| 层级 | 说明 | 采集方式 | 配置项 |
|------|------|----------|--------|
| **页面级** | 页面PV/UV/停留时长 | 前端SDK | `tracking.level=PAGE` |
| **功能级** | 按钮/功能使用频率 | 前端埋点 | `tracking.level=FEATURE` |
| **操作级** | 操作路径+漏斗 | 前端+后端联合 | `tracking.level=ACTION` |

**技术方案**：
- 新增前端 `AnalyticsSDK` — 统一埋点SDK
  - `trackPageView(page)` — 页面浏览
  - `trackFeature(feature, action)` — 功能使用
  - `trackAction(action, target, context)` — 操作事件
- 新增 `analytics_event` 表（eventId/tenantId/userId/sessionId/page/feature/action/contextJson/timestamp）
- 新增 `AnalyticsService` — 事件采集+聚合+查询
- 新增 `AnalyticsController` — 查询API
- 配置项：`tracking.sampling-rate`（采样率0.01-1.0）、`tracking.batch-size`（批量上传大小）

#### 15.2 用户行为分析（4分析全做，可配置）

| 分析类型 | 说明 | 输出 | 配置项 |
|----------|------|------|--------|
| **路径分析** | 用户操作路径 | 桑基图 | `analytics.path.enabled` |
| **漏斗分析** | 转化漏斗 | 转化率+流失点 | `analytics.funnel.enabled` |
| **留存分析** | 功能/页面留存 | 留存曲线 | `analytics.retention.enabled` |
| **热力分析** | 点击/关注热区 | 热力图 | `analytics.heatmap.enabled` |

**技术方案**：
- 新增 `AnalyticsQueryService` — 统一分析查询
- 新增路径分析：基于analytics_event的sessionId排序 → 路径聚合
- 新增漏斗分析：定义步骤序列 → 计算每步转化率
- 新增留存分析：首日+次日+7日+30日留存率
- 前端新增"行为分析"页面（recharts桑基图+漏斗图+留存曲线）
- 配置项：`analytics.retention-cohorts`（留存队列定义）

#### 15.3 产品A/B测试（3类型全做，可配置）

| 类型 | 说明 | 配置项 |
|------|------|--------|
| **UI测试** | 按钮位置/文案/颜色 | `product-ab.type=UI` |
| **功能测试** | 功能开关/默认值 | `product-ab.type=FEATURE` |
| **策略测试** | 推荐算法/排序策略 | `product-ab.type=STRATEGY` |

**技术方案**：
- 新增 `feature_flag` 表（flagKey/name/description/type/ruleJson/variantsJson/enabled）
- 新增 `FeatureFlagService` — 特性标志管理+分流
- 新增 `FeatureFlagController` — CRUD API
- 分流策略：用户ID哈希/百分比/标签匹配
- 前端：`useFeatureFlag(flagKey)` hook
- 后端：`@FeatureFlagged("flagKey")` 注解
- 配置项：`product-ab.default-traffic-percent`（默认流量百分比）

#### 15.4 反馈闭环（4渠道全做，可配置）

| 渠道 | 说明 | 配置项 |
|------|------|--------|
| **应用内反馈** | 评分+文字+截图 | `feedback.in-app.enabled` |
| **NPS调查** | 净推荐值定期调查 | `feedback.nps.enabled` |
| **满意度评分** | 功能/页面级满意度 | `feedback.csat.enabled` |
| **功能请求** | 用户投票功能需求 | `feedback.feature-request.enabled` |

**技术方案**：
- 新增 `user_feedback` 表（feedbackId/tenantId/userId/type/page/rating/comment/screenshot/status）
- 新增 `nps_survey` 表（surveyId/tenantId/score/reason/triggerType/triggerDetail）
- 新增 `feature_request` 表（requestId/title/description/category/upvotes/status/tenantVotesJson）
- 新增 `FeedbackService` — 反馈收集+分析
- 前端新增"反馈"浮动按钮+反馈弹窗
- 配置项：`feedback.nps.trigger`（NPS触发条件）、`feedback.nps.cool-down-days`（冷却期）

#### 15.5 北极星指标体系（3层全做，可配置）

| 层级 | 指标 | 说明 | 配置项 |
|------|------|------|--------|
| **北极星** | 活跃画布执行成功率 | 平台核心价值度量 | `northstar.metric=EXECUTION_SUCCESS_RATE` |
| **一级指标** | 5个 | 触达用户数/画布发布数/渠道使用数/模板使用率/续费率 | `northstar.l1-metrics` |
| **二级指标** | 15+个 | 各功能使用率/转化率/响应时间/满意度 | `northstar.l2-metrics` |

**技术方案**：
- 新增 `northstar_metric_definition` 表（metricId/level/name/formula/dataSource/refreshCron/targetValue/thresholdsJson）
- 新增 `NorthstarService` — 指标计算+监控+告警
- 新增 `NorthstarDashboard` — 前端仪表盘（指标卡片+趋势+目标线）
- 配置项：`northstar.alert-threshold`（告警阈值）、`northstar.compare-period`（对比周期）

#### 15.6 告警规则引擎（3规则全做，可配置）

| 规则类型 | 说明 | 配置项 |
|----------|------|--------|
| **阈值告警** | 指标超过阈值 | `alert.type=THRESHOLD` |
| **趋势告警** | 指标趋势异常 | `alert.type=TREND` |
| **异常检测** | AI异常检测 | `alert.type=ANOMALY` |

**技术方案**：
- 新增 `alert_rule` 表（ruleId/name/metricId/type/conditionJson/actionsJson/enabled）
- 新增 `AlertRuleEngine` — 规则评估+动作执行
- 新增 `AlertAction` 接口 → NotificationAction/EmailAction/WebhookAction
- 扩展 NotificationEventService → 支持告警通知
- 配置项：`alert.evaluation-interval-seconds`（评估间隔）、`alert.cooldown-minutes`（冷却期）

---

## 配置项总表（方向11-15）

| 方向 | 配置项 | 选项 | 默认值 |
|------|--------|------|--------|
| 11-生态 | isv.tier | CERTIFIED/GOLD/STRATEGIC | CERTIFIED |
| 11-生态 | isv.auto-approve | true/false | false |
| 11-生态 | isv.commission-rate.certified | 0.0-1.0 | 0.2 |
| 11-生态 | isv.commission-rate.gold | 0.0-1.0 | 0.3 |
| 11-生态 | isv.commission-rate.strategic | 0.0-1.0 | 0.4 |
| 11-生态 | partner.wecom.mode | OFFICIAL_ISV/SERVICE_PROVIDER | OFFICIAL_ISV |
| 11-生态 | data-partner.cdp.provider | SHENCE/GROWINGIO/ZHUGE | — |
| 11-生态 | webhook.max-retries | 1-10 | 3 |
| 12-旅程 | journey.trial.days | 7-90 | 14 |
| 12-旅程 | journey.activation.auto-guide | true/false | true |
| 12-旅程 | success.gamification.enabled | true/false | false |
| 12-旅程 | expansion.trigger | USAGE_THRESHOLD/FEATURE_DISCOVERY/PERFORMANCE_MILESTONE/SCENARIO_MATCH | USAGE_THRESHOLD |
| 12-旅程 | expansion.usage-threshold-percent | 50-100 | 80 |
| 12-旅程 | renewal.remind-days-before | 7-60 | 30 |
| 12-旅程 | renewal.auto-downgrade | true/false | false |
| 12-旅程 | referral.reward.type | DISCOUNT/CREDIT/FEATURE | CREDIT |
| 13-知识 | template.source | OFFICIAL/INDUSTRY/COMMUNITY | ALL |
| 13-知识 | template.community-submit.enabled | true/false | false |
| 13-知识 | best-practice.auto-suggest.enabled | true/false | true |
| 13-知识 | benchmark.industry.enabled | true/false | true |
| 13-知识 | benchmark.anonymization-threshold | 5-100 | 10 |
| 14-体验 | design.theme | LIGHT/DARK | LIGHT |
| 14-体验 | design.compact-mode | true/false | false |
| 14-体验 | empty-state.type | FIRST_TIME/NO_DATA/ERROR/NO_PERMISSION | NO_DATA |
| 14-体验 | guide.level | GLOBAL/FEATURE/STEP | GLOBAL |
| 14-体验 | a11y.level | A/AA/AAA | AA |
| 14-体验 | validation.real-time | true/false | true |
| 15-数据 | tracking.level | PAGE/FEATURE/ACTION | FEATURE |
| 15-数据 | tracking.sampling-rate | 0.01-1.0 | 1.0 |
| 15-数据 | analytics.path.enabled | true/false | true |
| 15-数据 | product-ab.default-traffic-percent | 1-100 | 50 |
| 15-数据 | feedback.in-app.enabled | true/false | true |
| 15-数据 | feedback.nps.enabled | true/false | true |
| 15-数据 | feedback.nps.cool-down-days | 7-90 | 30 |
| 15-数据 | northstar.metric | EXECUTION_SUCCESS_RATE/REACH_USERS/… | EXECUTION_SUCCESS_RATE |
| 15-数据 | alert.evaluation-interval-seconds | 30-3600 | 300 |

**方向11-15新增：32个配置项**

---

## 方向1-15配置项汇总

方向1-10：61个配置项 + 方向11-15：32个配置项 = **93个配置项**

---

## 阶段映射（方向11-15）

| 阶段 | 时间 | 方向11-15重点 |
|------|------|-------------|
| **阶段0** | 1-2月 | 方向14(ErrorBoundary/404/403/空状态/表单验证) |
| **阶段1** | 2-4月 | 方向12(获客/新手引导)+方向15(功能追踪+反馈)+方向11(Webhook出站) |
| **阶段2** | 4-7月 | 方向11(ISV+渠道伙伴)+方向13(模板市场+最佳实践)+方向15(行为分析+A/B) |
| **阶段3** | 7-10月 | 方向12(扩展+续费+口碑)+方向13(效果基准+案例库)+方向14(设计系统+无障碍) |
| **阶段4** | 10-12月 | 方向11(数据伙伴+社区)+方向12(客户成功)+方向15(北极星+告警引擎) |

---

## 依赖关系（方向11-15）

```
方向11(生态) ──依赖──→ 方向2(插件体系) ──依赖──→ 方向6(行业化)
    │
    └──依赖──→ 方向12(客户旅程) ──依赖──→ 方向1(商业模式)

方向13(知识) ──依赖──→ 方向11(生态) ──依赖──→ 方向12(旅程)

方向14(体验) ──依赖──→ 方向2(平台化) ──依赖──→ 方向10(国际化)

方向15(数据驱动) ──依赖──→ 方向3(数据资产) ──依赖──→ 方向4(智能化)
    │
    └──依赖──→ 方向12(客户旅程)
```

---

*制定人：John (PM Agent) | 日期：2026-05-31 | 代码扫描+脑暴决策 | 93个配置项 | 15大演进方向全覆盖*
