# 产品演进方向全景（2026-05-31）

> 10大演进方向深度方案，基于代码扫描+竞品对标+脑暴决策
> 原则：**全做优先 → 配置项 → 脑暴选最优**

---

## 总览

| # | 演进方向 | 已有能力 | 缺失能力 | 配置项数 | 阶段 |
|---|----------|----------|----------|----------|------|
| 1 | 商业模式 | 配额+统计+租户套餐 | 计费引擎/支付/分成 | 8 | 2-4 |
| 2 | 平台化 | Handler注册+Groovy+Schema驱动 | 插件体系/开放API/低代码 | 6 | 2-4 |
| 3 | 数据资产 | CDP+标签+人群+画像+规则引擎 | 数据治理/数据变现/ETL | 5 | 2-4 |
| 4 | 智能化 | 3个AI节点(stub)+评分+实验 | AI Gateway/Agent/预测/NLP | 7 | 1-4 |
| 5 | 渠道生态 | 5渠道+优先级+可用性检查 | 企微/私域/RCS/渠道编排 | 9 | 1-4 |
| 6 | 行业化 | 积分+会员+合规节点(通用) | 行业模板/行业节点/行业合规 | 6 | 3-4 |
| 7 | 运营模式 | 审批节点+告警+执行统计 | 代运营/咨询/培训/客户成功 | 4 | 3-4 |
| 8 | 技术架构 | 虚拟线程+Reactor+车道+Disruptor | 微服务/Serverless/多云/边缘 | 5 | 3-4 |
| 9 | 安全合规 | JWT+RBAC+同意+抑制+脱敏+审计表 | 删除权/导出/隐私计算/等保 | 6 | 1-4 |
| 10 | 国际化 | 时区(Asia/Shanghai)+region字段 | i18n框架/多语言/多货币 | 5 | 3-4 |

---

## 方向1：商业模式演进

### 代码扫描现状

| 能力 | 实现位置 | 状态 |
|------|----------|------|
| 配额执行 | TriggerPreCheckService (Redis Lua原子消费) | ✅ 完整 |
| 配额数据 | CanvasUserQuotaDO + CanvasDO (5种限制) | ✅ 完整 |
| 执行统计 | CanvasExecutionStatsDO + CanvasMetrics | ✅ 完整 |
| 租户套餐 | TenantDO.planCode + quotaJson | ⚠️ 仅标签+JSON |
| 租户用量 | TenantService.usage() | ✅ 基础 |
| 限流 | CanvasExecutionReplayRateLimiter + RedisKeyUtil.apiRateLimit | ✅ 完整 |
| Kill Switch | KillSwitchSubscriber (FORCE/GRACEFUL) | ✅ 完整 |
| 权益发放 | CouponHandler + PointsOperationHandler | ✅ 完整 |
| 支付集成 | — | ❌ 无 |
| 计费引擎 | — | ❌ 无 |
| 分成/佣金 | — | ❌ 无 |
| 发票 | — | ❌ 无 |

### 解决方案（全做）

#### 1.1 计费引擎（3模式全做，可配置）

| 模式 | 说明 | 适用场景 | 配置项 |
|------|------|----------|--------|
| **按量计费** | 按执行次数/触达人数/节点数 | SaaS标准 | `billing.mode=USAGE` |
| **按效果付费** | 按转化事件/归因结果计费 | 高级客户 | `billing.mode=OUTCOME` |
| **功能分层** | 基础/专业/企业版功能矩阵 | 所有客户 | `billing.mode=TIER` |

**技术方案**：
- 新增 `billing_plan` 表（planCode/name/price/monthlyQuota/overagePrice/featuresJson）
- 新增 `billing_usage` 表（tenantId/period/metric/count/amount）
- 新增 `BillingMeteringService` — 从 CanvasMetrics 采集 → 聚合 → 写入 billing_usage
- 新增 `BillingEngine` — 按配置的计费模式计算账单
- 扩展 `TenantDO.planCode` → 外键关联 `billing_plan`
- 扩展 `quotaJson` → 从 billing_plan.featuresJson 读取功能开关

**按效果付费实现**：
- 依赖归因系统（决策3已规划）
- 新增 `billing_outcome_event` 表 — 记录归因转化事件
- `BillingOutcomeCalculator` — 转化事件 × 单价 = 费用
- 配置项：`billing.outcome.event_types`（哪些事件算效果）、`billing.outcome.price_per_event`

#### 1.2 支付集成（3渠道全做，可配置）

| 渠道 | 说明 | 配置项 |
|------|------|--------|
| **支付宝** | 企业支付宝 | `payment.alipay.enabled` |
| **微信支付** | 企业微信支付 | `payment.wechat.enabled` |
| **银行转账** | 线下转账+手动确认 | `payment.bank.enabled` |

**技术方案**：
- 新增 `payment_order` 表（orderId/tenantId/amount/currency/status/paymentMethod/externalTradeNo）
- 新增 `PaymentService` — 统一支付接口
- 新增 `PaymentCallbackController` — 支付回调
- 新增 `InvoiceService` — 发票生成

#### 1.3 增值服务矩阵（全做）

| 服务 | 说明 | 计费方式 |
|------|------|----------|
| **数据服务** | 受众包/标签市场/洞察报告 | 按次 |
| **咨询服务** | 策略咨询/数据咨询 | 按项目 |
| **定制开发** | 定制节点/定制渠道/定制报表 | 按项目 |
| **培训认证** | 运营师认证/最佳实践库 | 按人 |

**技术方案**：
- 新增 `value_added_service` 表（serviceCode/name/category/priceUnit/price）
- 新增 `service_order` 表
- 前端新增"增值服务市场"页面

#### 1.4 生态分成（全做）

| 分成模式 | 说明 | 配置项 |
|----------|------|--------|
| **模板市场分成** | 模板作者获得分成 | `commission.template.rate` |
| **渠道插件分成** | 渠道适配器开发者分成 | `commission.channel.rate` |
| **ISV合作分成** | ISV集成分成 | `commission.isv.rate` |

**技术方案**：
- 新增 `commission_rule` 表（ruleId/type/rate/settlementCycle）
- 新增 `commission_settlement` 表（settlementId/fromTenantId/toTenantId/amount/period/status）
- 新增 `CommissionService` — 分成计算+结算

#### 1.5 客户成功驱动（全做）

| 功能 | 说明 |
|------|------|
| **健康度评分** | 基于使用频率/成功率/功能覆盖 |
| **流失预警** | 使用下降+支持工单+NPS |
| **续费管理** | 自动续费提醒+续费优惠 |
| **扩展收入** | 升版推荐+增值服务推荐 |

**技术方案**：
- 新增 `tenant_health_score` 表（tenantId/score/dimensionsJson/lastCalculatedAt）
- 新增 `TenantHealthService` — 健康度计算
- 新增 `ChurnPredictionService` — 流失预警（规则引擎先行，后续接AI）
- 扩展 `TenantService` — 续费+扩展

