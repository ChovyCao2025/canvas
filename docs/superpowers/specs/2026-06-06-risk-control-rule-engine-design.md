# Enterprise Risk Control Rule Engine Design

## 1. 背景与目标

目标是在 Marketing Canvas 平台内建设一套企业级、生产级的风控规则引擎产品。它面向营销、交易、支付、内容、私域触达、AI 决策和运营审批等场景，提供低延迟在线决策、离线/准实时特征、规则运营、灰度仿真、审计治理和模型协同能力。

这不是复刻某一家厂商的内部实现，而是基于公开资料抽象出适合 Canvas 的生产系统：

- 蚁盾/AIR Engine 和 Antom Shield 公开资料体现产品能力：风险评分、规则配置、名单、智能仿真、团伙分析、AI 辅助配置、实时决策和专家运营。
- 美团 Zeus 公开资料体现工程模型：场景、规则组、规则、因子、名单、决策表、标记、双跑、回溯、实时与异步/离线边界。
- 有赞风控规则引擎实践体现生态支撑：实时特征库、规则管理中心、离线任务、运营平台、模型作为规则补充、100ms 级实时拦截目标。
- Drools/DMN/OpenL 公开资料体现企业决策管理标准：规则引擎、DMN 决策服务、FEEL、决策表、业务知识模型、表格化规则。

本设计的目标是形成一个可作为独立产品售卖和内嵌 Canvas 使用的风控规则引擎，生产级定义包括：

1. 规则和模型上线不依赖发版。
2. 在线决策 P95 延迟可控，默认目标 50ms，复杂策略目标 100ms。
3. 所有策略发布、命中、拦截、人工复核、名单变更和阈值调整都有审计记录。
4. 新规则必须支持标记、双跑、回溯和灰度后再转为强制执行。
5. 特征、名单、规则、模型、动作、决策结果均租户隔离。
6. 规则错误不能导致资损扩大；运行失败按场景配置 fail-open/fail-closed。
7. 支持从 Canvas 节点、外部 API、MQ 事件、批量任务和运营控制台调用。

## 2. 参考资料

强参考：

- Antom Shield 概览：`https://docs.antom.com/ac/antomshield_zh-cn/overview`
- Antom Shield 规则配置：`https://docs.antom.com/ac/antomshield_zh-cn/rules`
- Antom Shield 风险评分：`https://docs.antom.com/ac/antomshield_zh-cn/risklevel`
- Antom Shield 名单管理：`https://docs.antom.com/ac/antomshield_zh-cn/list`
- Antom Shield 智能仿真：`https://docs.antom.com/ac/antomshield_zh-cn/simulation`
- Antom Shield 团伙分析：`https://docs.antom.com/ac/antomshield_zh-cn/analysis`
- Antom Shield 支持变量：`https://docs.antom.com/ac/antomshield_zh-cn/supported-attributes`
- Antom Shield 数据质量和安全 SDK：`https://docs.antom.com/ac/antomshield_zh-cn/getstarted`
- 支付宝开放平台“蚁盾”风险评分产品介绍：`https://doc.open.alipay.com/doc2/detail?articleId=105271&docType=1&treeId=214`
- 支付宝开放平台“蚁盾”风险评分快速接入：`https://doc.open.alipay.com/docs/doc.htm?treeId=214&articleId=105182&docType=1`
- 支付宝开放平台“蚁盾”风险评分 API 列表：`https://doc.open.alipay.com/docs/doc.htm?treeId=214&articleId=105270&docType=1`
- `alipay.security.risk.rainscore.query` API：`https://api.alidayu.com/docs/api.htm?docType=4&apiId=1048`
- 美团 Zeus 风控规则引擎原文地址：`https://tech.meituan.com/2020/05/14/meituan-security-zeus.html`。该地址当前返回 404，只作为历史定位，不作为可验证正文来源。
- 美团 Zeus 风控规则引擎同步页：`https://cloud.tencent.com/developer/news/628286`
- 美团 Zeus 风控规则引擎同步页：`https://segmentfault.com/a/1190000022653962`
- 美团 Zeus 运行时 JVM/ZGC 实践原文地址：`https://tech.meituan.com/2020/08/06/new-zgc-practice-in-meituan.html`。该地址当前返回 404，只作为历史定位，不作为可验证正文来源。
- 美团 Zeus 运行时 JVM/ZGC 实践同步页：`https://segmentfault.com/a/1190000023568163`
- 有赞风控规则引擎实践：`https://tech.youzan.com/rules-engine/`
- Drools Introduction：`https://docs.drools.org/latest/drools-docs/drools/introduction/index.html`
- Drools DMN：`https://docs.drools.org/latest/drools-docs/drools/DMN/index.html`
- OpenL Tablets Developer Guide：`https://openldocs.readthedocs.io/en/latest/documentation/guides/developer_guide`

弱参考：

- AIR Engine 发布报道：`https://www.cfbond.com/2024/06/25/wap_991052868.html`
- AIR Engine 发布报道：`https://www.bianews.com/news/details?id=188342`

