# 画布示例库设计

## 背景

当前项目已有少量示例画布通过 Flyway SQL 直接写入 `canvas` 和 `canvas_version.graph_json`，例如新用户领券、AB 推送、事件触发金额分级、人群圈选三路分流。这种方式能让用户打开画布列表后直接看到示例，但存在几个问题：

1. 示例数量少，无法覆盖现有全部节点和常见组合。
2. 示例和正式画布混在一起，缺少统一标记、分类和开关。
3. 示例逻辑散落在多个 migration 中，后续维护、补充和查找成本高。
4. 现有 `canvas_template` 表和模板接口已经存在，但尚未承载完整官方示例库；`createFromTemplate` 当前只创建 `canvas`，没有把模板 `graph_json` 写入草稿版本。

本次目标是建立一套可维护的“文档示例库 + 官方模板画布”体系：运营可以学习怎么用，售前可以演示行业案例，研发可以用模板验证节点边界和执行路径。

## 目标

1. 覆盖现有画布组件的详细用法、配置要点、连接方式和常见组合。
2. 提供 40+ 个官方示例模板，覆盖不同公司类型和营销场景。
3. 示例模板以 SQL 数据形式进入库中，最终仍落到 `canvas` / `canvas_version.graph_json`，前端沿用现有画布渲染链路。
4. 增加开关控制示例是否出现在画布列表中，默认展示。
5. 复用并扩展已有 `canvas_template` 表，避免重复建设新的模板模型。
6. 保证导入幂等，不覆盖用户复制、编辑或改造后的画布。

## 非目标

1. 不新增一套独立的“示例画布渲染器”。
2. 不让前端直接读取 demo 表渲染画布详情。
3. 不把示例画布默认发布成线上可触发活动，默认保持草稿态。
4. 不为示例引入第二个业务数据源；已有 `canvas_demo` 数据库继续只承载人群演示外部数据。
5. 不在本次实现模板市场、评分、收藏、权限分发等能力。

## 方案选型

### 方案 A：继续用 Flyway 直接写 `canvas`

做法：每个示例都通过 migration 直接 `INSERT canvas` 和 `INSERT canvas_version`。

优点：
- 实现最简单。
- 前端无需任何调整。

缺点：
- 运行时开关弱，migration 执行后无法自然撤回。
- 40+ 示例会把正式画布列表撑满。
- 示例缺少稳定的模板元数据，后续文档、分类、复制和维护都不方便。

### 方案 B：只写 `canvas_template`

做法：所有示例只进入 `canvas_template`，用户从模板创建正式画布。

优点：
- 数据模型更像“模板库”。
- 不污染正式画布列表。

缺点：
- 不满足“默认渲染给用户看”的要求。
- 需要前端有完整模板入口，否则用户看不到画布效果。
- 现有 `createFromTemplate` 还需要修复，不能直接依赖。

### 方案 C：`canvas_template` 存模板，启动导入正式画布

做法：
- Flyway 负责扩展并填充 `canvas_template` 官方模板数据。
- 应用启动时读取官方模板，根据 `canvas.examples.enabled` 决定是否导入/展示。
- 导入后创建 `canvas` 和草稿 `canvas_version`，前端仍按现有接口渲染。

优点：
- 符合“SQL 写入、默认渲染”的要求。
- 模板元数据集中，方便分类、文档关联和后续维护。
- 开关可以控制是否展示，默认开启。
- 可以做到幂等导入，避免重复创建。

缺点：
- 需要少量后端导入器和列表过滤逻辑。
- 需要扩展现有模板表和修复从模板创建画布的版本写入。

### 结论

采用方案 C。它同时满足“示例要直接渲染给用户看”和“要有开关”的要求，也能复用项目中已有的 `canvas_template` 能力。

## 数据模型设计

### 扩展 `canvas_template`

现有 `canvas_template` 保留作为模板主表，新增官方示例需要的稳定标识和分类字段：