#### 1.6 3版本功能矩阵

| 功能 | 基础版 | 专业版 | 企业版 |
|------|--------|--------|--------|
| 画布数 | 10 | 100 | 无限 |
| 执行次数/月 | 10K | 100K | 无限 |
| 渠道数 | 3 | 5 | 全部+自定义 |
| AI能力 | — | AI写文案 | 全部AI |
| A/B测试 | — | 基础 | 多臂老虎机 |
| 数据看板 | 基础 | 高级 | 自定义 |
| API调用 | — | 1000/天 | 无限 |
| SSO | — | — | SAML/OAuth/OIDC |
| 专属支持 | — | — | 客户成功经理 |

---

## 方向2：平台化演进

### 代码扫描现状

| 能力 | 实现位置 | 状态 |
|------|----------|------|
| Handler注册 | HandlerRegistry (@PostConstruct扫描) | ✅ 完整 |
| 节点类型注册 | NodeTypeRegistryDO (configSchema驱动) | ✅ 完整 |
| Groovy脚本 | GroovyHandler (沙箱+白名单+超时) | ✅ 完整 |
| API定义 | ApiDefinitionController + ApiCallHandler | ✅ 完整 |
| SSRF防护 | OutboundUrlValidator | ✅ 完整 |
| 多租户 | TenantContext + TenantContextResolver | ⚠️ 仅数据隔离 |
| Schema驱动UI | MetaController + configSchema | ✅ 完整 |
| 插件体系 | — | ❌ 无 |
| 开放API/开发者平台 | — | ❌ 无 |
| 应用市场 | — | ❌ 无 |

### 解决方案（全做）

#### 2.1 插件体系（3层全做，可配置）

| 层级 | 说明 | 加载方式 | 配置项 |
|------|------|----------|--------|
| **内置插件** | 编译时注册，当前Handler模式 | Spring @Component | `plugin.builtin.enabled` |
| **热插拔插件** | JAR包运行时加载 | PluginClassLoader | `plugin.hot-swap.enabled` |
| **远程插件** | gRPC/HTTP远程调用 | RemoteHandler | `plugin.remote.enabled` |

**技术方案**：
- 定义 `CanvasPlugin` 接口（pluginId/version/init/destroy/getHandlers）
- 新增 `PluginManager` — 插件生命周期管理
- 新增 `PluginClassLoader` — 隔离类加载器（每个插件独立）
- 新增 `RemoteHandler` — 通用远程Handler，通过gRPC/HTTP调用远程插件
- 扩展 `HandlerRegistry` — 支持运行时注册/注销
- 新增 `plugin_registry` 表（pluginId/name/version/type/status/configSchema）
- 前端新增"插件管理"页面

**Handler插件化**：
- 当前：`@NodeHandlerType` + `@Component` → 编译时注册
- 演进：`CanvasPlugin.getHandlers()` → 返回 `List<NodeHandler>` → 运行时注册
- 兼容：内置Handler保持 `@Component` 模式不变，插件Handler走 PluginManager

#### 2.2 开放平台（全做）

| 功能 | 说明 |
|------|------|
| **开发者门户** | API文档+SDK+示例+沙箱 |
| **API Key管理** | 已有ApiDefinition，扩展为API Key+权限+限流 |
| **Webhook出站** | 事件订阅+重试+签名验证 |
| **OAuth应用** | 第三方应用授权接入 |
| **速率限制** | 已有RedisKeyUtil.apiRateLimit，扩展为按API Key限流 |

**技术方案**：
- 新增 `api_key` 表（keyId/tenantId/name/secret/permissionsJson/rateLimitPerSec/status）
- 新增 `ApiKeyService` — Key生成+验证+限流
- 新增 `webhook_subscription` 表（subscriptionId/tenantId/eventType/callbackUrl/secret/retryPolicy）
- 新增 `WebhookDispatcher` — 事件分发+重试+签名
- 新增 `oauth_application` 表（clientId/clientSecret/name/redirectUris/scopes）
- 新增 `OAuthService` — 授权码+令牌流程
- 前端新增"开发者门户"页面

#### 2.3 低代码扩展（3级全做，可配置）

| 级别 | 说明 | 适用人群 | 配置项 |
|------|------|----------|--------|
| **L1: Schema配置** | 当前configSchema模式 | 运营人员 | `lowcode.level=SCHEMA` |
| **L2: Groovy脚本** | 当前GroovyHandler | 技术运营 | `lowcode.level=SCRIPT` |
| **L3: 可视化编程** | 拖拽式逻辑编排 | 运营人员 | `lowcode.level=VISUAL` |

**L3可视化编程方案**：
- 新增 `VisualLogicNode` — 子画布式逻辑编排
- 复用DagEngine执行子画布
- 前端新增"逻辑编辑器"组件（简化版画布编辑器）
- 内置逻辑块：条件判断/循环/变量操作/HTTP调用/数据转换

#### 2.4 多租户SaaS（3隔离级别全做，可配置）

| 级别 | 说明 | 适用场景 | 配置项 |
|------|------|----------|--------|
| **共享库+行隔离** | 当前模式，tenant_id过滤 | 标准SaaS | `tenant.isolation=ROW` |
| **Schema隔离** | 同库不同Schema | 中等规模 | `tenant.isolation=SCHEMA` |
| **库隔离** | 独立数据库 | 大客户 | `tenant.isolation=DATABASE` |

**技术方案**：
- 新增 `TenantIsolationStrategy` 接口
- `RowIsolationStrategy` — 当前模式，MyBatis-Plus TenantLineInnerInterceptor
- `SchemaIsolationStrategy` — 动态Schema切换
- `DatabaseIsolationStrategy` — 动态数据源路由（已有HikariCP）
- 新增 `TenantRoutingDataSource` — 按租户路由数据源
- 配置项：`tenant.isolation-strategy` 全局默认 + `tenant.isolation-strategy.{tenantId}` 按租户覆盖

---

## 方向3：数据资产演进

### 代码扫描现状

| 能力 | 实现位置 | 状态 |
|------|----------|------|
| CDP用户 | CdpUserService (身份解析+画像) | ✅ 完整 |
| CDP标签 | CdpTagService (CRUD+批量+导入+审计) | ✅ 完整 |
| CDP洞察 | CdpUserInsightService (360视图) | ✅ 完整 |
| 人群计算 | AudienceBatchComputeService (3路径+Bitmap) | ✅ 完整 |
| 规则引擎 | RuleEvaluatorRouter (AVIATOR/QLExpress) | ✅ 完整 |
| 数据源 | DataSourceConfigController | ⚠️ 仅JDBC |
| 标签导入 | TagImportController (API+Excel) | ✅ 完整 |
| 数据治理 | — | ❌ 无 |
| 数据变现 | — | ❌ 无 |
| ETL管道 | — | ❌ 无 |
| 数据目录 | DataSourceConfigController.listTables | ⚠️ 极简 |