AIR Engine 的公开 API 手册、SDK 文档和可信开源仓库未找到。因此 AIR Engine 仅用于产品方向参考，不作为接口或实现依据。

补充资料矩阵：

- 风控规则引擎资料证据矩阵：`docs/superpowers/specs/2026-06-07-risk-control-reference-matrix.md`

生产契约：

- 风控规则引擎生产契约：`docs/superpowers/specs/2026-06-07-risk-control-contracts.md`

需求追踪与验收：

- 风控规则引擎需求追踪矩阵：`docs/superpowers/specs/2026-06-07-risk-control-traceability-matrix.md`

生产运行手册：

- 风控规则引擎生产运行手册：`docs/runbooks/risk-control-rule-engine.md`

### 2.1 Zeus 参考评估

Zeus 的公开资料足以作为工程架构和产品治理参考，但不足以作为接口兼容或源码复刻依据。它提供的是风控规则平台从硬编码策略演进为平台化规则服务的设计经验，而不是可直接实现的 API、SDK、数据库表或完整算法。

可强参考的部分：

- 接入模型：从业务系统内函数演化为独立服务，通过用户中心、商户中心、订单中心、收银台等通用节点统一接入，同时保留独立服务接口给不经过通用节点的业务调用。
- 规则分层：以场景承载策略集合，通过规则组聚类复用规则，规则由因子组成，因子封装数据获取、加工和计算逻辑。这个模型适合 Canvas 将风控节点放在发布、触达、权益发放、交易和 AI 动作前。
- 因子体系：扩展函数、累计因子、决策表因子、名单库因子、工具因子都应内化为平台能力，而不是让业务方用脚本拼接。
- 策略验证：标记、双跑、回溯是生产上线前的核心机制。标记和双跑在线执行但不影响真实决策，回溯使用历史数据验证命中和误伤。
- 规则分析：实时日志查询、规则执行详情、执行流程回放、命中分析和版本对比必须是策略工作台的一部分。
- 防错治理：业务高峰期封版、上线前强制逻辑测试、生产变更强制双跑、验证结果强制回填等机制应作为默认治理策略。
- 运行时经验：Zeus 运行规则基于 Aviator 表达式引擎，公开资料提到规则数量超过万条、单机每天请求量达到百万级，并暴露了表达式编译产生 ClassLoader 和 CodeCache 压力的问题。因此 Canvas 不应让 Aviator 成为全部热路径规则的唯一承载方式，而应使用结构化 AST 作为主引擎，并对表达式层做编译缓存、容量、TTL、超时和类加载风险控制。

不可直接复用的部分：

- 没有公开 API 契约、SDK、数据库 schema、控制台交互细节和完整执行算法。
- 没有公开因子计算服务、累计服务、名单服务、回溯服务的内部实现。
- 公开资料来自 2020 年前后的工程实践，需要结合当前 Java 21、隐私合规、多租户隔离、模型治理和 AI 生成规则治理重新设计。
- 同步页可验证正文，原美团技术团队页面当前不可直接访问，因此引用时应注明可验证来源和历史原文地址的可访问性限制。

对本设计的落地影响：

- `risk_scene`、`risk_strategy_version`、`risk_rule_group`、`risk_rule`、`risk_factor_definition` 必须是独立元数据对象，不能只存一份脚本。
- `MARK`、`SHADOW`、`DUAL_RUN`、`SIMULATION`、`CANARY`、`ENFORCE` 必须成为策略版本状态或运行模式，而不是临时开关。
- 因子目录需要明确 `EXTENSION_FUNCTION`、`REALTIME_AGG`、`DECISION_TABLE`、`LIST_LOOKUP`、`TOOL_FACTOR`、`MODEL_SCORE` 等类型。
- 在线执行层必须记录可回放 trace，包括入参快照、特征快照、规则组执行顺序、命中规则、缺失特征、动作合并过程和 fail policy。
- 表达式引擎只作为受控表达式子层，第一版热路径以结构化 DSL + Java AST evaluator 为主。

## 3. 产品定位

系统定位为 `Risk Control Platform`，包含三层：

1. `Decision Runtime`：在线决策引擎，提供毫秒级 API/MQ/Canvas 节点决策。
2. `Strategy Studio`：运营和策略团队配置规则、名单、评分、决策表、灰度、仿真、发布审批。
3. `Risk Intelligence`：特征库、风险评分、团伙图谱、模型接入、AI 规则助手和复盘分析。

它与 Canvas 的关系：

- Canvas 调用风控规则引擎做发布前检查、触达前检查、权益发放前检查、AI 动作前检查和运营异常拦截。
- 风控规则引擎复用 Canvas 已有租户、审计、任务、通知、BI/Doris/Flink/Redis/RocketMQ 能力。
- Canvas 不直接解释复杂风控规则，只通过稳定的 `risk_decision` 接口消费决策结果。

## 4. 用户角色

### 4.1 平台管理员

管理租户、全局能力开关、决策动作、模型接入、特征源、系统 SLO、审计策略和高危发布权限。

### 4.2 风控策略专家

创建场景、规则组、规则、决策表、名单、评分卡、灰度策略和回溯任务。

