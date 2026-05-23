# 系统选项配置化设计

## 背景

前端和元数据层目前有多处固定下拉选项：

- 前端组件内写死，例如条件操作符、参数类型、HTTP 方法、标签类型、人群规则操作符、用户角色、延迟单位。
- `node_type_registry.config_schema` 内联写死，例如阈值触发条件、聚合评估方式、Tagger 模式、审批超时动作、画布调用模式。
- `/meta/*` 接口看似动态，部分背后仍由 `MetaService` 的 `List.of(...)` 返回固定 stub，例如券类型、触达场景、业务线、业务线接口、行为策略类型、消息编码、AB 实验分组。

这些选项应统一治理。目标不是让管理员随意创造后端无法执行的新 key，而是把展示名、排序、启停从代码中移出，让系统选项具备一致的页面维护能力。

## 目标

- 当前审计出的所有固定下拉和固定选项都有明确配置来源，不再散落在前端组件或后端 stub 常量里。
- 内置业务 key 保持稳定，页面只维护显示名、描述、排序、启停。
- 新增统一“系统选项配置”页面，覆盖全局枚举和简单业务选项。
- AB 实验分组做成实验下属配置，不再使用固定 A/B 接口或节点内手写默认分组作为唯一来源。
- 配置面板支持 `optionCategory`，可以从系统选项加载 schema 枚举。
- 禁用选项只影响新选择，历史画布和历史配置仍可展示已有值。

## 非目标

- 不开放任意新增未知业务 key。新增后端不认识的操作符、规则引擎、执行模式仍需要代码和执行逻辑配套。
- 不把数学范围做成可任意配置，例如小时 `0-23`、分钟 `0-59`、日期 `1-28`。这类范围会集中为共享生成函数；频率、周几展示名进入字典。
- 不在本次实现完整外部实验平台。AB 分组先落本地表，后续可替换为外部实验平台同步。
- 不批量改写已保存画布中的业务 key。

## 数据模型

### system_option

通用系统选项表，承载全局枚举和轻量业务选项。

- `id`
- `category`: 选项分类。
- `option_key`: 稳定业务 key。
- `label`: 展示名。
- `description`: 说明。
- `sort_order`: 排序。
- `enabled`: 是否在新配置下拉中展示。
- `system_builtin`: 是否内置。内置项不可删除，不可修改 `category` 或 `option_key`。
- `created_at`
- `updated_at`

约束：

- `category + option_key` 唯一。
- 管理页只允许更新 `label`、`description`、`sort_order`、`enabled`。
- 后端保存业务配置时仍按现有业务逻辑校验 key，避免保存未知值。

### ab_experiment_group

AB 实验分组属于具体实验，不放入全局字典。

- `id`
- `experiment_id`
- `group_key`
- `label`
- `sort_order`
- `enabled`
- `created_at`
- `updated_at`

约束：

- `experiment_id + group_key` 唯一。
- 删除实验时删除或禁用其分组。
- 新建实验默认创建 `A`、`B` 两个分组。

## 系统选项分类

初始种子数据覆盖当前全部固定项。

| 分类 | 当前 key |
| --- | --- |
| `condition_operator` | `EQ`, `NEQ`, `CONTAINS`, `GT`, `LT`, `GTE`, `LTE` |
| `audience_condition_operator` | `=`, `!=`, `>`, `>=`, `<`, `<=`, `in` |
| `logic_relation` | `AND`, `OR` |
| `query_combinator` | `and`, `or` |
| `param_type` | `STRING`, `NUMBER`, `TEXT`, `DATE`, `STRING_PARAM`, `BOOLEAN`, `LIST` |
| `event_attr_type` | `STRING`, `NUMBER`, `DATE` |
| `http_method` | `GET`, `POST` |
| `tag_type` | `offline`, `realtime` |
| `audience_data_source_type` | `TAGGER_API`, `JDBC` |
| `audience_evaluation_strategy` | `OFFLINE_BATCH`, `ONLINE`, `HYBRID` |
| `audience_engine_type` | `AVIATOR`, `QL` |
| `user_role` | `ADMIN`, `OPERATOR` |
| `context_value_type` | `CUSTOM`, `CONTEXT` |
| `delay_unit` | `SECOND`, `MINUTE`, `HOUR` |
| `cron_frequency` | `daily`, `weekly`, `monthly`, `hourly` |
| `weekday` | `0`, `1`, `2`, `3`, `4`, `5`, `6` |
| `schedule_type` | `ONCE`, `CRON` |
| `tagger_mode` | `realtime`, `offline`, `audience` |
| `threshold_mode` | `min_success`, `min_done`, `any_fail` |
| `aggregate_evaluate_mode` | `count`, `rate`, `script` |
| `approval_timeout_action` | `REJECT`, `APPROVE`, `KEEP_WAITING` |
| `canvas_invoke_mode` | `SYNC`, `ASYNC` |
| `direct_return_build_type` | `CUSTOM` |
| `coupon_type` | 当前 `MetaService.getCouponTypes()` 的三项 |
| `reach_scene` | 当前 `MetaService.getReachScenes()` 的三项 |
| `biz_line` | 当前 `MetaService.getBizLines()` 的三项 |
| `biz_line_api` | 当前 `MetaService.getBizLineApis()` 的三项，后续可升级为带父级业务线的领域表 |
| `behavior_strategy_type` | 当前 `MetaService.getBehaviorStrategyTypes()` 的三项 |
| `message_code_in_app` | 当前端内消息编码 |
| `message_code_mq` | 当前 MQ 消息编码 |
| `mq_topic_legacy` | 当前旧 `/meta/mq-topics` 三项，保留兼容旧 schema |