```sql
ALTER TABLE canvas_template
  ADD COLUMN template_key VARCHAR(100) NULL COMMENT '官方模板稳定唯一键',
  ADD COLUMN company_type VARCHAR(50) NULL COMMENT '公司类型',
  ADD COLUMN marketing_scenario VARCHAR(50) NULL COMMENT '营销场景',
  ADD COLUMN difficulty VARCHAR(20) NULL COMMENT '入门/进阶/复杂',
  ADD COLUMN covered_node_types VARCHAR(1000) NULL COMMENT '覆盖的节点类型，逗号分隔',
  ADD COLUMN sort_order INT NOT NULL DEFAULT 0 COMMENT '官方模板排序',
  ADD COLUMN enabled TINYINT NOT NULL DEFAULT 1 COMMENT '1=模板可用',
  ADD UNIQUE KEY uk_canvas_template_key (template_key);
```

约束：
- 用户自建模板允许 `template_key` 为空。
- 官方示例模板必须有 `template_key`，并设置 `is_official=1`。
- `graph_json` 继续是完整画布 JSON，格式与 `canvas_version.graph_json` 一致。

### 扩展 `canvas`

为默认展示和幂等导入增加示例标记：

```sql
ALTER TABLE canvas
  ADD COLUMN is_example TINYINT NOT NULL DEFAULT 0 COMMENT '1=官方示例画布',
  ADD COLUMN source_template_key VARCHAR(100) NULL COMMENT '来源官方模板 key',
  ADD INDEX idx_example_template (is_example, source_template_key);
```

用途：
- `is_example=1` 用于列表展示开关和示例标识。
- `source_template_key` 用于启动导入时判断某个模板是否已经生成过画布。
- 用户复制示例画布时，新画布应设置 `is_example=0`，避免被开关隐藏或导入器管理。

### 旧示例回收

已有名称以 `示例：` 开头且 `created_by='system'` 的历史示例画布，应在迁移中标记为 `is_example=1`。能准确匹配到新模板 key 的，补齐 `source_template_key`；不能准确匹配的，保留为 legacy 示例。

## 配置设计

新增配置：

```yaml
canvas:
  examples:
    enabled: true
```

语义：
- 默认 `true`：应用启动时自动补齐官方示例画布，`/canvas/list` 默认展示 `is_example=1` 的画布。
- 设置为 `false`：启动时不导入官方示例，`/canvas/list` 隐藏 `is_example=1` 的画布。
- 关闭开关不会删除已经导入的示例，避免误删用户正在查看或基于 URL 打开的内容。

## 后端设计

### 官方模板 SQL

新增一组 migration 写入 `canvas_template`：
- 扩展模板字段。
- 插入或补齐官方模板记录。
- 回填历史示例画布标记。

模板 SQL 仍然是示例的来源，符合“demo 表中存 demo”的要求；导入器只负责把模板实例化为正式画布草稿。

### 示例导入器

新增 `CanvasExampleSeeder`，在应用启动后执行：

1. 读取 `canvas.examples.enabled`。
2. 如果为 `false`，直接结束。
3. 查询 `canvas_template` 中 `is_official=1 AND enabled=1 AND template_key IS NOT NULL` 的模板。
4. 对每个模板检查是否已存在 `canvas.source_template_key = template_key` 的画布。
5. 不存在则创建草稿画布：
   - `canvas.name = template.name`
   - `canvas.description = template.description`
   - `canvas.status = DRAFT`
   - `canvas.created_by = example-seed`
   - `canvas.is_example = 1`
   - `canvas.source_template_key = template_key`
6. 为新画布创建 `canvas_version`：
   - `version = 1`
   - `status = DRAFT`
   - `graph_json = template.graph_json`
   - `created_by = example-seed`

导入器不覆盖已存在的示例画布。模板内容升级时，新模板记录生效，但已经生成的画布保持原样，避免覆盖用户直接编辑过的草稿。

### 画布列表

`CanvasService.list` 增加配置感知：
- `canvas.examples.enabled=true`：列表展示示例和普通画布。
- `canvas.examples.enabled=false`：列表过滤 `is_example=1`。

后续可在前端增加“示例”标签和筛选，但第一版不依赖前端改造即可默认渲染。

### 从模板创建画布

修复 `POST /canvas/from-template/{templateId}`：
- 创建 `canvas` 后必须创建草稿 `canvas_version`，写入模板 `graph_json`。
- 从模板创建的用户画布 `is_example=0`，`source_template_key` 为空。
- `use_count` 正常递增。

## 文档示例库设计

文档建议放在 `docs/canvas-examples/`，按三层组织。

### 1. 组件手册

