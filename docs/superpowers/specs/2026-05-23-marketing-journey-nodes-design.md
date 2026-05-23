# 营销旅程全量节点体系设计

## 背景

当前项目已经具备营销画布的基础执行能力：节点注册表、DAG 解析、Handler 调度、执行轨迹、触发路由、定时触发、人群圈选、AB 分流、API 调用、MQ、券、触达平台、子流程和灰度发布。

这套能力偏“技术画布”：能编排业务节点，但营销旅程中最关键的目标退出、等待事件、触达保护、渠道可达性、频控、触达渠道产品化、实验统计和循环策略还不完整。

本设计目标是把系统升级为完整营销旅程系统。用户明确要求“不隐藏、不计成本做”，因此新增节点全部作为可用产品能力进入节点库；对暂时依赖外部系统的能力，通过稳定适配器接口和本地基础数据模型落地，不做纯展示占位。

## 目标

1. 建立统一节点执行协议，让所有节点都能表达 `success / fail / timeout / suppressed / skipped / pending` 等结果。
2. 新增完整营销旅程节点，不再只依赖通用 `API_CALL` 或 `REACH_PLATFORM` 表达运营动作。
3. 将触达前保护能力内建为一等节点，避免黑名单、退订、静默时段、渠道不可达和过度触达事故。
4. 支持等待事件、目标达成、循环、跳转、合并、实验、评分、推荐和旅程转移。
5. 保持现有画布兼容，已发布或已有草稿中的旧节点继续可加载、可执行。
6. 前端节点库直接展示全部能力，不通过隐藏未完成节点规避复杂度。

## 非目标

1. 不把系统拆成多个微服务。本轮仍在当前模块化单体内完成。
2. 不允许无边界无限循环。Loop / GoTo 必须有最大次数、退出条件或发布期可证明的终止约束。
3. 不废弃现有节点类型。`DELAY`、`REACH_PLATFORM`、`IN_APP_NOTIFY` 等保留兼容，但产品推荐使用新节点。

## 总体方案

采用“先协议、后节点、再高级控制流”的实施路线。

### 方案对比

#### 方案 A：直接逐个加节点

优点是短期看起来进展快。缺点是每个新节点都要重复处理分支、失败、超时、跳过、摘要、前端 handle 和 trace，后期会形成大量特殊逻辑。

#### 方案 B：先升级节点协议，再批量接入节点

优点是一次解决通用出口、通用策略、前端动态 handle、发布校验和 trace 语义。后续节点只实现业务判断或动作。缺点是第一阶段改动较大。

#### 方案 C：完全重构为工作流引擎

优点是理论模型更统一。缺点是会推翻当前已经可用的 DAG 引擎、节点注册表、前端画布和执行轨迹，成本高且风险集中。

采用 **方案 B**。它最大化利用现有工程基础，同时能承载全量节点体系。

## 节点执行协议

### NodeResult V2

新增通用执行结果模型，同时兼容现有 `NodeResult` 字段。

```json
{
  "outcome": "SUCCESS",
  "routes": {
    "success": "node_next",
    "fail": "node_error",
    "timeout": "node_timeout",
    "suppressed": "node_suppressed",
    "skipped": "node_after_skip"
  },
  "output": {},
  "reasonCode": "OK",
  "reasonMessage": "",
  "pending": false,
  "resumeAt": null
}
```

支持的 `outcome`：

- `SUCCESS`：正常完成
- `FAIL`：执行失败
- `TIMEOUT`：等待或动作超时
- `SUPPRESSED`：被合规、退订、黑名单、频控或渠道不可达拦截
- `SKIPPED`：节点被配置跳过
- `PENDING`：等待外部事件、审批或时间恢复

### 通用节点策略

每个节点支持 `runtimePolicy`：

```json
{
  "enabled": true,
  "skip": false,
  "timeout": {
    "enabled": true,
    "seconds": 600,
    "onTimeout": "TIMEOUT_BRANCH"
  },
  "errorBranch": {
    "enabled": true,
    "targetNodeId": "node_error"
  },
  "idempotency": {
    "enabled": true,
    "keyTemplate": "{{journey.id}}:{{execution.id}}:{{node.id}}"
  }
}
```

发布校验要求：

- `skip=true` 时仍可保留连线，但执行直接走 `skipped` 或默认后继。
- 有副作用节点必须启用幂等，或显式选择“允许重复执行”。
- 有 `timeout` 出口的节点必须在前端展示可连线 handle。

### 节点注册表扩展