### 解决方案（全做）

#### 3.1 数据治理（5模块全做）

| 模块 | 说明 | 技术方案 |
|------|------|----------|
| **数据质量** | 完整性/准确性/一致性/时效性检查 | 新增 `DataQualityRule` + `DataQualityCheckJob` |
| **数据血缘** | 字段级血缘追踪 | 新增 `DataLineageService` — 解析画布DAG+SQL生成血缘图 |
| **数据目录** | 元数据管理+搜索+分类 | 扩展 DataSourceConfigController → `DataCatalogService` |
| **数据保留** | 保留策略+自动清理 | 扩展 CanvasVersionCleanupJob → 通用 `DataRetentionService` |
| **数据合规** | PII识别+分类+脱敏策略 | 扩展 DataMaskingUtil → `DataClassificationService` |

**数据质量规则配置**：
- 规则类型：NOT_NULL/UNIQUE/RANGE/REGEX/CUSTOM_GROOVY
- 检查频率：ON_WRITE/SCHEDULED/ON_DEMAND
- 配置项：`data-quality.auto-fix`（自动修复开关）、`data-quality.alert-threshold`

#### 3.2 数据产品化（3产品全做）

| 产品 | 说明 | 计费方式 |
|------|------|----------|
| **受众包** | 打包人群定义+计算结果，供外部使用 | 按人群规模 |
| **标签市场** | 标签定义共享+交易 | 按标签使用量 |
| **洞察报告** | 自动生成行业/渠道/活动洞察 | 按报告数 |

**技术方案**：
- 新增 `audience_package` 表（packageId/audienceId/price/downloadCount）
- 新增 `tag_market_item` 表（tagCode/price/downloadCount/rating）
- 新增 `insight_report` 表（reportId/type/period/parametersJson/resultJson）
- 新增 `InsightReportGenerator` — 基于CanvasStats+CDP数据自动生成

#### 3.3 CDP一体化（3层全做，可配置）

| 层级 | 说明 | 配置项 |
|------|------|--------|
| **L1: 营销CDP** | 当前能力（标签+人群+画像+触达） | `cdp.level=MARKETING` |
| **L2: 分析CDP** | +行为分析+归因+漏斗+路径 | `cdp.level=ANALYTICS` |
| **L3: 全栈CDP** | +数据采集+ETL+实时计算+数据科学 | `cdp.level=FULLSTACK` |

**L2扩展方案**：
- 新增 `BehaviorAnalyticsService` — 行为序列分析
- 新增 `PathAnalyticsService` — 用户路径分析（桑基图）
- 扩展 CanvasStatsController → 渠道归因+多触点归因

**L3扩展方案**：
- 新增 `EventCollectorService` — 统一事件采集（SDK+Server-side+Webhook）
- 新增 `RealtimeComputeService` — Flink/Spark Streaming 实时计算
- 新增 `DataScienceWorkspace` — Jupyter集成/模型训练/特征工程

#### 3.4 数据同步/ETL（3模式全做，可配置）

| 模式 | 说明 | 延迟 | 配置项 |
|------|------|------|--------|
| **实时** | CDC/Stream | <1s | `sync.mode=REALTIME` |
| **准实时** | 微批 | 1-5min | `sync.mode=NEAR_REALTIME` |
| **批量** | 定时全量/增量 | 1h+ | `sync.mode=BATCH` |

**技术方案**：
- 新增 `data_sync_task` 表（taskId/sourceType/sourceConfig/targetType/targetConfig/syncMode/cronExpression）
- 新增 `DataSyncService` — 统一同步调度
- 新增 `CdcConnector` — MySQL Binlog CDC
- 新增 `BatchSyncJob` — 增量同步（基于updated_at时间戳）
- 扩展 TagImportSourceController → 通用数据同步管理

---

## 方向4：智能化演进路径

### 代码扫描现状

| 能力 | 实现位置 | 状态 |
|------|----------|------|
| AI下一步动作 | AiNextBestActionHandler | ⚠️ stub(本地兜底) |
| 推荐节点 | RecommendationHandler | ⚠️ stub(静态fallback) |
| 评分节点 | ScoringHandler | ✅ 规则引擎(非ML) |
| A/B实验 | ExperimentHandler + WeightedChoice | ✅ 完整 |
| 随机分流 | RandomSplitHandler | ✅ 完整 |
| AI Gateway | — | ❌ 无 |
| LLM集成 | — | ❌ 无 |
| 预测模型 | — | ❌ 无 |
| NLP | — | ❌ 无 |
| 异常检测 | — | ❌ 无 |
| 自愈/自优化 | — | ❌ 无 |

### 解决方案（4级全做）

#### 4.1 AI能力4级路径

| 级别 | 名称 | 能力 | 阶段 |
|------|------|------|------|
| **L1** | AI辅助 | 写文案/翻译/润色/变量建议 | 阶段1 |
| **L2** | AI建议 | 建画布/优化建议/异常检测/渠道推荐 | 阶段2 |
| **L3** | AI自主 | Agent自主执行/自愈/自优化 | 阶段3 |
| **L4** | AI原生 | 自然语言交互/对话式运营/预测引擎 | 阶段4 |

#### 4.2 AI Gateway（统一入口，全做）

| 功能 | 说明 | 配置项 |
|------|------|--------|
| **多模型路由** | OpenAI/Claude/通义/文心/智谱 | `ai.gateway.models` |
| **模型选择策略** | 按任务类型/成本/延迟 | `ai.gateway.routing=TASK/COST/LATENCY` |
| **Token管理** | 用量统计+限流+预算 | `ai.gateway.token-budget` |
| **Prompt模板** | 内置+自定义Prompt模板 | `ai.gateway.prompt-templates` |
| **安全过滤** | 输入/输出安全检查 | `ai.gateway.safety-filter` |

**技术方案**：
- 新增 `ai_gateway_config` 表
- 新增 `AiGatewayService` — 统一AI调用入口
- 新增 `AiModelAdapter` 接口 → OpenAiAdapter/ClaudeAdapter/TongyiAdapter
- 新增 `PromptTemplateService` — Prompt模板管理
- 新增 `AiTokenMeteringService` — Token用量统计
- 扩展 AiNextBestActionHandler → 调用AiGatewayService
- 扩展 RecommendationHandler → 调用AiGatewayService

#### 4.3 AI Agent体系（3类Agent全做）

| Agent类型 | 说明 | 自主度 | 配置项 |
|-----------|------|--------|--------|
| **执行Agent** | 按画布定义执行，异常时自愈 | 低 | `agent.type=EXECUTION` |
| **优化Agent** | 监控效果，自动调整参数 | 中 | `agent.type=OPTIMIZATION` |
| **规划Agent** | 给定目标，自动规划画布 | 高 | `agent.type=PLANNING` |