每个组件按统一结构说明：
- 适用场景
- 核心配置
- 输入上下文
- 输出上下文
- 出边/分支规则
- 常见组合
- 注意事项
- 对应模板 key

覆盖组件：
- 入口与触发：`START`、`EVENT_TRIGGER`、`MQ_TRIGGER`、`SCHEDULED_TRIGGER`、`DIRECT_CALL`、`CANVAS_TRIGGER`
- 分支与路由：`IF_CONDITION`、`SELECTOR`、`PRIORITY`、`AB_SPLIT`
- 并行与汇聚：`HUB`、`AGGREGATE`、`THRESHOLD`、`LOGIC_RELATION`
- 人群与标签：`TAGGER` 的 `audience`、`offline`、`realtime` 模式
- 动作：`API_CALL`、`COUPON`、`IN_APP_NOTIFY`、`REACH_PLATFORM`、`SEND_MQ`、`GROOVY`
- 控制：`DELAY`、`MANUAL_APPROVAL`、`DIRECT_RETURN`、`SUB_FLOW_REF`、`END`

### 2. 组合套路

沉淀可复用的画布搭法：
- 实时事件触发：`START -> EVENT_TRIGGER -> IF_CONDITION -> ACTION -> END`
- 定时批量运营：`START -> SCHEDULED_TRIGGER -> TAGGER(audience) -> REACH_PLATFORM -> END`
- 多渠道触达：`ACTION A/B/C -> AGGREGATE -> IF_CONDITION -> END`
- 快速失败保护：`parallel actions -> THRESHOLD(any_fail) -> MANUAL_APPROVAL`
- 人群优先级：`PRIORITY -> COUPON/REACH -> END`
- 实验分流：`AB_SPLIT -> variant actions -> AGGREGATE`
- 同步直调返回：`DIRECT_CALL -> API_CALL -> DIRECT_RETURN`
- 子流程复用：`CANVAS_TRIGGER/SUB_FLOW_REF -> parent continuation`

### 3. 行业和营销场景

每个行业模板说明：
- 公司背景
- 营销目标
- 画布结构
- 节点配置要点
- 可观察结果
- 可改造方向

## 官方模板清单

第一版模板目标为 48 个，分为组件教学模板和行业场景模板。

### 组件教学模板（18 个）

| template_key | 名称 | 重点组件 |
|---|---|---|
| component_event_if_coupon | 示例：事件触发新客领券 | `EVENT_TRIGGER`, `IF_CONDITION`, `COUPON` |
| component_mq_validate_route | 示例：MQ 消息校验后路由 | `MQ_TRIGGER`, `IF_CONDITION`, `SEND_MQ` |
| component_scheduled_audience_push | 示例：定时人群 Push | `SCHEDULED_TRIGGER`, `TAGGER(audience)`, `REACH_PLATFORM` |
| component_direct_call_return | 示例：直调查询并同步返回 | `DIRECT_CALL`, `API_CALL`, `DIRECT_RETURN` |
| component_selector_multi_branch | 示例：条件选择器多分支 | `SELECTOR`, `IN_APP_NOTIFY` |
| component_priority_offer | 示例：优先级权益匹配 | `PRIORITY`, `COUPON` |
| component_ab_split_compare | 示例：AB 分流触达实验 | `AB_SPLIT`, `IN_APP_NOTIFY`, `REACH_PLATFORM` |
| component_hub_wait_all | 示例：集线器等待并行完成 | `HUB`, `API_CALL` |
| component_aggregate_kpi | 示例：聚合评估成功率 | `AGGREGATE`, `IF_CONDITION` |
| component_threshold_fast_win | 示例：阈值触发快速决策 | `THRESHOLD`, `SEND_MQ` |
| component_logic_relation | 示例：逻辑关系组合判断 | `LOGIC_RELATION`, `IF_CONDITION` |
| component_manual_approval | 示例：人工审批后发券 | `MANUAL_APPROVAL`, `COUPON` |
| component_delay_followup | 示例：延迟二次触达 | `DELAY`, `REACH_PLATFORM` |
| component_groovy_transform | 示例：Groovy 字段加工 | `GROOVY`, `API_CALL` |
| component_tagger_offline | 示例：离线标签判断 | `TAGGER(offline)`, `IF_CONDITION` |
| component_tagger_realtime | 示例：实时标签触发流程 | `TAGGER(realtime)`, `IN_APP_NOTIFY` |
| component_sub_flow_ref | 示例：子流程引用 | `SUB_FLOW_REF`, `CANVAS_TRIGGER` |
| component_send_mq_receipt | 示例：发送 MQ 通知下游 | `SEND_MQ`, `END` |

