# 组件手册

## 入口触发

| 组件 | 用途 | 核心配置 | 常见组合 | 示例模板 |
| --- | --- | --- | --- | --- |
| `START` | 流程入口，每个画布一个 | 无业务配置，连接第一个触发节点 | `START -> EVENT_TRIGGER`、`START -> SCHEDULED_TRIGGER` | `component_event_if_coupon` |
| `EVENT_TRIGGER` | 业务事件实时触发 | `eventCode` | `EVENT_TRIGGER -> IF_CONDITION -> COUPON` | `component_event_if_coupon`、`ecommerce_new_user_coupon` |
| `MQ_TRIGGER` | MQ 消息触发 | `topicKey`、`validateResult`、`validateRules` | `MQ_TRIGGER -> IF_CONDITION -> SEND_MQ` | `component_mq_validate_route` |
| `SCHEDULED_TRIGGER` | 定时批量触发 | `scheduleType`、`cronExpression`、`timezone` | `SCHEDULED_TRIGGER -> TAGGER -> REACH_PLATFORM` | `component_scheduled_audience_push` |
| `DIRECT_CALL` | 业务系统同步直调 | `eventCode` 或输入参数定义 | `DIRECT_CALL -> API_CALL -> DIRECT_RETURN` | `component_direct_call_return` |
| `CANVAS_TRIGGER` | 子画布被父流程调用时的入口 | `targetCanvasId`、`invokeMode` | `CANVAS_TRIGGER -> SUB_FLOW_REF` | `component_sub_flow_ref` |

## 分支决策

| 组件 | 用途 | 核心配置 | 连线 handle | 示例模板 |
| --- | --- | --- | --- | --- |
| `IF_CONDITION` | 单组条件判断 | `rules`、`successNodeId`、`failNodeId` | `success`、`fail` | `component_event_if_coupon` |
| `SELECTOR` | 多条件按顺序匹配，命中第一条 | `branches`、`elseNodeId` | `branch-0..n`、`else` | `component_selector_multi_branch` |
| `PRIORITY` | 按优先级选择权益或路径 | `priorities`、兜底 `nextNodeId` | `priority-0..n`、`default` | `component_priority_offer` |
| `AB_SPLIT` | 实验分组和流量对照 | `experimentKey`、`groups` | `group-A`、`group-B` 等 | `component_ab_split_compare` |
| `LOGIC_RELATION` | AND/OR 逻辑组合 | `relation`、`nextNodeId` | `default` | `component_logic_relation` |

## 并行与聚合

| 组件 | 用途 | 核心配置 | 适合场景 | 示例模板 |
| --- | --- | --- | --- | --- |
| `HUB` | 等待多条上游路径汇合 | `timeout`、`nextNodeId` | 并行任务全部完成后继续 | `component_hub_wait_all` |
| `AGGREGATE` | 等待上游完成后评估数量、比例或脚本 | `evaluateMode`、`minRate`、`successNodeId`、`failNodeId` | 多接口结果打分、渠道成功率评估 | `component_aggregate_kpi` |
| `THRESHOLD` | 任一上游完成时评估阈值 | `thresholdMode`、`threshold`、`successNodeId`、`failNodeId` | 风控任一信号命中即拦截 | `component_threshold_fast_win` |

## 人群与标签

| 组件 | 用途 | 核心配置 | 常见组合 | 示例模板 |
| --- | --- | --- | --- | --- |
| `TAGGER` | 标签判断或人群圈选 | `mode`、`audienceId`、`tagCodeKey` | `TAGGER(audience) -> COUPON -> REACH_PLATFORM` | `component_tagger_offline`、`component_tagger_realtime` |
| `TAGGER_OFFLINE` | 离线标签能力的历史类型 | `tagCodeKey` | 新模板优先使用 `TAGGER` 的 `offline` 模式 | `component_tagger_offline` |
| `TAGGER_REALTIME` | 实时标签能力的历史类型 | `tagCodeKey` | 新模板优先使用 `TAGGER` 的 `realtime` 模式 | `component_tagger_realtime` |

## 动作节点

| 组件 | 用途 | 核心配置 | 常见组合 | 示例模板 |
| --- | --- | --- | --- | --- |
| `API_CALL` | 调用外部服务并把结果写入上下文 | `apiKey`、`inputParams`、`outputPrefix` | `API_CALL -> IF_CONDITION`、`API_CALL -> DIRECT_RETURN` | `component_direct_call_return`、`component_groovy_transform` |
| `COUPON` | 发放权益或券 | `couponTypeKey` | `IF_CONDITION -> COUPON -> END` | `component_event_if_coupon`、`component_priority_offer` |
| `IN_APP_NOTIFY` | 站内信或端内消息 | `messageCodeKey` | `AB_SPLIT -> IN_APP_NOTIFY` | `component_ab_split_compare` |
| `REACH_PLATFORM` | Push、短信等外部触达 | `serviceSceneKey` | `TAGGER -> REACH_PLATFORM` | `component_scheduled_audience_push` |
| `SEND_MQ` | 通知下游系统 | `messageCodeKey` | `IF_CONDITION -> SEND_MQ` | `component_send_mq_receipt` |
| `GROOVY` | 加工上下文字段或复杂脚本 | `code`、`inputParams`、`outputParams` | `GROOVY -> API_CALL` | `component_groovy_transform` |

## 控制节点

| 组件 | 用途 | 核心配置 | 常见组合 | 示例模板 |
| --- | --- | --- | --- | --- |
| `DELAY` | 延迟一段时间后继续 | `duration`、`unit`、`nextNodeId` | `EVENT_TRIGGER -> DELAY -> REACH_PLATFORM` | `component_delay_followup` |
| `MANUAL_APPROVAL` | 挂起等待人工审批 | `approvers`、`timeoutHours`、`approveNodeId`、`rejectNodeId` | `IF_CONDITION -> MANUAL_APPROVAL -> SEND_MQ` | `component_manual_approval` |
| `DIRECT_RETURN` | 直调场景同步返回结果 | `buildType`、`data` | `DIRECT_CALL -> API_CALL -> DIRECT_RETURN` | `component_direct_call_return` |
| `SUB_FLOW_REF` | 调用已有子流程 | `subFlowId`、`subFlowVersion`、`outputPrefix` | `CANVAS_TRIGGER -> SUB_FLOW_REF` | `component_sub_flow_ref` |
| `END` | 结束当前路径 | 无业务配置 | 任意路径尾部 | 多数示例模板 |