### 4.3 运营人员

查看风险命中、处理人工复核、导入名单、调整低风险阈值、查看仿真报告，但不能跳过高危发布审批。

### 4.4 数据开发者

注册特征、维护实时/离线特征计算、配置特征质量规则、管理样本集和回溯数据。

### 4.5 审批人/合规

审查高危规则、模型阈值、名单批量导入、权益阻断策略、AI 生成规则和跨租户数据使用。

### 4.6 外部调用方

通过 API/MQ/SDK/Canvas 节点提交风险事件并消费决策。

## 5. 场景范围

第一批生产场景：

1. 营销权益风控：优惠券、积分、红包、折扣、会员权益。
2. 触达风控：短信、邮件、Push、私域消息、WhatsApp/RCS/WebChat。
3. 账号与设备风控：注册、登录、资料修改、批量异常行为。
4. 交易/支付前置风控：下单、支付、退款、拒付、套现。
5. 内容与舆情风控：营销素材发布、监控 webhook、AI 生成内容。
6. AI 决策护栏：下一步动作推荐、智能回复、自动投放、模型输出拦截。

后续扩展：

- 产业链对公风控。
- 信贷/授信风控。
- 图谱团伙分析。
- 跨商户联合风控。

## 6. 核心能力范围

### 6.1 场景与路由

每个风险事件进入系统后，先按 `tenantId + eventType + channel + businessLine + riskSceneKey` 路由到一个或多个策略版本。

场景配置包含：

- `sceneKey`：如 `MARKETING_BENEFIT_ISSUE`、`MESSAGE_SEND_PRECHECK`。
- `eventSchemaKey`：输入事件 schema。
- `defaultMode`：`ENFORCE`、`MARK`、`SHADOW`、`SIMULATION_ONLY`。
- `failPolicy`：`FAIL_OPEN`、`FAIL_REVIEW`、`FAIL_CLOSED`。
- `latencyBudgetMs`。
- `requiredFeatures`。
- `allowedActions`。
- `owner`、`approverGroup`、`riskLevel`。

### 6.2 规则组

规则组用于复用和组合。参考 Zeus 的规则组思想，规则组不是简单文件夹，而是可独立灰度、版本化、仿真和复用的策略单元。

规则组类型：

- `GUARDRAIL`：底线规则，如黑名单、高风险分强拦截。
- `SCORING`：累加风险分。
- `DECISION_TABLE`：表格化决策。
- `SEGMENT`：人群/标签匹配。
- `MODEL_GATE`：模型分数阈值和解释。
- `LIST_GATE`：名单命中。
- `RATE_LIMIT`：频率/金额/次数累计。
- `CUSTOM_FACTOR`：已批准的函数因子。

规则组配置：

- 顺序执行或并行执行。
- 命中策略：`FIRST_HIT`、`ALL_MATCHED`、`ANY_MATCHED`、`WEIGHTED_SCORE`、`DECISION_TABLE_HIT`。
- 输出：风险分增量、风险标签、解释、推荐动作、证据。

### 6.3 规则 DSL

规则 DSL 必须结构化，不允许前端提交任意脚本作为在线执行规则。表达式语言仅作为受控因子的一部分。

推荐 DSL：

```json
{
  "logic": "AND",
  "conditions": [
    {
      "left": { "type": "FEATURE", "key": "buyer.fail_count_1d" },
      "op": ">=",
      "right": { "type": "LITERAL", "value": 3 }
    },
    {
      "left": { "type": "FEATURE", "key": "risk.score" },
      "op": ">=",
      "right": { "type": "LITERAL", "value": 85 }
    }
  ],
  "groups": [
    {
      "logic": "OR",
      "conditions": [
        {
          "left": { "type": "FEATURE", "key": "device.change_card_1d" },
          "op": ">",
          "right": { "type": "LITERAL", "value": 2 }
        },
        {
          "left": { "type": "LIST", "key": "blacklist.device" },
          "op": "CONTAINS",
          "right": { "type": "FEATURE", "key": "device.id" }
        }
      ]
    }
  ]
}
```

支持运算符：

- 比较：`==`、`!=`、`>`、`>=`、`<`、`<=`
- 字符串：`LIKE`、`STARTS_WITH`、`ENDS_WITH`、`CONTAINS`
- 集合：`IN`、`NOT_IN`、`INTERSECTS`
- 空值：`EXISTS`、`IS_EMPTY`、`IS_NULL`
- 时间：`BEFORE`、`AFTER`、`BETWEEN_TIME`
- 地理/设备：通过注册因子实现，不放入通用 DSL。

### 6.4 因子体系

因子是可复用的计算单元，参考 Zeus 和有赞实践。

因子类型：

- `RAW_FIELD`：事件原始字段。
- `PROFILE_FEATURE`：CDP Profile、标签、人群。
- `REALTIME_AGG`：过去 5 分钟/1 小时/1 天的次数、金额、去重数。
- `OFFLINE_FEATURE`：Doris/Hive/Flink 离线计算特征。
- `LIST_LOOKUP`：黑白灰名单、短期名单。
- `MODEL_SCORE`：模型分数、风险等级、解释。
- `DECISION_TABLE`：表格化多条件决策。
- `GRAPH_FEATURE`：关联设备、IP、账号、卡、收货地址形成的团伙特征。
- `SCRIPTED_SAFE_FUNCTION`：经过审核和沙箱限制的 Aviator/QLExpress 表达式，不允许文件、网络、反射、类加载。