`node_type_registry` 新增字段：

- `outlet_schema`：节点出口定义，驱动前端 handle 和发布校验。
- `summary_template`：节点摘要模板，驱动画布卡片可读信息。
- `runtime_policy_schema`：该节点允许的通用策略项。
- `risk_level`：发布审批、风险提示和操作审计使用。

示例：

```json
[
  { "id": "success", "label": "通过", "outcome": "SUCCESS", "required": false },
  { "id": "suppressed", "label": "被抑制", "outcome": "SUPPRESSED", "required": false },
  { "id": "timeout", "label": "超时", "outcome": "TIMEOUT", "required": false }
]
```

前端不再只维护硬编码分支 handle。`IF_CONDITION`、`AB_SPLIT`、`TAGGER` 等旧节点先通过兼容映射继续工作，新节点走 `outlet_schema`。

## 数据模型

### 等待订阅

新增 `canvas_wait_subscription`：

```sql
CREATE TABLE canvas_wait_subscription (
  id              BIGINT PRIMARY KEY AUTO_INCREMENT,
  execution_id    VARCHAR(64) NOT NULL,
  canvas_id       BIGINT NOT NULL,
  version_id      BIGINT NOT NULL,
  user_id         VARCHAR(128) NOT NULL,
  node_id         VARCHAR(64) NOT NULL,
  wait_type       VARCHAR(32) NOT NULL,
  event_code      VARCHAR(128),
  event_filters   TEXT,
  resume_payload  TEXT,
  expires_at      DATETIME,
  status          VARCHAR(32) NOT NULL,
  created_at      DATETIME NOT NULL,
  updated_at      DATETIME NOT NULL,
  INDEX idx_event_wait (event_code, user_id, status),
  INDEX idx_expire (status, expires_at)
);
```

用途：

- `WAIT_UNTIL_EVENT`
- `GOAL_CHECK` 的异步目标监听
- 超时恢复

### 用户触达基础数据

新增基础表，真实企业系统可通过适配器替换。

- `customer_profile`：用户属性、地区、时区、生命周期字段。
- `customer_channel`：email、phone、push token、wechat openid、渠道可用状态。
- `marketing_consent`：渠道授权、退订状态、授权来源、更新时间。
- `marketing_suppression`：黑名单、风险地区、用户级或渠道级抑制。
- `marketing_frequency_counter`：按用户、渠道、旅程、节点维度计数。
- `message_send_record`：触达发送记录、模板、渠道、状态、外部回执 ID。

这些表不是最终唯一数据源。系统增加 `UserProfileAdapter`、`ConsentAdapter`、`ChannelAdapter`、`ReachAdapter`，本地表是默认实现。

### 事件日志

现有事件定义继续使用。新增事件写入统一入口：

- `POST /canvas/events/report`
- 事件写入 `event_log`
- 同步唤醒 `canvas_wait_subscription`
- 同步触发匹配的 `EVENT_TRIGGER`

## 全量节点清单

### 入口类

#### START

保留当前开始节点。每个画布必须有且只有一个。

#### EVENT_TRIGGER

增强配置：

- `eventCode`
- `eventFilters`
- `userFilters`
- `reentryRule`
- `lateEventPolicy`

`reentryRule` 支持：

- 是否允许重复进入
- 冷却窗口
- 单用户最大进入次数
- 去重 key 模板

#### SCHEDULED_TRIGGER

增强配置：

- 单次执行
- Cron 周期执行
- 用户来源：静态列表、人群、API、SQL 数据源
- jitter
- 并发限制
- 用户时区批处理

#### API_TRIGGER

新增产品化节点。底层兼容当前 `DIRECT_CALL`。

能力：

- 自动生成调用 URL
- 入参 schema
- 鉴权方式
- 示例请求
- 幂等 key 规则

#### AUDIENCE_TRIGGER

监听人群进入或离开。

配置：

- `audienceId`
- `triggerOn`: `ENTER / EXIT`
- `checkFrequency`: `REALTIME / HOURLY / DAILY`
- `reentryRule`

第一阶段通过人群计算完成后的差集检测触发；后续接入实时人群变更流。

#### MQ_TRIGGER

保留并补充属性过滤和重入规则。

### 控制流类

#### CONDITION

保留 `IF_CONDITION`，新增产品名称 `Condition`。支持多条件 AND / OR，后续由规则构建器支持嵌套组。

#### SELECTOR

保留多分支选择器，作为多分支条件节点。

#### RANDOM_SPLIT