**执行Agent**：
- 基于现有DagEngine
- 新增 `SelfHealingService` — 异常时自动重试/降级/跳过
- 新增 `ExecutionAgentController` — Agent状态监控

**优化Agent**：
- 新增 `CanvasOptimizationAgent` — 监控转化率，自动调整分流比例/发送时间
- 新增 `ChannelOptimizationAgent` — 监控渠道效果，自动调整渠道优先级
- 配置项：`agent.optimization.auto-apply`（自动应用/仅建议）

**规划Agent**：
- 新增 `CanvasPlanningAgent` — 输入营销目标 → 生成画布DAG
- 新增 `GoalDecompositionService` — 目标分解为子目标+节点
- 前端新增"AI建画布"交互面板

#### 4.4 预测能力（3模型全做，可配置）

| 模型 | 说明 | 输入 | 输出 | 配置项 |
|------|------|------|------|--------|
| **流失预测** | 预测用户流失概率 | 行为+画像+标签 | churn_score | `prediction.model=CHURN` |
| **CLV预测** | 预测客户终身价值 | 交易+行为 | clv_score | `prediction.model=CLV` |
| **转化预测** | 预测营销转化概率 | 画像+历史响应 | conversion_score | `prediction.model=CONVERSION` |

**技术方案**：
- 新增 `prediction_model` 表（modelId/type/version/featuresJson/endpointUrl）
- 新增 `PredictionService` — 统一预测接口
- 新增 `FeatureStoreService` — 特征计算+存储
- 新增 `PredictionNodeHandler` — 画布中调用预测模型
- 初期：规则引擎评分（ScoringHandler增强）→ 后期：ML模型服务

#### 4.5 NLP/对话式运营（全做）

| 功能 | 说明 | 阶段 |
|------|------|------|
| **自然语言建画布** | "给活跃用户发优惠券" → 自动生成画布 | 阶段4 |
| **自然语言查询** | "上周邮件打开率多少" → 返回数据 | 阶段4 |
| **对话式优化** | "帮我提高转化率" → 建议并执行 | 阶段4 |

**技术方案**：
- 新增 `NlpIntentParser` — 意图识别+实体抽取
- 新增 `IntentToCanvasTranslator` — 意图→画布DAG
- 新增 `IntentToQueryTranslator` — 意图→SQL/统计查询
- 前端新增"AI助手"对话面板

---

## 方向5：渠道生态演进

### 代码扫描现状

| 能力 | 实现位置 | 状态 |
|------|----------|------|
| 5渠道Handler | SendSms/Email/Push/InApp/Wechat | ✅ 完整 |
| 统一投递 | ReachDeliveryService (幂等+记录) | ✅ 完整 |
| 渠道可用性 | ChannelAvailabilityHandler | ✅ 完整 |
| 优先级路由 | PriorityHandler (顺序尝试) | ✅ 完整 |
| 条件选择 | SelectorHandler | ✅ 完整 |
| 同意/抑制 | SuppressionCheckHandler | ✅ 完整 |
| 静默时段 | QuietHoursHandler | ✅ 完整 |
| 疲劳度 | FrequencyCapHandler | ✅ 完整 |
| 渠道偏好 | CustomerChannelDO (enabled/verified) | ⚠️ 无偏好排序 |
| 企微 | — | ❌ 无 |
| 私域矩阵 | — | ❌ 无 |
| RCS/WhatsApp/Telegram | — | ❌ 无 |
| 渠道编排引擎 | — | ❌ 无(Priority+可用性=隐式) |

### 解决方案（全做）

#### 5.1 企微4层（已规划，细化）

| 层级 | 节点 | Handler | 阶段 |
|------|------|---------|------|
| **L1: 基础触达** | SEND_WECOM_MSG | SendWecomMsgHandler | 阶段1 |
| **L2: 客户管理** | WECOM_CUSTOMER_ADD/UPDATE | WecomCustomerHandler | 阶段1 |
| **L3: 裂变** | WECOM_SHARE/INVITE | WecomShareHandler | 阶段2 |
| **L4: SCRM** | WECOM_SOP/ASSIGN/CHAT_ARCHIVE | WecomScrmHandler | 阶段3 |

**技术方案**：
- 新增 `wecom_config` 表（corpId/agentId/secret/callbackUrl）
- 新增 `WecomApiService` — 企微API封装
- 新增 `WecomCallbackController` — 企微回调
- 4个Handler继承AbstractSendMessageHandler

#### 5.2 私域矩阵（4平台全做，可配置）

| 平台 | Handler | 能力 | 配置项 |
|------|---------|------|--------|
| **企微** | SendWecomMsgHandler | 消息+客户+SOP | `channel.wecom.enabled` |
| **抖音私域** | SendDouyinHandler | 私信+粉丝群 | `channel.douyin.enabled` |
| **小红书私域** | SendXiaohongshuHandler | 私信+笔记 | `channel.xiaohongshu.enabled` |
| **快手私域** | SendKuaishouHandler | 私信+群 | `channel.kuaishou.enabled` |

**技术方案**：
- 每个平台一个Handler + ConfigService + CallbackController
- 统一继承AbstractSendMessageHandler
- 新增 `channel_config` 表（channelType/appId/appSecret/callbackUrl/extraConfig）
- 前端新增"渠道配置"页面

#### 5.3 新兴渠道（3渠道全做，可配置）

| 渠道 | Handler | 配置项 |
|------|---------|--------|
| **RCS** | SendRcsHandler | `channel.rcs.enabled` |
| **WhatsApp** | SendWhatsappHandler | `channel.whatsapp.enabled` |
| **Telegram** | SendTelegramHandler | `channel.telegram.enabled` |

#### 5.4 渠道编排引擎（3策略全做，可配置）

| 策略 | 说明 | 配置项 |
|------|------|--------|
| **优先级降级** | 当前Priority模式，按顺序尝试 | `orchestration.strategy=PRIORITY` |
| **智能选择** | 基于用户偏好+历史响应率+渠道成本 | `orchestration.strategy=SMART` |
| **全渠道协同** | 多渠道同时触达+去重+归因 | `orchestration.strategy=OMNICHANNEL` |

**智能选择方案**：
- 新增 `ChannelSelectionEngine` — 综合评分选择最优渠道
- 评分维度：用户偏好(40%) + 历史响应率(30%) + 渠道成本(20%) + 时效性(10%)
- 新增 `channel_response_stats` 表 — 渠道响应率统计
- 新增 `ChannelPreferenceService` — 用户渠道偏好管理