因子必须声明：

- 输入字段。
- 输出类型。
- 缺失值策略。
- 缓存 TTL。
- 成本等级：`O(1)`、`LOCAL_CACHE`、`REDIS`、`DB`、`REMOTE_MODEL`。
- 是否可在线执行。
- 是否可用于仿真。
- 数据敏感级别。

### 6.5 决策动作

标准动作：

- `ALLOW`：放行。
- `REVIEW`：进入人工复核。
- `VERIFY`：要求二次验证或更强校验，如 3DS、人脸、短信、人工确认。
- `BLOCK`：拒绝。
- `DELAY`：延迟处理。
- `LIMIT`：限额、限频、降级。
- `SHADOW_ONLY`：仅记录命中，不影响业务。

动作优先级默认：

`BLOCK > VERIFY > REVIEW > LIMIT > DELAY > ALLOW`

名单优先级：

1. 高优白名单可绕过普通拒绝规则，但不能绕过法规/合规强拦截。
2. 黑名单优先于规则。
3. 灰名单只提升风险分或进入复核。
4. 所有名单变更需记录来源、有效期、操作人、审批单。

### 6.6 风险评分

风险评分采用 0 到 100 分，参考蚁盾和 Antom 文档：

- `< 50`：低风险。
- `50-84`：中风险。
- `>= 85`：高风险。

评分来源：

- 规则组分数叠加。
- 评分卡。
- ML 模型分数。
- 名单和强规则。
- 场景权重。

评分输出必须包含：

- `score`
- `riskBand`
- `decision`
- `reasons[]`
- `matchedRules[]`
- `riskFactors[]`
- `labels[]`
- `explainabilityVersion`

老蚁盾风险评分 API 的 `score + infocode + label` 可以作为外部评分响应结构参考，但内部需要更完整的审计字段。

### 6.7 名单管理

名单类型：

- 用户 ID。
- 手机号。
- 邮箱。
- 设备 ID。
- IP。
- 支付卡指纹。
- 收货地址。
- 社交账号。
- 商户/门店。
- Canvas ID。
- 渠道账号。

名单能力：

- 黑名单、白名单、灰名单、观察名单。
- 批量导入、导出、审批、过期时间。
- 命中记录。
- 过去 7 天/30 天命中趋势。
- 自动名单：从团伙分析、拒付反馈、模型高置信输出自动生成候选，需审批后生效。

### 6.8 决策表

决策表用于多条件、多动作、多输出策略，参考 DMN/OpenL：

- 表头定义输入变量、类型、可选值、区间。
- 行定义条件组合。
- 输出列定义动作、风险分、标签、原因、限额。
- 支持 hit policy：`FIRST`、`UNIQUE`、`COLLECT`、`PRIORITY`。
- 支持 Excel/CSV 导入，但导入后转换为内部结构化模型。
- 导入必须做冲突检测、覆盖率检测、空行检测、重复行检测。

### 6.9 仿真、回溯、标记、双跑

生产级上线流程必须包含：

- `SIMULATION`：基于历史事件回放，输出命中率、拒绝率、误伤样本、资损拦截预估、影响渠道和人群。
- `MARK`：Zeus 式标记模式，候选规则在线执行并记录命中细节，但不影响返回给上游的真实动作。
- `SHADOW`：线上执行但不影响真实决策，只记录命中和建议动作。
- `DUAL_RUN`：新旧策略同时执行，比较动作差异。
- `CANARY`：按租户、场景、人群、流量比例灰度。
- `ENFORCE`：强制生效。

仿真报告指标：

- 总事件数。
- 规则命中数和命中率。
- 动作分布。
- 新旧策略差异。
- 高价值用户误拦风险。
- 权益节省/资损拦截预估。
- 模型特征缺失率。
- P50/P95/P99 估算延迟。

### 6.10 风险实验室

风险实验室是策略发布前的分析工作台：

- 选择历史窗口和样本来源。
- 执行规则回溯。
- 比较版本。
- 查看命中样本。
- 查看规则贡献度。
- 导出报告。
- 一键发起发布审批。

### 6.11 团伙分析

第一阶段只做可解释的关联图谱，不做复杂深度图算法：

- 节点：用户、设备、IP、手机号、邮箱、支付介质、收货地址、商户。
- 边：共享设备、共享 IP、共享收货地址、同卡、多账号同设备、同手机号。
- 特征：关联用户数、关联失败次数、关联高风险标签数、资金/权益金额。
- 输出：团伙 ID、风险摘要、成员列表、建议名单、建议规则。

第二阶段接入图计算：

- 使用 Flink/Doris 或图数据库生成社区特征。
- 支持欺诈团伙聚类。
- 支持 AI 摘要，但 AI 输出只能作为建议，不能直接强制拦截。