新增随机分流节点。

配置：

- 多路径
- 每路径权重
- 分配模式：每次随机或用户稳定随机

#### EXPERIMENT

新增实验节点，不替代现有 `AB_SPLIT`，但作为运营推荐节点。

配置：

- 实验名称
- variants 与权重
- 对照组
- 分配策略
- 目标指标
- 最小样本量
- 置信度
- 自动胜出策略

产出：

- `experimentId`
- `variantId`
- `isControl`

#### WAIT

新增增强等待节点，`DELAY` 保留兼容。

模式：

- `DURATION`：固定时长
- `UNTIL_DATE`：直到指定时间
- `RELATIVE_TIME`：直到用户本地时间或工作日窗口
- `TIME_WINDOW`：只在允许窗口继续，否则延后
- `UNTIL_EVENT`：直到事件发生

`UNTIL_EVENT` 必须配置最大等待时间。超时走 `timeout` 出口。

#### GOAL_CHECK

新增目标检测节点。

模式：

- 同步检测：检查进入旅程以来是否已发生目标事件。
- 异步监听：进入等待状态，目标事件发生后恢复；到期走未达成分支。

出口：

- `goal_met`
- `goal_not_met`
- `timeout`

#### MERGE

新增合并节点。

模式：

- `ANY`：任意路径到达即继续
- `ALL`：等待所有上游到达后继续
- `COUNT`：达到指定数量后继续

底层复用 `HUB / AGGREGATE / THRESHOLD` 的等待能力，但作为产品化节点展示。

#### LOOP

新增循环节点。

发布期硬约束：

- 必须配置 `maxIterations`
- 必须配置 `exitCondition` 或目标节点
- 每轮必须配置间隔，避免同步忙循环

运行时按 `executionId + nodeId` 记录迭代次数。超过上限走 `max_exceeded` 出口。

#### GOTO

新增跳转节点。

允许跳到已有节点，包括上游节点，但必须满足：

- 向后跳转无限制。
- 向前或横向跳转必须配置 `maxJumps`。
- 发布校验会计算潜在循环，没有终止约束则拒绝发布。

#### END

增强结束原因：

- `COMPLETED`
- `CONVERTED`
- `SUPPRESSED`
- `FAILED_HANDLED`
- `TRANSFERRED`

### 动作类

#### SEND_EMAIL

配置：

- 模板
- 主题
- 预览文本
- 发件人
- 变量映射
- UTM
- 静默时段策略
- 退订处理
- 失败分支

#### SEND_SMS

配置：

- 模板
- 签名
- 手机号字段
- 国际短信策略
- 退订文案
- 发送窗口
- fallback 渠道

#### SEND_PUSH

配置：

- 平台
- 标题、正文、图片
- deep link
- collapse key
- ttl
- 优先级
- badge
- fallback

#### SEND_IN_APP

产品化替代当前 `IN_APP_NOTIFY`。保留旧节点可执行。

#### SEND_WECHAT

配置：

- 模板消息
- 小程序页面
- openid 字段
- 订阅消息类型
- fallback

#### WEBHOOK

产品化包装 `API_CALL` 的开放外调场景。

配置：

- URL
- method
- headers
- body 模板
- timeout
- retry
- response mapping
- secret 引用

#### UPDATE_PROFILE

更新用户画像。

操作：

- `SET`
- `SET_IF_NULL`
- `INCREMENT`
- `DECREMENT`
- `APPEND`
- `REMOVE`
- `CLEAR`

#### TAG_OPERATION

添加、移除标签，支持 TTL。

#### POINTS_OPERATION

积分发放或扣减，必须配置幂等 key。

#### CREATE_TASK

创建人工跟进任务，支持指定人、规则分配和轮询分配。

#### TRACK_EVENT

写入标准事件流，可供后续目标、等待和其他旅程触发。

#### TRANSFER_JOURNEY

转移到其他旅程。

配置：

- 目标画布
- 是否携带上下文
- 字段映射
- 是否等待目标旅程完成

#### SUBFLOW

产品化替代 `SUB_FLOW_REF`，支持输入输出映射和版本选择。

### 保护类

#### SUPPRESSION_CHECK

检查：

- 全局黑名单
- 渠道黑名单
- 退订
- 营销授权
- 地区限制
- 风险用户

出口：

- `allowed`
- `suppressed`

#### QUIET_HOURS

检查用户本地静默时段。

策略：

- 通过
- 延后到允许时间
- 直接走 suppressed
- 走 timeout