**全渠道协同方案**：
- 新增 `OmnichannelOrchestrator` — 多渠道编排
- 新增 `channel_dedup` 表 — 跨渠道去重
- 新增 `OmnichannelAttributionService` — 跨渠道归因

#### 5.5 渠道冲突解决（3策略全做，可配置）

| 策略 | 说明 | 配置项 |
|------|------|--------|
| **先到先得** | 第一个成功渠道为准 | `conflict.resolution=FIRST_WINS` |
| **最优渠道** | 效果最好的渠道为准 | `conflict.resolution=BEST_CHANNEL` |
| **用户偏好** | 用户选择的渠道为准 | `conflict.resolution=USER_PREFERENCE` |

---

## 方向6：行业化演进

### 代码扫描现状

| 能力 | 实现位置 | 状态 |
|------|----------|------|
| 积分系统 | PointsOperationHandler + CustomerPointsLedgerDO | ✅ 通用 |
| 会员身份 | IdentityTypeDO (含member_id) | ✅ 通用 |
| 生命周期 | CustomerProfileDO.lifecycleStage | ✅ 通用(3阶段) |
| 合规节点 | 同意/抑制/疲劳度/静默时段 | ✅ 通用 |
| 行业特定 | — | ❌ 无 |

### 解决方案（全做，配置项驱动）

#### 6.1 行业模板包（4行业全做）

| 行业 | 模板数 | 核心模板 | 特殊节点 |
|------|--------|----------|----------|
| **零售** | 15+ | 新客欢迎/复购提醒/会员日/大促/流失挽回 | 门店引流/优惠券核销 |
| **金融** | 10+ | 开户欢迎/产品推荐/还款提醒/风控预警 | 合规审查/风险评估 |
| **教育** | 10+ | 报名确认/开课提醒/续费提醒/作业催交 | 课程推荐/学习进度 |
| **医疗** | 8+ | 预约确认/就诊提醒/用药提醒/体检通知 | 处方提醒/隐私保护 |

**技术方案**：
- 新增 `industry_template_pack` 表（packId/industry/templatesJson/nodesJson）
- 新增 `IndustryTemplateService` — 行业模板加载+初始化
- 前端新增"行业选择"引导页（首次使用时）
- 配置项：`industry.default`（默认行业）、`industry.template.auto-install`（自动安装模板）

#### 6.2 行业节点（按行业扩展）

| 行业 | 新增节点 | Handler |
|------|----------|---------|
| **零售** | STORE_CHECKIN(门店签到) | StoreCheckinHandler |
| **零售** | COUPON_REDEEM(优惠券核销) | CouponRedeemHandler |
| **金融** | RISK_ASSESSMENT(风险评估) | RiskAssessmentHandler |
| **金融** | COMPLIANCE_CHECK(合规审查) | ComplianceCheckHandler |
| **教育** | COURSE_RECOMMEND(课程推荐) | CourseRecommendHandler |
| **教育** | LEARNING_PROGRESS(学习进度) | LearningProgressHandler |
| **医疗** | PRESCRIPTION_REMIND(处方提醒) | PrescriptionRemindHandler |
| **医疗** | PRIVACY_GUARD(隐私保护) | PrivacyGuardHandler |

**技术方案**：
- 所有行业节点通过插件体系（方向2）注册
- 行业节点默认禁用，按行业模板包启用
- 配置项：`industry.nodes.{nodeType}.enabled`

#### 6.3 行业合规（4行业全做，可配置）

| 行业 | 合规要求 | 实现方式 |
|------|----------|----------|
| **金融** | 营销话术审核/风险提示/录音存档 | ComplianceCheckHandler + audit_log |
| **医疗** | 患者隐私/处方合规/广告限制 | PrivacyGuardHandler + 数据分类 |
| **教育** | 未成年人保护/广告限制 | AgeCheckHandler + 内容过滤 |
| **零售** | 消费者权益/7天无理由 | RefundHandler + 消费提醒 |

**配置项**：`industry.compliance.{industry}.enabled`

#### 6.4 行业指标（4行业全做）

| 行业 | 核心指标 | 数据源 |
|------|----------|--------|
| **零售** | GMV/复购率/客单价/会员渗透率 | 交易系统+CDP |
| **金融** | AUM/转化率/留存率/NPS | CRM+CDP |
| **教育** | 续费率/完课率/推荐率 | LMS+CDP |
| **医疗** | 就诊率/复诊率/满意度 | HIS+CDP |

**技术方案**：
- 新增 `industry_metric_definition` 表（metricId/industry/name/formula/dataSourceConfig）
- 新增 `IndustryMetricService` — 指标计算+展示
- 扩展 HomeOverviewController → 行业指标看板

---

## 方向7：运营模式演进

### 代码扫描现状

| 能力 | 实现位置 | 状态 |
|------|----------|------|
| 审批节点 | ManualApprovalHandler | ✅ 完整 |
| 执行统计 | CanvasStatsController | ✅ 基础 |
| 版本管理 | CanvasVersionController | ✅ 完整 |
| 代运营 | — | ❌ 无 |
| 咨询服务 | — | ❌ 无 |
| 培训认证 | — | ❌ 无 |
| 客户成功 | — | ❌ 无 |

### 解决方案（全做）

#### 7.1 代运营服务（3模式全做，可配置）

| 模式 | 说明 | 配置项 |
|------|------|--------|
| **全托管** | 平台代运营，客户只看效果 | `managed-service.mode=FULL` |
| **半托管** | 平台执行，客户审批 | `managed-service.mode=SEMI` |
| **自助** | 客户自助，平台支持 | `managed-service.mode=SELF` |

**技术方案**：
- 新增 `managed_service_config` 表
- 新增 `ManagedServiceController` — 代运营工单管理
- 扩展 ManualApprovalHandler → 代运营审批流
- 新增 `EffectReportService` — 效果对赌报告

#### 7.2 咨询服务（2类全做）

| 类型 | 说明 | 交付物 |
|------|------|--------|
| **策略咨询** | 营销策略+画布设计 | 咨询报告+画布模板 |
| **数据咨询** | 数据分析+洞察 | 分析报告+优化建议 |

**技术方案**：
- 新增 `consultation_order` 表
- 新增 `ConsultationService` — 咨询工单管理
- 前端新增"咨询服务"入口

#### 7.3 培训认证（3级全做）

| 级别 | 名称 | 要求 |
|------|------|------|
| **初级** | 营销画布运营师 | 完成培训+考试 |
| **中级** | 高级运营师 | 初级+3个实战案例 |
| **高级** | 专家运营师 | 中级+5个成功项目 |

**技术方案**：
- 新增 `certification` 表（certId/userId/level/issuedAt/expiresAt）
- 新增 `TrainingService` — 培训课程+考试
- 新增 `CertificationService` — 认证管理
- 前端新增"培训认证"页面

#### 7.4 客户成功（4功能全做）