### 6.12 AI Copilot

AI Copilot 只做辅助，不自动发布：

- 自然语言生成规则草稿。
- 根据历史命中推荐阈值。
- 解释规则命中原因。
- 生成仿真报告摘要。
- 提醒规则冲突、过宽、过窄、长期零命中。

AI 生成内容必须：

- 标记来源为 `AI_ASSISTED`。
- 强制仿真。
- 强制人工审批。
- 记录 prompt 摘要、模型版本、输出规则 diff。

## 7. 推荐技术选型

### 7.1 总体原则

最优选型不是使用最重的 BRMS，而是按在线风控特性拆分：

- 在线热路径使用自研结构化 DSL + AST 编译执行，保证低延迟、可控、安全和可审计。
- 表格化决策支持 DMN/OpenL 风格，但内部仍转为平台统一模型。
- 复杂脚本只允许受控表达式，不允许在线执行任意 Groovy。
- 实时特征由 Flink/RocketMQ/Redis 提供，离线回溯由 Doris 提供。
- Drools/DMN 可作为决策表导入、兼容和非热路径实验能力，不作为第一版在线核心。

### 7.2 在线规则核心

选择：自研 `RiskRuleAst` + Java evaluator + optional AviatorScript safe expression。

原因：

- 结构化 DSL 更容易做权限、审计、仿真、SQL/stream 编译、UI 可视化。
- 低延迟热路径不依赖复杂 Rete 网络和大量对象插入。
- 当前 Canvas 已有 Java、MySQL、Redis、RocketMQ、Doris、Flink 基础。
- 可渐进兼容现有人群规则和 Canvas 条件节点。

### 7.3 表达式层

选择：AviatorScript 作为受控表达式引擎；QLExpress 作为后续可选兼容引擎。

约束：

- 只允许白名单函数。
- 禁止反射、系统调用、网络、文件、类加载。
- 编译缓存有最大容量和 TTL。
- 单次执行超时默认 20ms。
- 输出大小有限制。

### 7.4 决策表

选择：内部决策表模型 + DMN/OpenL/Excel 导入适配。

原因：

- 业务人员需要表格化规则。
- DMN/OpenL 文档证明决策表是成熟的企业决策表达方式。
- 直接运行外部 Excel/DMN 不利于统一审计和在线性能控制。

### 7.5 特征计算

选择：

- 事件总线：RocketMQ。
- 实时计算：Flink。
- 热特征：Redis。
- 离线和回溯：Doris。
- 元数据：MySQL。
- 本地缓存：Caffeine。

目标：

- O(1) 或 Redis 特征用于在线强拦截。
- DB/远程模型特征默认不进入强实时路径，除非有缓存和超时保护。

### 7.6 模型服务

选择：模型注册表 + HTTP/gRPC model gateway。

要求：

- 模型版本化。
- 输入输出 schema。
- 超时和熔断。
- fallback 策略。
- 分数校准。
- 解释字段。
- 模型 drift 监控。

### 7.7 观测

选择：OpenTelemetry + Prometheus + Grafana + 结构化审计日志。

必须观测：

- 决策请求 QPS。
- P50/P95/P99 延迟。
- 规则命中率。
- 动作分布。
- 特征缺失率。
- Redis/Flink/Doris 延迟。
- 失败率和 fallback 次数。
- 发布/审批/名单操作审计。

## 8. 系统架构

```
External Event/API/Canvas Node/MQ
        |
        v
Risk Decision Gateway
        |
        v
Scene Router ---- Strategy Version Store
        |
        v
Feature Resolver ---- Redis/Caffeine/CDP/Profile/Model Gateway
        |
        v
Rule Group Executor ---- Rule AST / Decision Table / List / Scorecard / Model Gate
        |
        v
Decision Merger ---- Action Policy / Priority / Fail Policy
        |
        v
Decision Ledger ---- Trace / Audit / Metrics / Feedback
        |
        v
Caller: ALLOW / REVIEW / VERIFY / BLOCK / LIMIT / DELAY
```

后台：

```
Strategy Studio
  -> Rule Authoring
  -> Variable Dictionary
  -> List Management
  -> Risk Lab
  -> Approval
  -> Monitoring

Data/Intelligence
  -> RocketMQ events
  -> Flink real-time features
  -> Redis feature store
  -> Doris replay store
  -> Model Gateway
```

## 9. 核心数据模型

### 9.1 元数据表

`risk_scene`

- `id`
- `tenant_id`
- `scene_key`
- `name`
- `event_schema_key`
- `status`
- `default_mode`
- `fail_policy`
- `latency_budget_ms`
- `owner`
- `created_at`
- `updated_at`

`risk_strategy`

- `id`
- `tenant_id`
- `scene_key`
- `strategy_key`
- `name`
- `status`
- `active_version`
- `draft_version`
- `risk_level`
- `owner`

`risk_strategy_version`

- `id`
- `tenant_id`
- `strategy_key`
- `version`
- `mode`
- `traffic_percent`
- `compiled_hash`
- `definition_json`
- `created_by`
- `approved_by`
- `approved_at`
- `effective_from`
- `effective_to`