### 行业场景模板（30 个）

| template_key | 公司类型 | 营销场景 | 名称 |
|---|---|---|---|
| ecommerce_new_user_coupon | 电商 | 拉新转化 | 示例：新客首单券发放 |
| ecommerce_cart_recall | 电商 | 弃购召回 | 示例：加购未支付召回 |
| ecommerce_vip_tier_offer | 电商 | 会员运营 | 示例：会员等级差异化权益 |
| ecommerce_cross_sell | 电商 | 交叉销售 | 示例：订单完成后关联推荐 |
| travel_flight_delay_care | 出行 | 服务关怀 | 示例：航班延误补偿触达 |
| travel_hotel_bundle | 出行 | 复购提升 | 示例：机票成交后酒店联售 |
| travel_high_value_route | 出行 | 高价值客户 | 示例：高价值用户专属活动 |
| travel_pre_departure_reminder | 出行 | 行前提醒 | 示例：出行前多渠道提醒 |
| fintech_card_activation | 金融 | 激活转化 | 示例：信用卡开卡激活 |
| fintech_risk_review | 金融 | 风控拦截 | 示例：大额交易人工复核 |
| fintech_loan_repay_reminder | 金融 | 还款提醒 | 示例：贷款还款分层提醒 |
| fintech_wealth_cross_sell | 金融 | 交叉销售 | 示例：理财产品适配推荐 |
| saas_trial_nurture | SaaS | 试用转化 | 示例：试用期行为培育 |
| saas_onboarding_steps | SaaS | 新手引导 | 示例：新账号上手路径 |
| saas_churn_risk_save | SaaS | 流失挽回 | 示例：低活跃客户挽留 |
| saas_expansion_signal | SaaS | 增购扩容 | 示例：高用量客户扩容推荐 |
| local_food_coupon | 本地生活 | 到店转化 | 示例：餐饮券包发放 |
| local_service_reactivation | 本地生活 | 沉睡召回 | 示例：本地服务沉睡用户召回 |
| local_weather_push | 本地生活 | 场景营销 | 示例：天气触发即时权益 |
| retail_store_lbs | 零售 | 到店引流 | 示例：门店附近用户触达 |
| retail_inventory_clearance | 零售 | 清仓促销 | 示例：库存清仓定向触达 |
| retail_member_anniversary | 零售 | 会员纪念日 | 示例：会员周年礼 |
| content_subscription_trial | 内容平台 | 订阅转化 | 示例：内容试读后订阅转化 |
| content_inactive_reader | 内容平台 | 活跃提升 | 示例：沉默读者唤醒 |
| gaming_level_reward | 游戏 | 成长激励 | 示例：等级达成奖励 |
| gaming_lost_user_winback | 游戏 | 回流召回 | 示例：流失玩家回流礼包 |
| education_course_followup | 教育 | 课程转化 | 示例：试听课后跟进 |
| education_learning_reminder | 教育 | 学习促活 | 示例：学习计划提醒 |
| b2b_lead_scoring | B2B | 线索培育 | 示例：线索评分后分配 |
| logistics_delivery_care | 物流 | 服务通知 | 示例：异常配送关怀 |

## `graph_json` 编写规范

1. 每个模板必须包含 `START` 和 `END`。
2. 节点坐标采用自上而下布局，主干 x 坐标固定，分支左右展开。
3. 节点 ID 使用稳定、可读的短 key，例如 `start`、`event_order_paid`、`if_new_user`。
4. 分支节点必须使用当前前端支持的 handle 配置：
   - `IF_CONDITION`: `successNodeId`, `failNodeId`
   - `SELECTOR`: `branches`, `elseNodeId`
   - `AB_SPLIT`: `groups`
   - `PRIORITY`: `priorities`
   - `TAGGER(audience)`: `hitNextNodeId`, `missNextNodeId`
   - `AGGREGATE`: `successNodeId`, `failNodeId`
   - `THRESHOLD`: `successNodeId`, `failNodeId`
   - `MANUAL_APPROVAL`: `approveNodeId`, `rejectNodeId`