#### CHANNEL_AVAILABILITY

检查渠道可达性。

支持渠道：

- email
- sms
- push
- in_app
- wechat

出口：

- `available`
- `unavailable`

#### FREQUENCY_CAP

频控节点。

维度：

- 当前旅程
- 当前节点
- 全局
- 渠道
- 自定义 scope

超频策略：

- 退出
- 跳过
- 等待窗口重置
- 走 suppressed 分支

### 决策增强类

#### SCORING

基于规则或外部模型为用户打分。

产出：

- `score`
- `scoreBand`
- `modelVersion`

可直接路由到高、中、低分分支。

#### RECOMMENDATION

推荐商品、内容或下一步动作。

配置：

- 推荐场景
- 候选数量
- 过滤条件
- fallback 策略

产出：

- `recommendation.items`
- `recommendation.strategy`

#### AI_NEXT_BEST_ACTION

高级推荐节点，使用规则 + 模型或外部 AI 服务决定下一步动作。它必须有降级策略，AI 调用失败时走 fallback 分支。

### 结构与 UI 类

#### GROUP

纯 UI 分组，不参与执行。

#### TEMPLATE_NODE

模板节点用于快速生成常见片段。插入后展开为真实节点组合，不作为运行时节点执行。

## 前端设计

### 节点库

节点库展示全部新增节点。每个节点包含：

- 名称
- 分类
- 一句用途摘要
- 风险标记
- 是否有副作用

不隐藏未配置外部系统的节点。若外部通道未配置，节点可拖入画布，发布时给出明确错误。

### 配置面板

配置面板由 `config_schema`、`outlet_schema` 和 `summary_template` 共同驱动。

新增控件：

- 事件过滤规则构建器
- 用户过滤规则构建器
- 时间窗口选择器
- 渠道选择器
- 模板变量映射器
- 频控规则列表
- 目标条件配置器
- 实验 variants 配置器
- 循环退出条件配置器

### 画布节点卡片

卡片摘要显示：

- 关键配置
- 风险等级
- 是否跳过
- 是否有超时
- 是否有保护策略

### 连线

前端根据 `outlet_schema` 渲染 handle。旧节点继续通过兼容函数映射到 outlet schema。

## 后端设计

### Handler 分层

新增四类基础服务：

- `JourneyRuntimeService`：统一处理 NodeResult V2、路由、通用策略。
- `WaitSubscriptionService`：管理等待事件和超时恢复。
- `MarketingPolicyService`：抑制、静默、可达性、频控。
- `ReachDeliveryService`：统一触达发送、回执、幂等和记录。

每个节点 Handler 只关注本节点业务判断或动作，不直接处理通用超时、跳过和错误分支。

### 事件唤醒

事件上报流程：

1. 校验事件定义。
2. 写入事件日志。
3. 查询匹配 `EVENT_TRIGGER` 路由并触发旅程。
4. 查询匹配 `canvas_wait_subscription` 并恢复等待中的执行。
5. 更新目标达成统计。

### 循环与跳转

当前引擎是 DAG。为了实现 Loop / GoTo，新增“受控回边”概念。

发布校验：

- 普通连线仍必须无环。
- `LOOP` 和 `GOTO` 产生的回边单独记录。
- 回边必须有最大次数或退出条件。
- 估算最大节点执行次数，超过系统上限拒绝发布。

运行时：

- `ExecutionContext` 记录 `visitCount[nodeId]` 和 `jumpCount[nodeId]`。
- 每次进入节点先检查访问上限。
- 超限走 `max_exceeded` 或失败。

## 兼容策略

1. 旧 `DELAY` 继续执行，新画布推荐使用 `WAIT`。
2. 旧 `REACH_PLATFORM` 继续执行，新画布推荐使用 `SEND_*` 节点。
3. 旧 `AB_SPLIT` 继续执行，新实验推荐使用 `EXPERIMENT`。
4. 旧 `SUB_FLOW_REF` 和 `CANVAS_TRIGGER` 继续执行，新画布推荐使用 `SUBFLOW` 和 `TRANSFER_JOURNEY`。
5. 旧图 JSON 的连接字段继续支持：`nextNodeId`、`successNodeId`、`failNodeId`、`elseNodeId`、`branches`、`groups`。

## 发布校验

发布时新增校验：