`risk_rule_group`

- `id`
- `tenant_id`
- `strategy_version_id`
- `group_key`
- `group_type`
- `execution_order`
- `match_policy`
- `enabled`
- `definition_json`

`risk_rule`

- `id`
- `tenant_id`
- `group_key`
- `rule_key`
- `priority`
- `mode`
- `dsl_json`
- `action`
- `score_delta`
- `reason_code`
- `enabled`

`risk_factor_definition`

- `id`
- `tenant_id`
- `factor_key`
- `factor_type`
- `value_type`
- `source`
- `online_allowed`
- `cache_ttl_seconds`
- `missing_policy`
- `sensitivity_level`
- `owner`

`risk_decision_table`

- `id`
- `tenant_id`
- `table_key`
- `version`
- `hit_policy`
- `input_columns_json`
- `output_columns_json`
- `rows_json`
- `compiled_hash`

`risk_list`

- `id`
- `tenant_id`
- `list_key`
- `list_type`
- `subject_type`
- `status`
- `requires_approval`
- `owner`

`risk_list_entry`

- `id`
- `tenant_id`
- `list_key`
- `subject_hash`
- `subject_masked`
- `reason`
- `source`
- `effective_from`
- `expires_at`
- `created_by`
- `approval_id`

`risk_model_registry`

- `id`
- `tenant_id`
- `model_key`
- `model_version`
- `endpoint`
- `input_schema_json`
- `output_schema_json`
- `timeout_ms`
- `fallback_policy`
- `status`

### 9.2 运行时表

`risk_decision_run`

- `id`
- `tenant_id`
- `request_id`
- `scene_key`
- `strategy_key`
- `strategy_version`
- `subject_key`
- `decision`
- `score`
- `risk_band`
- `mode`
- `latency_ms`
- `status`
- `created_at`

`risk_decision_trace`

- `id`
- `tenant_id`
- `decision_run_id`
- `step_type`
- `step_key`
- `matched`
- `output_json`
- `latency_ms`
- `error_summary`

`risk_rule_hit`

- `id`
- `tenant_id`
- `decision_run_id`
- `rule_key`
- `group_key`
- `action`
- `score_delta`
- `reason_code`
- `evidence_json`

`risk_simulation_run`

- `id`
- `tenant_id`
- `strategy_key`
- `candidate_version`
- `baseline_version`
- `sample_source`
- `sample_window_start`
- `sample_window_end`
- `status`
- `summary_json`
- `created_by`

`risk_approval`

- `id`
- `tenant_id`
- `resource_type`
- `resource_key`
- `resource_version`
- `risk_level`
- `status`
- `requested_by`
- `reviewed_by`
- `reason`

### 9.3 事件 schema

事件统一字段：

```json
{
  "requestId": "risk-req-001",
  "tenantId": 7,
  "sceneKey": "MARKETING_BENEFIT_ISSUE",
  "subject": {
    "userId": "u-123",
    "deviceId": "d-456",
    "ip": "10.0.0.1"
  },
  "eventTime": "2026-06-06T12:00:00Z",
  "event": {
    "amount": 100,
    "channel": "APP",
    "couponCode": "WELCOME50"
  },
  "context": {
    "canvasId": 42,
    "nodeId": "coupon_1",
    "executionId": "exec-9"
  }
}
```

## 10. API 设计

### 10.1 在线决策

`POST /canvas/risk/decisions/evaluate`

请求：

```json
{
  "requestId": "risk-req-001",
  "sceneKey": "MARKETING_BENEFIT_ISSUE",
  "subject": {
    "userId": "u-123",
    "deviceId": "d-456",
    "ip": "10.0.0.1"
  },
  "event": {
    "amount": 100,
    "couponCode": "WELCOME50"
  },
  "context": {
    "canvasId": 42,
    "nodeId": "coupon_1"
  },
  "options": {
    "mode": "ENFORCE",
    "includeTrace": true
  }
}
```

响应：

```json
{
  "requestId": "risk-req-001",
  "decision": "REVIEW",
  "score": 78,
  "riskBand": "MEDIUM",
  "strategyVersion": 12,
  "reasons": [
    {
      "code": "HIGH_FAIL_COUNT",
      "message": "User failed payment count in 1 day is high"
    }
  ],
  "matchedRules": [
    {
      "groupKey": "payment_velocity",
      "ruleKey": "fail_count_1d_gte_3",
      "action": "REVIEW",
      "scoreDelta": 30
    }
  ],
  "labels": ["PAYMENT_VELOCITY_RISK"],
  "latencyMs": 24
}
```

### 10.2 策略管理

- `GET /canvas/risk/scenes`
- `POST /canvas/risk/scenes`
- `GET /canvas/risk/strategies`
- `POST /canvas/risk/strategies/{strategyKey}/draft`
- `POST /canvas/risk/strategies/{strategyKey}/validate`
- `POST /canvas/risk/strategies/{strategyKey}/simulation-runs`
- `POST /canvas/risk/strategies/{strategyKey}/submit-approval`
- `POST /canvas/risk/strategies/{strategyKey}/versions/{version}/activate`
- `POST /canvas/risk/strategies/{strategyKey}/versions/{version}/rollback`