5. 模板使用的 `apiKey`、`messageCodeKey`、`couponTypeKey`、`audienceId` 应引用现有或同步新增的 demo 元数据。
6. 所有官方模板默认草稿态，不注册触发路由，不产生真实触达。

## 前端设计

第一版不新增渲染器。现有画布列表和编辑器继续工作：
- 示例导入到 `canvas` 后自然出现在列表。
- 点击示例进入编辑器，React Flow 按 `graph_json` 渲染。
- 复制示例后得到普通画布，运营可以修改并发布。

实施时需要处理一个现有兼容点：`deriveEdges`、`patchBizConfig` 和发布前校验都把 IF 失败分支映射为 `failNodeId` / `fail` handle，但 `branchHandles.ts` 当前对 `IF_CONDITION` 的负分支使用 `else` handle。官方模板统一使用 `failNodeId`；前端应将 IF 负分支 handle 调整为 `fail`，或在渲染层同时兼容 `fail` 和 `else`，避免示例画布出现不可见或不可编辑的失败分支。

建议增加两处轻量体验优化：
1. 画布列表对 `isExample` 显示“示例”标签。
2. 新建画布入口增加“从模板创建”入口，调用现有 `/canvas/templates` 和 `/canvas/from-template/{templateId}`。

体验优化不阻塞第一版默认展示目标。

## 错误处理

1. 模板 JSON 解析失败：启动导入器记录错误日志并跳过该模板，不阻塞应用启动。
2. 模板引用未知节点类型：启动导入器记录错误并跳过该模板；测试阶段应阻止这种情况进入主干。
3. 模板引用缺失 demo 元数据：允许导入，但文档和测试需标记依赖；涉及执行测试的模板必须补齐元数据。
4. 重复 `template_key`：数据库唯一索引阻止写入。
5. 示例开关关闭：不导入、不在列表展示，但不删除已有示例。
6. 用户复制示例：副本不继承 `is_example` 和 `source_template_key`。

## 测试策略

### 单元测试

1. `CanvasExampleSeeder`：
   - 开关关闭时不导入。
   - 开关开启时导入缺失模板。
   - 已有 `source_template_key` 时不重复创建。
   - 模板 JSON 异常时跳过并继续处理其他模板。

2. `CanvasService.list`：
   - 开关开启时包含示例。
   - 开关关闭时过滤示例。
   - 普通画布不受影响。

3. `createFromTemplate`：
   - 创建 canvas 后同步创建草稿 canvas_version。
   - 副本 `is_example=0`。
   - `use_count` 递增。

### 集成测试

1. 启动上下文后确认官方模板能生成画布。
2. 读取生成画布详情，确认 `graph_json` 与模板一致。
3. 对每个官方模板运行 DAG 解析，确认无环、节点 ID 唯一、连接目标存在。
4. 对带分支 handle 的模板验证配置字段与前端 `branchHandles` 约定一致。

### 文档校验

1. 每个模板 key 在文档中至少出现一次。
2. 每个 active 节点类型至少被一个组件教学模板覆盖。
3. 行业模板至少覆盖拉新、促活、召回、复购、交叉销售、风控/审批、服务关怀七类营销目标。

## 实施顺序

1. 扩展 `canvas_template` 和 `canvas` 表字段，回填历史示例标记。
2. 增加配置项 `canvas.examples.enabled=true`。
3. 实现 `CanvasExampleSeeder` 和列表过滤。
4. 修复 `createFromTemplate` 写入草稿版本。
5. 先落地 18 个组件教学模板 SQL。
6. 再落地 30 个行业场景模板 SQL。
7. 编写 `docs/canvas-examples/` 文档。
8. 补充单元测试和 DAG 校验测试。

## 验收标准

1. 默认配置启动后，画布列表能看到官方示例画布。
2. 设置 `canvas.examples.enabled=false` 后，画布列表不展示官方示例。
3. 任意官方示例都能打开编辑器并正常渲染节点和连线。
4. 示例导入重复启动不会产生重复画布。
5. 从模板创建的新画布包含模板 graph，并且不是 `is_example`。
6. 文档能按组件、组合套路、行业场景三个维度解释示例。
7. 每个 active 节点类型至少有一个示例说明“怎么用”和“怎么组合”。