不进入字典但已配置化或领域化的来源：

- API 定义：继续来自 `api_definition`。
- MQ 消息定义：继续来自 `mq_message_definition`。
- 事件定义：继续来自 `event_definition`。
- 标签定义：继续来自 `tag_definition` 或 Tagger 服务兜底。
- 人群：继续来自人群接口。
- AB 实验：继续来自 `ab_experiment`。
- AB 实验分组：来自新增 `ab_experiment_group`。
- Cron 小时、分钟、日期：固定合法范围，集中生成，不做页面配置。

## 后端接口

### 元数据查询

`GET /meta/options?category=condition_operator`

- 返回启用项，按 `sort_order, id` 排序。
- 用于业务页面和配置面板下拉。

`GET /meta/options/batch?categories=condition_operator,param_type`

- 可选优化接口，减少前端多分类加载请求。

### 管理接口

`GET /admin/system-options`

- 支持按 `category`、`enabled`、关键词查询。
- 返回启用和禁用项。

`PUT /admin/system-options/{id}`

- 更新 `label`、`description`、`sort_order`、`enabled`。
- 拒绝修改 `category`、`option_key`、`system_builtin`。

`POST /admin/system-options/sync-defaults`

- 幂等补齐内置项。
- 用于环境修复和未来新增分类补种子。

### AB 分组接口

`GET /canvas/ab-experiments/{id}/groups?includeDisabled=true`

- 查询某实验分组。
- 管理页传 `includeDisabled=true`，返回启用和禁用项。
- 元数据和节点配置场景不传该参数，只返回启用项。

`POST /canvas/ab-experiments/{id}/groups`

- 创建分组。AB 分组是实验下属领域数据，允许管理员输入新的 `group_key`，后端校验非空、唯一、长度和字符集。

`PUT /canvas/ab-experiments/{id}/groups/{groupId}`

- 更新展示名、排序、启停。

`DELETE /canvas/ab-experiments/{id}/groups/{groupId}`

- 首版做软删除或禁用，避免破坏历史画布引用。

`GET /meta/ab-experiments/{key}/groups`

- 保留接口路径，但改为从 `ab_experiment_group` 查询，不再返回固定 A/B。

## 配置面板 schema

`SchemaField` 增加：

- `optionCategory?: string`

渲染规则：

1. `dataSource` 优先，继续用于 API、事件、MQ、标签、人群等领域数据。
2. `optionCategory` 次之，从 `/meta/options` 加载系统选项。
3. `options` 保留兜底兼容，用于未迁移或旧环境 schema。

需要迁移的 `node_type_registry.config_schema`：

- `scheduleType` 使用 `optionCategory: "schedule_type"`。
- `TAGGER.mode` 使用 `optionCategory: "tagger_mode"`。
- `THRESHOLD.thresholdMode` 使用 `optionCategory: "threshold_mode"`。
- `AGGREGATE.evaluateMode` 使用 `optionCategory: "aggregate_evaluate_mode"`。
- `MANUAL_APPROVAL.onTimeout` 使用 `optionCategory: "approval_timeout_action"`。
- `CANVAS_TRIGGER.invokeMode` 使用 `optionCategory: "canvas_invoke_mode"`。
- `LOGIC_RELATION.relation` 使用 `optionCategory: "logic_relation"`。
- `DIRECT_RETURN.buildType` 使用 `optionCategory: "direct_return_build_type"`。
- 旧 `DELAY.unit` 兼容字段使用 `optionCategory: "delay_unit"`。

## 前端接入

新增共享能力：