### 10.3 名单

- `GET /canvas/risk/lists`
- `POST /canvas/risk/lists`
- `POST /canvas/risk/lists/{listKey}/entries`
- `POST /canvas/risk/lists/{listKey}/entries/import`
- `DELETE /canvas/risk/lists/{listKey}/entries/{entryId}`
- `GET /canvas/risk/lists/{listKey}/hits`

### 10.4 风险实验室

- `POST /canvas/risk/lab/simulations`
- `GET /canvas/risk/lab/simulations/{runId}`
- `GET /canvas/risk/lab/simulations/{runId}/samples`
- `GET /canvas/risk/lab/strategy-diff`

### 10.5 运营复核

- `GET /canvas/risk/review-cases`
- `POST /canvas/risk/review-cases/{caseId}/approve`
- `POST /canvas/risk/review-cases/{caseId}/reject`
- `POST /canvas/risk/review-cases/{caseId}/add-to-list`

## 11. Canvas 集成

### 11.1 风控节点

新增 `RISK_DECISION` 节点：

```json
{
  "sceneKey": "MARKETING_BENEFIT_ISSUE",
  "subjectMapping": {
    "userId": "${userId}",
    "deviceId": "${deviceId}",
    "ip": "${ip}"
  },
  "eventMapping": {
    "couponCode": "${couponCode}",
    "amount": "${couponAmount}"
  },
  "routes": {
    "ALLOW": "send_coupon",
    "REVIEW": "manual_review",
    "VERIFY": "verify_user",
    "BLOCK": "end_blocked"
  },
  "failPolicy": "FAIL_REVIEW"
}
```

### 11.2 发布前风控

Canvas 发布审批接入风险策略：

- 无上限权益。
- 高额券。
- 含 AI 自动动作。
- 含批量触达。
- 缺少频控。
- 缺少退出条件。
- 命中高风险模板。

### 11.3 权益发放保护

优惠券、积分、红包、会员权益节点在执行前可调用风控决策。

默认策略：

- 高风险用户 BLOCK。
- 中风险 REVIEW。
- 白名单 ALLOW。
- 特征缺失 FAIL_REVIEW。

### 11.4 消息触达保护

消息触达前检查：

- 用户退订/黑名单。
- 频率过高。
- 内容风险。
- 渠道异常。
- 私域会话状态。

## 12. 发布治理

发布状态：

- `DRAFT`
- `VALIDATED`
- `SIMULATED`
- `APPROVAL_PENDING`
- `MARK`
- `SHADOW`
- `DUAL_RUN`
- `CANARY`
- `ACTIVE`
- `PAUSED`
- `ROLLED_BACK`
- `ARCHIVED`

高危变更必须审批：

- `BLOCK` 动作影响超过 1% 历史样本。
- 风险分阈值降低。
- 白名单批量导入。
- 黑名单批量导入。
- AI 生成规则。
- 强制拦截权益发放。
- fail policy 设为 `FAIL_CLOSED`。

自动阻断发布：

- 规则解析失败。
- 引用未知变量。
- 引用在线不可用因子。
- 决策表冲突。
- 仿真样本不足。
- 命中率异常高。
- 缺少审批。

## 13. 安全与合规

### 13.1 租户隔离

所有表必须带 `tenant_id`，所有 API 必须从认证上下文解析租户，不接受前端覆盖。

### 13.2 PII

- 手机号、邮箱、卡号、地址只存 hash 和 masked 值。
- 明文只允许在一次请求内短暂存在，不写日志。
- 导入名单时服务端计算 hash。
- 审计日志展示 masked 值。

### 13.3 权限

权限动作：

- `RISK_SCENE_VIEW`
- `RISK_STRATEGY_EDIT`
- `RISK_STRATEGY_PUBLISH`
- `RISK_LIST_EDIT`
- `RISK_LIST_IMPORT`
- `RISK_SIMULATION_RUN`
- `RISK_CASE_REVIEW`
- `RISK_ADMIN`

### 13.4 审计

审计事件：

- 场景创建/修改。
- 规则创建/修改/删除。
- 决策表导入。
- 名单导入/删除。
- 仿真执行。
- 发布审批。
- 策略激活/回滚。
- 人工复核。
- AI 规则生成。

### 13.5 运行时安全

- 外部调用必须有签名或 JWT。
- 请求体大小限制。
- 超时限制。
- 熔断和限流。
- 重放保护：`tenantId + requestId` 幂等。
- 结构化错误，不泄露内部规则内容。

## 14. 性能与 SLO

### 14.1 在线决策

默认 SLO：

- P50 < 20ms。
- P95 < 50ms。
- P99 < 100ms。
- 可用性 99.9%。
- 单租户默认 500 QPS，可配置。

复杂策略 SLO：

- P95 < 100ms。
- P99 < 200ms。
- 远程模型调用必须设置独立超时，默认 30ms。

### 14.2 特征读取