| 功能 | 说明 | 技术方案 |
|------|------|----------|
| **健康度监控** | 租户使用健康度评分 | 复用方向1.5 TenantHealthService |
| **流失预警** | 使用下降+支持工单预警 | 复用方向1.5 ChurnPredictionService |
| **续费管理** | 到期提醒+续费优惠 | 扩展 TenantService |
| **最佳实践库** | 行业+场景最佳实践 | 新增 `best_practice` 表 |

---

## 方向8：技术架构演进

### 代码扫描现状

| 能力 | 实现位置 | 状态 |
|------|----------|------|
| 虚拟线程 | DagEngine + 全局 | ✅ Java 21 |
| Reactor/WebFlux | 全局响应式 | ✅ 完整 |
| 执行车道 | ExecutionLane (4车道) | ✅ 完整 |
| Disruptor | LMAX无锁事件 | ✅ 完整 |
| 断路器 | CircuitBreakerRegistry | ✅ 完整 |
| Watchdog | ExecutionWatchdog | ✅ 完整 |
| 缓存 | Caffeine L1 + Redis L2 | ✅ 完整 |
| 微服务 | — | ❌ 单体 |
| Serverless | — | ❌ 无 |
| 多云 | — | ❌ 无 |
| 边缘计算 | — | ❌ 无 |

### 解决方案（全做，渐进式）

#### 8.1 微服务拆分（3步走，可配置）

| 步骤 | 说明 | 配置项 |
|------|------|--------|
| **Step1: 模块化** | Maven多模块+模块间API | `arch.module-boundaries=true` |
| **Step2: 服务拆分** | 独立部署+RPC通信 | `arch.deployment=MICROSERVICE` |
| **Step3: 事件驱动** | 事件总线+CQRS | `arch.communication=EVENT_DRIVEN` |

**服务拆分方案**：
| 服务 | 职责 | 独立部署理由 |
|------|------|-------------|
| **canvas-editor** | 画布编辑+版本管理 | 读写比高，可独立伸缩 |
| **canvas-engine** | DAG执行+触发+投递 | CPU密集，需独立伸缩 |
| **canvas-cdp** | 用户+标签+人群+画像 | 数据密集，需独立存储 |
| **canvas-analytics** | 统计+漏斗+归因 | 计算密集，可异步 |
| **canvas-admin** | 租户+用户+权限+配置 | 低频，可独立部署 |
| **canvas-billing** | 计费+支付+发票 | 独立合规要求 |

**通信方式配置**：
| 方式 | 说明 | 配置项 |
|------|------|--------|
| **进程内** | 当前模式，方法调用 | `service.communication=IN_PROCESS` |
| **Feign** | HTTP REST调用 | `service.communication=FEIGN` |
| **gRPC** | 高性能RPC | `service.communication=GRPC` |
| **事件** | RocketMQ事件 | `service.communication=EVENT` |

#### 8.2 Serverless（3场景全做，可配置）

| 场景 | 说明 | 配置项 |
|------|------|--------|
| **触发执行** | 按需启动引擎 | `serverless.trigger=true` |
| **人群计算** | 按需计算人群 | `serverless.audience=true` |
| **数据同步** | 按需同步数据 | `serverless.sync=true` |

**技术方案**：
- 新增 `ServerlessTaskRunner` — 提交任务到Serverless平台
- 适配器：Knative/阿里云FC/AWS Lambda
- 配置项：`serverless.platform=KNATIVE/ALIYUN_FC/AWS_LAMBDA`

#### 8.3 多云部署（3云全做，可配置）

| 云 | 说明 | 配置项 |
|-----|------|--------|
| **阿里云** | 国内首选 | `cloud.provider=ALIYUN` |
| **腾讯云** | 企微生态 | `cloud.provider=TENCENT` |
| **AWS** | 海外 | `cloud.provider=AWS` |
| **私有化** | 大客户 | `cloud.provider=ON_PREMISE` |

**技术方案**：
- 新增 `CloudAdapter` 接口 → AliyunAdapter/TencentAdapter/AwsAdapter
- 新增 `CloudResourceService` — 统一资源管理
- 配置项：`cloud.provider` + `cloud.region`

#### 8.4 边缘计算（2场景全做，可配置）

| 场景 | 说明 | 配置项 |
|------|------|--------|
| **就近执行** | 用户就近节点执行 | `edge.execution=true` |
| **数据合规** | 数据不出境 | `edge.data-residency=true` |

**技术方案**：
- 新增 `EdgeNodeRegistry` — 边缘节点注册
- 新增 `EdgeRoutingService` — 请求路由到最近边缘节点
- 新增 `DataResidencyService` — 数据驻留合规检查

---

## 方向9：安全与合规演进

### 代码扫描现状

| 能力 | 实现位置 | 状态 |
|------|----------|------|
| JWT认证 | JwtAuthFilter + JwtUtil | ✅ 完整 |
| RBAC | SecurityConfig (4角色) | ✅ 完整 |
| 暴力防护 | AuthController (5次→15分钟锁定) | ✅ 完整 |
| SSRF防护 | OutboundUrlValidator | ✅ 完整 |
| PII脱敏 | DataMaskingUtil (6种) | ✅ 完整 |
| 同意管理 | MarketingConsentDO (OPT_IN/OUT) | ✅ 完整 |
| 抑制列表 | MarketingSuppressionDO | ✅ 完整 |
| 审计日志表 | canvas_audit_log (SQL) | ⚠️ 表存在无Service |
| 事件签名 | EventReportAuthService (HMAC-SHA256) | ✅ 完整 |
| 数据删除权 | — | ❌ 无 |
| 数据导出 | — | ❌ 无 |
| 隐私计算 | — | ❌ 无 |
| 等保/ISO/SOC2 | — | ❌ 无 |
| 跨境合规 | — | ❌ 无 |

### 解决方案（全做）

#### 9.1 安全体系（4认证全做，可配置）

| 认证 | 说明 | 配置项 |
|------|------|--------|
| **JWT** | 当前模式 | `auth.type=JWT` |
| **SAML** | 企业SSO | `auth.type=SAML` |
| **OAuth2** | 社交登录 | `auth.type=OAUTH2` |
| **OIDC** | 标准SSO | `auth.type=OIDC` |

**技术方案**：
- 新增 `SamlAuthProvider` — SAML断言验证
- 新增 `OAuth2AuthProvider` — OAuth2授权码流程
- 新增 `OidcAuthProvider` — OIDC发现+验证
- 新增 `auth_provider_config` 表（providerType/configJson）
- 扩展 SecurityConfig → 多认证Provider链

#### 9.2 数据删除权（3模式全做，可配置）

| 模式 | 说明 | 配置项 |
|------|------|--------|
| **软删除** | 标记删除，保留审计 | `deletion.mode=SOFT` |
| **匿名化** | PII替换为匿名标识 | `deletion.mode=ANONYMIZE` |
| **硬删除** | 物理删除所有关联数据 | `deletion.mode=HARD` |