- `systemOptionsApi`
- `useSystemOptions(category)`
- `useSystemOptionsBatch(categories)`
- `mergeCurrentValueOption(options, currentValue)`：当前值被禁用时仍显示“已禁用：label/key”。

接入页面：

- API 配置：参数类型、HTTP 方法。
- MQ 配置：参数类型。
- 事件配置：属性类型。
- 标签配置：标签类型。
- 人群编辑：数据源类型、计算策略、规则引擎、规则操作符、组合关系。
- 用户管理：用户角色。
- 画布配置面板：条件操作符、逻辑关系、上下文值类型、参数定义类型、延迟单位、schema `optionCategory`。
- CronBuilder：频率和周几显示名从字典加载；小时、分钟、月内日期由共享范围函数生成。
- AB 实验管理：新增分组管理抽屉或弹窗。
- AB_SPLIT 节点：选择实验后加载启用分组，生成路由项；用户只配置连线，不手写分组 key。

新增页面：

- 菜单：系统设置 -> 系统选项配置。
- 表格字段：分类、Key、显示名、描述、排序、启用、内置、更新时间、操作。
- 编辑弹窗：显示名、描述、排序、启用。
- 高风险项提示：禁用只影响新选择，不会改历史配置；禁用核心项可能影响新建配置。

## AB 分组行为

新建 AB 实验：

- 自动创建 `A`、`B` 两个启用分组。

编辑 AB 实验：

- 可在“分组”入口维护分组显示名、排序、启停。

AB_SPLIT 节点：

- `experimentKey` 仍保存实验 key。
- `groups` 仍保存在节点配置中，用于保存每个分组的 `nextNodeId`。
- 打开配置时用实验分组同步 `groupKey` 和显示名。
- 分组禁用后，新配置不再展示；历史画布若仍有该 `groupKey`，显示为已禁用并保留连线信息。

执行逻辑：

- 首版继续使用现有 `Hash(userId:experimentKey) % 100` 按启用分组数量等比分桶。
- 若未来接入实验平台，可在 `AbSplitHandler` 内替换为实验平台结果；节点配置结构无需大改。

## 迁移策略

新增 migration：

- 创建 `system_option`。
- 创建 `ab_experiment_group`。
- 幂等插入系统选项种子。
- 为已有 AB 实验插入默认 `A/B` 分组。
- 更新已知 `node_type_registry.config_schema`，把内联 `options` 改为 `optionCategory`。

兼容：

- 前端保留 `field.options` 兜底。
- 当前已经保存的画布不批量改 key。
- 禁用项历史值继续显示。

## 错误处理

- 字典加载失败时，下拉禁用并显示“选项加载失败”，不静默使用旧写死列表。
- 保存时若 key 不存在或已禁用，后端对新建/更新请求返回明确错误；历史读取不报错。
- 管理页更新内置项 key 或删除内置项时返回错误。
- AB_SPLIT 选择实验但无启用分组时，配置面板提示先在 AB 实验管理中配置分组。

## 测试

后端：

- `SystemOptionService` 查询只返回启用项。
- 管理接口可更新 label、描述、排序、启停。
- 内置项不可删除，不可改 key。
- `sync-defaults` 幂等。
- `MetaService` stub 选项替换为字典查询。
- AB 实验创建时生成默认 A/B 分组。
- AB 分组 CRUD 和唯一约束。
- `/meta/ab-experiments/{key}/groups` 从表读取。

前端：

- `useSystemOptions` 正常加载、失败提示、禁用状态。
- `ConfigPanel` 支持 `optionCategory`。
- 条件规则和分支规则操作符来自字典。
- 人群 QueryBuilder 操作符和组合关系来自字典。
- API/MQ/事件/标签/用户管理下拉来自字典。
- 系统选项配置页可编辑 label、排序、启停。
- AB 实验分组管理可维护分组。
- AB_SPLIT 节点按实验分组同步路由项并保留历史禁用值。

回归：

- 打开并保存 API 配置、MQ 配置、事件配置、标签配置、人群编辑、用户创建、画布节点配置。
- 已有画布的 IF、SELECTOR、THRESHOLD、AGGREGATE、TAGGER、MANUAL_APPROVAL、AB_SPLIT 节点能正常展示。

## 风险与处理

- 风险：禁用核心选项后新配置无法选择。处理：管理页提示，并保留历史值展示。
- 风险：部分 schema 内联 options 未被迁移。处理：保留 `options` 兜底，并在测试中覆盖已审计分类。
- 风险：AB 分组从手填变为实验下属配置，旧画布分组可能和实验表不一致。处理：按 `groupKey` 合并，旧值显示为历史分组。
- 风险：一次性改动面广。处理：先落后端字典和管理页，再逐页替换前端使用点，保持每一步可回归。