- 必须存在 START。
- 至少存在一个真实入口节点。
- 所有必填配置完整。
- 有副作用节点必须满足幂等要求。
- 触达节点必须有模板或通道配置。
- 保护节点的 suppressed 出口如果未连接，必须选择默认处理策略。
- WAIT_UNTIL_EVENT 必须有最大等待时长。
- GOAL_CHECK 异步监听必须有超时或结束条件。
- LOOP / GOTO 不能形成无边界循环。
- 外部适配器缺失时阻止发布。

## 可观测性

执行轨迹新增字段：

- `outcome`
- `reasonCode`
- `reasonMessage`
- `routeHandle`
- `suppressionReason`
- `waitUntil`
- `iteration`
- `idempotencyKey`

统计新增：

- 节点出口漏斗
- 目标达成率
- 抑制率
- 超频率
- 渠道不可达率
- 触达成功率
- 实验 variant 指标

## 实施顺序

### Phase 1：协议底座

- 扩展 `node_type_registry`
- 实现 NodeResult V2 兼容层
- 实现 outlet schema 驱动 handle
- 通用 runtime policy
- 发布校验升级
- trace 字段升级

### Phase 2：Goal + Wait

- `WAIT`
- `GOAL_CHECK`
- `canvas_wait_subscription`
- 事件上报唤醒
- 超时恢复

### Phase 3：保护节点

- `SUPPRESSION_CHECK`
- `QUIET_HOURS`
- `CHANNEL_AVAILABILITY`
- `FREQUENCY_CAP`
- 用户触达基础表和适配器

### Phase 4：触达节点产品化

- `SEND_EMAIL`
- `SEND_SMS`
- `SEND_PUSH`
- `SEND_IN_APP`
- `SEND_WECHAT`
- `message_send_record`
- 触达回执和失败分支

### Phase 5：入口增强和数据动作

- `API_TRIGGER`
- `AUDIENCE_TRIGGER`
- `EVENT_TRIGGER` 过滤与重入
- `UPDATE_PROFILE`
- `TAG_OPERATION`
- `POINTS_OPERATION`
- `CREATE_TASK`
- `TRACK_EVENT`

### Phase 6：高级决策

- `RANDOM_SPLIT`
- `EXPERIMENT`
- `SCORING`
- `RECOMMENDATION`
- `AI_NEXT_BEST_ACTION`

### Phase 7：结构复用与受控循环

- `MERGE`
- `LOOP`
- `GOTO`
- `TRANSFER_JOURNEY`
- `SUBFLOW`
- `GROUP`
- `TEMPLATE_NODE`

## 测试策略

后端测试：

- 每个 Handler 的单元测试
- NodeResult V2 路由测试
- 旧图 JSON 兼容测试
- WAIT 事件唤醒和超时测试
- GOAL_CHECK 同步和异步测试
- 频控 Redis 并发测试
- 触达幂等测试
- Loop / GoTo 最大次数测试
- 发布校验测试

前端测试：

- outlet schema 转 handle
- 新节点配置表单渲染
- 分支连线保存与回显
- 旧节点兼容回显
- 发布前校验提示

端到端场景：

1. 事件进入旅程，等待 1 小时，未达成目标则短信触达。
2. 用户已退订短信，触达保护走 suppressed 分支。
3. Push 不可达时 fallback 到短信。
4. 实验 A/B/C 三组分流并记录 variant。
5. Loop 每 7 天提醒一次，最多 4 次，目标达成后退出。
6. Audience 进入触发召回旅程。

## 风险与处理

1. **节点数量暴涨导致运营难找。** 节点库保留搜索、分类、常用节点和模板片段。
2. **Loop / GoTo 破坏 DAG 假设。** 通过受控回边、发布校验和运行时访问计数限制。
3. **外部通道未配置导致节点不可用。** 节点可配置但发布阻断，并提示缺失适配器。
4. **等待事件恢复遗漏。** 等待订阅落库，事件上报和定时扫描双路径恢复。
5. **副作用节点重复执行。** 副作用节点强制幂等策略，缺失时发布失败。

## 验收标准

1. 节点库展示本设计中的全部节点。
2. 所有新增节点能拖入、配置、连线、保存、回显。
3. 发布校验能阻止不完整配置和无边界循环。
4. `WAIT` 和 `GOAL_CHECK` 能真实挂起、恢复和超时。
5. 触达保护、静默、可达性和频控能真实改变路径。
6. Email/SMS/Push/In-App/WeChat 节点通过统一触达适配器产生发送记录。
7. 旧画布不需要迁移即可继续打开、保存和执行。
8. 新增节点都有后端单元测试和前端关键行为测试。