**技术方案**：
- 新增 `DataDeletionService` — 统一删除入口
- 新增 `data_deletion_request` 表（requestId/userId/scope/mode/status/requestedAt/completedAt）
- 级联删除：用户画像→标签→渠道→同意→抑制→执行记录→消息记录
- 审计：删除操作记录到 canvas_audit_log
- 配置项：`deletion.grace-period-days`（宽限期）

#### 9.3 数据导出（3格式全做）

| 格式 | 说明 |
|------|------|
| **CSV** | 表格数据导出 |
| **JSON** | 结构化数据导出 |
| **PDF** | 合规报告导出 |

**技术方案**：
- 新增 `DataExportService` — 统一导出
- 新增 `data_export_task` 表（taskId/userId/scope/format/status/fileUrl）
- 异步导出：虚拟线程执行 → OSS存储 → 通知下载

#### 9.4 隐私计算（3技术全做，可配置）

| 技术 | 说明 | 适用场景 | 配置项 |
|------|------|----------|--------|
| **差分隐私** | 数据加噪保护个体 | 统计分析 | `privacy.differential-privacy=true` |
| **联邦学习** | 数据不出域联合建模 | 跨企业模型 | `privacy.federated-learning=true` |
| **可信执行环境** | 加密计算 | 敏感数据处理 | `privacy.trusted-execution=true` |

**技术方案**：
- 新增 `DifferentialPrivacyService` — 统计结果加噪（Laplace机制）
- 新增 `FederatedLearningClient` — 联邦学习客户端
- 新增 `TeeExecutionService` — TEE执行代理
- 阶段4实现，当前仅架构预留

#### 9.5 跨境合规（3法规全做，可配置）

| 法规 | 说明 | 配置项 |
|------|------|--------|
| **GDPR** | 欧盟数据保护 | `compliance.gdpr.enabled` |
| **CCPA** | 加州消费者隐私 | `compliance.ccpa.enabled` |
| **PIPL** | 中国个人信息保护 | `compliance.pipl.enabled` |

**技术方案**：
- 新增 `ComplianceRuleEngine` — 合规规则引擎
- 新增 `compliance_rule` 表（ruleId/regulation/action/conditionJson）
- 新增 `DataResidencyService` — 数据驻留检查
- 新增 `CrossBorderTransferService` — 跨境传输合规
- 配置项：`compliance.data-residency-regions`（数据驻留区域列表）

#### 9.6 审计追溯（补全）

| 缺失 | 方案 |
|------|------|
| AuditLog Service层 | 新增 `AuditLogDO` + `AuditLogMapper` + `AuditLogService` |
| 全链路审计 | 扩展 ExecutionContext → 审计日志 |
| 不可篡改 | 新增 `audit_log_hash` 字段（链式哈希） |
| 审计查询API | 新增 `AuditLogController` |

---

## 方向10：国际化演进

### 代码扫描现状

| 能力 | 实现位置 | 状态 |
|------|----------|------|
| 时区支持 | CustomerProfileDO.timezone + MarketingPolicyService | ✅ Asia/Shanghai默认 |
| Region字段 | CustomerProfileDO.region | ⚠️ 仅存储无逻辑 |
| i18n框架 | — | ❌ 无 |
| 多语言 | — | ❌ 全中文硬编码 |
| 多货币 | — | ❌ 无 |
| 本地化 | — | ❌ 无 |

### 解决方案（全做）

#### 10.1 i18n框架（3层全做）

| 层 | 说明 | 技术方案 |
|-----|------|----------|
| **后端** | Spring MessageSource + 多语言消息 | 新增 `messages_{locale}.properties` |
| **前端** | react-i18next + 语言包 | 新增 `locales/{locale}.json` |
| **数据库** | 多语言字段 | 新增 `i18n_text` 表（entityId/field/locale/value） |

**技术方案**：
- 后端：新增 `LocaleResolver` — 从Accept-Language/JWT/cookie解析
- 后端：新增 `I18nService` — 统一多语言
- 前端：集成 react-i18next
- 前端：新增 `LanguageSwitcher` 组件
- 配置项：`i18n.default-locale=zh-CN`、`i18n.supported-locales=zh-CN,en-US,ja-JP`

#### 10.2 多语言（3语言全做，可配置）

| 语言 | 说明 | 配置项 |
|------|------|--------|
| **中文** | 当前默认 | `i18n.locale=zh-CN` |
| **英文** | 国际化 | `i18n.locale=en-US` |
| **日文** | 亚洲市场 | `i18n.locale=ja-JP` |

**翻译范围**：
- UI文本（前端语言包）
- 错误消息（后端MessageSource）
- 节点名称/描述（NodeTypeRegistry configSchema）
- 邮件/短信模板（模板变量+多语言版本）
- 通知消息（i18n_text表）

#### 10.3 多时区（3模式全做，可配置）

| 模式 | 说明 | 配置项 |
|------|------|--------|
| **用户时区** | 按用户profile.timezone | `timezone.mode=USER` |
| **租户时区** | 按租户默认时区 | `timezone.mode=TENANT` |
| **系统时区** | 全局统一时区 | `timezone.mode=SYSTEM` |

**技术方案**：
- 扩展 `TenantDO` → 新增 `defaultTimezone` 字段
- 扩展 `MarketingPolicyService.timezoneFor()` → 3级回退（用户→租户→系统）
- 扩展 `ScheduleRegistration` → 支持租户时区
- 前端：所有时间显示使用用户时区

#### 10.4 多货币（3货币全做，可配置）

| 货币 | 说明 | 配置项 |
|------|------|--------|
| **CNY** | 人民币 | `currency.default=CNY` |
| **USD** | 美元 | `currency.supported=CNY,USD,EUR` |
| **EUR** | 欧元 | — |

**技术方案**：
- 新增 `currency_config` 表（currencyCode/symbol/exchangeRate/decimalPlaces）
- 新增 `CurrencyService` — 货币转换+格式化
- 扩展 `billing_usage` → 支持 multi-currency
- 前端：金额显示使用用户/租户货币

#### 10.5 本地化（4维度全做）

| 维度 | 说明 | 技术方案 |
|------|------|----------|
| **渠道本地化** | 不同地区不同渠道 | 渠道配置按region启用/禁用 |
| **合规本地化** | 不同地区不同合规规则 | 复用方向9 ComplianceRuleEngine |
| **支付本地化** | 不同地区不同支付方式 | 支付宝/微信(中国) + Stripe(海外) |
| **内容本地化** | 不同地区不同内容 | 模板多语言版本 |

---

## 配置项总表