特征读取目标：

- 本地缓存 < 1ms。
- Redis < 5ms。
- 模型网关 < 30ms。
- 不允许在线请求直接查询 Doris/MySQL 大表。

### 14.3 编译缓存

策略激活时预编译：

- DSL AST。
- 决策表索引。
- 因子依赖图。
- 规则引用变量集合。
- 命中解释模板。

运行时仅读取已发布编译快照。

## 15. 观测与运营

仪表盘：

- 请求量。
- 延迟。
- 决策动作分布。
- 场景命中率。
- 规则命中率。
- 特征缺失率。
- 规则版本差异。
- 仿真结果。
- 人工复核 backlog。
- 名单命中趋势。
- 模型分数分布。

告警：

- P95 超过预算。
- `BLOCK` 比例突增。
- `ALLOW` 比例突增。
- 特征缺失突增。
- 模型调用失败。
- Redis 延迟异常。
- 仿真任务失败。
- 高危规则未经审批激活。

## 16. 测试策略

### 16.1 单元测试

- DSL 解析。
- 操作符语义。
- 缺失值策略。
- 决策表 hit policy。
- 规则优先级。
- 动作合并。
- 名单优先级。
- 分数计算。

### 16.2 集成测试

- 在线决策 API。
- 策略发布和回滚。
- 名单导入和命中。
- 仿真回溯。
- Canvas 风控节点路由。
- 权限和审计。

### 16.3 性能测试

- 纯规则 1000 条。
- 规则组 20 个。
- 名单 100 万条。
- Redis 特征读取。
- 1000 QPS 持续压测。
- 规则发布不中断在线请求。

### 16.4 安全测试

- 租户越权。
- PII 泄露。
- 任意表达式注入。
- 决策表导入恶意 payload。
- 重放攻击。
- 超大请求。

## 17. 分阶段交付

### Phase 1: Foundation

目标：可用的在线规则决策内核。

范围：

- 场景、策略、版本、规则组、规则表。
- 结构化 DSL。
- AST evaluator。
- 名单管理。
- 在线决策 API。
- 决策日志和命中日志。
- 基础权限和审计。

验收：

- 单场景可创建规则并在线决策。
- 规则发布不重启服务。
- 命中结果可追溯。

### Phase 2: Risk Studio

目标：运营可配置。

范围：

- 规则编辑器。
- 变量字典。
- 名单 UI。
- 决策表 UI。
- 发布审批。
- 规则版本 diff。

验收：

- 运营可创建规则、提交审批、发布、回滚。
- 高危变更不能绕过审批。

### Phase 3: Simulation and Governance

目标：生产上线前可验证。

范围：

- 风险实验室。
- 历史回放。
- 标记、双跑、灰度。
- 仿真报告。
- 命中样本分析。

验收：

- 新规则可在历史样本上回测。
- 可双跑新旧版本并比较差异。

### Phase 4: Feature Platform

目标：实时/离线特征稳定供给。

范围：

- 特征字典。
- Flink 实时特征。
- Redis 热特征。
- Doris 离线样本。
- 特征质量监控。

验收：

- 在线决策可读取实时累计特征。
- 特征缺失和延迟可观测。

### Phase 5: Model and Graph Intelligence

目标：规则、模型、图谱协同。

范围：

- 模型注册表。
- 模型网关。
- 风险评分融合。
- 团伙图谱。
- AI Copilot。

验收：

- 模型分数可进入规则。
- 团伙分析可生成名单建议。
- AI 规则草稿必须审批后生效。

## 18. 非目标

第一版不做：

- 完整复刻 AIR Engine。
- 跨租户联合建模。
- 无审批自动发布 AI 规则。
- 在线热路径直接执行任意 Groovy。
- 在线直接查询大表。
- 完整图数据库平台。

## 19. 风险与缓解

| 风险 | 缓解 |
| --- | --- |
| 规则误拦导致业务损失 | 强制仿真、标记、双跑、灰度、审批、快速回滚 |
| 特征缺失导致判断错误 | 缺失策略、特征质量监控、fail policy |
| 规则过多导致延迟失控 | 编译缓存、成本等级、在线因子白名单、性能压测 |
| PII 泄露 | hash/masked 存储、审计脱敏、权限隔离 |
| AI 生成规则不可靠 | 只生成草稿、强制仿真和审批 |
| 决策表冲突 | 导入校验、hit policy、覆盖率分析 |
| 模型漂移 | 分布监控、阈值告警、版本回滚 |

## 20. 成功标准

系统达到生产级的最低标准：

1. 支持至少 5 个风险场景。
2. 支持规则、规则组、名单、决策表、评分。
3. 支持在线决策 API 和 Canvas 风控节点。
4. 支持规则版本、发布审批、回滚。
5. 支持命中日志、决策日志、审计日志。
6. 支持历史仿真、标记、双跑、灰度。
7. P95 在线决策低于 50ms，不含远程模型。
8. 租户隔离、PII 脱敏、权限边界通过测试。
9. 生产仪表盘和告警覆盖核心 SLO。
10. 有完整回滚和事故处理 runbook。