| 方向 | 配置项 | 选项 | 默认值 |
|------|--------|------|--------|
| 1-商业模式 | billing.mode | USAGE/OUTCOME/TIER | TIER |
| 1-商业模式 | billing.outcome.event_types | 自定义 | — |
| 1-商业模式 | payment.alipay.enabled | true/false | false |
| 1-商业模式 | payment.wechat.enabled | true/false | false |
| 1-商业模式 | payment.bank.enabled | true/false | true |
| 1-商业模式 | commission.template.rate | 0.0-1.0 | 0.3 |
| 1-商业模式 | commission.channel.rate | 0.0-1.0 | 0.2 |
| 1-商业模式 | commission.isv.rate | 0.0-1.0 | 0.15 |
| 2-平台化 | plugin.builtin.enabled | true/false | true |
| 2-平台化 | plugin.hot-swap.enabled | true/false | false |
| 2-平台化 | plugin.remote.enabled | true/false | false |
| 2-平台化 | lowcode.level | SCHEMA/SCRIPT/VISUAL | SCHEMA |
| 2-平台化 | tenant.isolation | ROW/SCHEMA/DATABASE | ROW |
| 3-数据资产 | cdp.level | MARKETING/ANALYTICS/FULLSTACK | MARKETING |
| 3-数据资产 | sync.mode | REALTIME/NEAR_REALTIME/BATCH | BATCH |
| 3-数据资产 | data-quality.auto-fix | true/false | false |
| 3-数据资产 | data-quality.alert-threshold | 0.0-1.0 | 0.8 |
| 3-数据资产 | data-retention.default-days | 30-3650 | 365 |
| 4-智能化 | ai.gateway.models | 自定义列表 | — |
| 4-智能化 | ai.gateway.routing | TASK/COST/LATENCY | TASK |
| 4-智能化 | ai.gateway.token-budget | 数字 | 100000 |
| 4-智能化 | ai.gateway.safety-filter | true/false | true |
| 4-智能化 | agent.type | EXECUTION/OPTIMIZATION/PLANNING | EXECUTION |
| 4-智能化 | agent.optimization.auto-apply | true/false | false |
| 4-智能化 | prediction.model | CHURN/CLV/CONVERSION | — |
| 5-渠道 | channel.wecom.enabled | true/false | false |
| 5-渠道 | channel.douyin.enabled | true/false | false |
| 5-渠道 | channel.xiaohongshu.enabled | true/false | false |
| 5-渠道 | channel.kuaishou.enabled | true/false | false |
| 5-渠道 | channel.rcs.enabled | true/false | false |
| 5-渠道 | channel.whatsapp.enabled | true/false | false |
| 5-渠道 | channel.telegram.enabled | true/false | false |
| 5-渠道 | orchestration.strategy | PRIORITY/SMART/OMNICHANNEL | PRIORITY |
| 5-渠道 | conflict.resolution | FIRST_WINS/BEST_CHANNEL/USER_PREFERENCE | FIRST_WINS |
| 6-行业化 | industry.default | RETAIL/FINANCE/EDUCATION/HEALTHCARE | — |
| 6-行业化 | industry.template.auto-install | true/false | true |
| 6-行业化 | industry.compliance.retail.enabled | true/false | false |
| 6-行业化 | industry.compliance.finance.enabled | true/false | false |
| 6-行业化 | industry.compliance.education.enabled | true/false | false |
| 6-行业化 | industry.compliance.healthcare.enabled | true/false | false |
| 7-运营 | managed-service.mode | FULL/SEMI/SELF | SELF |
| 7-运营 | agent.optimization.auto-apply | true/false | false |
| 7-运营 | certification.enabled | true/false | false |
| 7-运营 | customer-success.enabled | true/false | false |
| 8-架构 | arch.deployment | MONOLITH/MICROSERVICE | MONOLITH |
| 8-架构 | arch.communication | IN_PROCESS/FEIGN/GRPC/EVENT | IN_PROCESS |
| 8-架构 | serverless.platform | KNATIVE/ALIYUN_FC/AWS_LAMBDA | — |
| 8-架构 | cloud.provider | ALIYUN/TENCENT/AWS/ON_PREMISE | ON_PREMISE |
| 8-架构 | edge.execution | true/false | false |
| 9-安全 | auth.type | JWT/SAML/OAUTH2/OIDC | JWT |
| 9-安全 | deletion.mode | SOFT/ANONYMIZE/HARD | SOFT |
| 9-安全 | deletion.grace-period-days | 7-90 | 30 |
| 9-安全 | privacy.differential-privacy | true/false | false |
| 9-安全 | privacy.federated-learning | true/false | false |
| 9-安全 | compliance.gdpr.enabled | true/false | false |
| 9-安全 | compliance.ccpa.enabled | true/false | false |
| 9-安全 | compliance.pipl.enabled | true/false | true |
| 10-国际化 | i18n.default-locale | zh-CN/en-US/ja-JP | zh-CN |
| 10-国际化 | i18n.supported-locales | 自定义列表 | zh-CN |
| 10-国际化 | timezone.mode | USER/TENANT/SYSTEM | USER |
| 10-国际化 | currency.default | CNY/USD/EUR | CNY |
| 10-国际化 | currency.supported | 自定义列表 | CNY |

**总计：61个配置项**

---

## 阶段映射

| 阶段 | 时间 | 演进方向重点 |
|------|------|-------------|
| **阶段0** | 1-2月 | 方向9(安全止血)+方向5(L1渠道)+方向4(L1 AI) |
| **阶段1** | 2-4月 | 方向4(L1 AI)+方向5(企微L1-L2)+方向9(删除权/导出)+方向1(计费基础) |
| **阶段2** | 4-7月 | 方向1(计费引擎)+方向2(插件体系)+方向3(CDP L2)+方向5(私域+智能选择) |
| **阶段3** | 7-10月 | 方向3(CDP L3)+方向4(L3 Agent)+方向6(行业化)+方向8(微服务)+方向10(i18n) |
| **阶段4** | 10-12月 | 方向4(L4 AI原生)+方向7(运营服务)+方向8(Serverless/多云)+方向9(隐私计算) |

---

## 依赖关系

```
方向1(商业) ──依赖──→ 方向3(数据资产) ──依赖──→ 方向4(智能化)
    │                    │                        │
    └──依赖──→ 方向9(合规)                    └──依赖──→ 方向5(渠道智能)
    
方向2(平台化) ──依赖──→ 方向6(行业化) ──依赖──→ 方向10(国际化)
    │                    │
    └──依赖──→ 方向8(架构) ──依赖──→ 方向7(运营)

关键路径：方向9(安全) → 方向1(商业) → 方向3(数据) → 方向4(智能) → 方向5(渠道智能)
```

---

*制定人：John (PM Agent) | 日期：2026-05-31 | 代码扫描+竞品对标+脑暴决策 | 61个配置项 | 10大演进方向全覆盖*
