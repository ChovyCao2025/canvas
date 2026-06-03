# Node Governance Cleanup Design

## Background

The current node catalog mixes technical implementation nodes, historical compatibility nodes, product-facing actions, placeholders, and unproven future concepts. Because this code has not been released, compatibility with the existing experimental node set is not required. Governance should delete unclear or duplicated nodes instead of hiding them behind product copy.

## Goals

- Reduce the product-facing catalog to a small set of understandable journey nodes.
- Reclassify nodes by the operator's journey-building mental model.
- Merge duplicated capabilities into one product node when the behavior is still valuable.
- Remove unused handlers, node constants, branch handles, node registrations, and tests for deleted nodes.
- Keep backend execution and frontend editor behavior aligned with the final catalog.

## Final Categories

| Category | Nodes |
| --- | --- |
| 基础控制 | `START`, `END`, `DIRECT_RETURN` |
| 入口触发 | `DIRECT_CALL`, `EVENT_TRIGGER`, `MQ_TRIGGER`, `SCHEDULED_TRIGGER` |
| 条件与分流 | `IF_CONDITION`, `SPLIT` |
| 等待与汇聚 | `WAIT`, `HUB`, `AGGREGATE`, `THRESHOLD` |
| 动作执行 | `API_CALL`, `SEND_MQ`, `GROOVY` |
| 消息触达 | `SEND_MESSAGE` |
| 数据与权益 | `TAGGER`, `COMMIT_ACTION` |
| 流程复用 | `SUB_FLOW_REF` |

## Merge Decisions

`SPLIT` replaces `AB_SPLIT`, `RANDOM_SPLIT`, and `EXPERIMENT`. It uses one list of branches with ids, labels, weights, and next-node targets. Stable allocation is the default; random allocation remains a configuration option.

`SEND_MESSAGE` replaces `SEND_EMAIL`, `SEND_SMS`, `SEND_PUSH`, `SEND_IN_APP`, and `SEND_WECHAT`. Channel is a required configuration value. The runtime keeps the existing delivery service and send-record behavior.

`COMMIT_ACTION` remains the single node for committed side effects such as coupon and points operations. `COUPON` and `POINTS_OPERATION` are removed.

`TAGGER` remains the single Tagger node. `TAGGER_OFFLINE` and `TAGGER_REALTIME` are removed.

`SUB_FLOW_REF` remains the only subflow call node. `CANVAS_TRIGGER`, `SUBFLOW`, and `TRANSFER_JOURNEY` are removed.

The separate protection nodes `SUPPRESSION_CHECK`, `QUIET_HOURS`, `CHANNEL_AVAILABILITY`, and `FREQUENCY_CAP` are removed from this pass. A future product node should be named "触达保护" rather than "疲劳度控制", because fatigue is only one part of the protection problem.

## Delete Decisions

Delete the following product nodes and their handlers/tests unless another retained node still depends on implementation code:

`API_TRIGGER`, `AUDIENCE_TRIGGER`, `BEHAVIOR_IN_APP`, `AI_NEXT_BEST_ACTION`, `CANVAS_TRIGGER`, `CDP_TAG_WRITE`, `CHANNEL_AVAILABILITY`, `COUPON`, `CREATE_TASK`, `DELAY`, `EXPERIMENT`, `FREQUENCY_CAP`, `GOAL_CHECK`, `GOTO`, `GROUP`, `IN_APP_NOTIFY`, `LOGIC_RELATION`, `LOOP`, `MANUAL_APPROVAL`, `MERGE`, `POINTS_OPERATION`, `PRIORITY`, `QUIET_HOURS`, `RANDOM_SPLIT`, `REACH_PLATFORM`, `RECOMMENDATION`, `SCORING`, `SELECTOR`, `SEND_EMAIL`, `SEND_SMS`, `SEND_PUSH`, `SEND_IN_APP`, `SEND_WECHAT`, `SUBFLOW`, `SUPPRESSION_CHECK`, `TAG_OPERATION`, `TAGGER_OFFLINE`, `TAGGER_REALTIME`, `TEMPLATE_NODE`, `TRACK_EVENT`, `TRANSFER_JOURNEY`, `UPDATE_PROFILE`.

## Backend Design

- Update `NodeType` to expose only retained node constants plus `SPLIT` and `SEND_MESSAGE`.
- Add `SplitHandler` using `WeightedChoice` with `branches`.
- Add `SendMessageHandler` using the existing `AbstractSendMessageHandler` and a channel value from config.
- Remove handler classes for deleted node types.
- Update `ExecutionLaneResolver` to remove deleted trigger and node types.
- Update `DagParser` convergence validation to allow only `HUB`, `AGGREGATE`, `THRESHOLD`, and `END`.
- Edit unreleased Flyway migrations directly so `node_type_registry` seeds only the final product catalog.
- Update demo migrations that reference deleted node types.

## Frontend Design

- Update category colors and default names to the final category set.
- Update publishable trigger node types to the final entrance nodes.
- Replace branch handle support for `AB_SPLIT`, `RANDOM_SPLIT`, and `EXPERIMENT` with `SPLIT`.
- Remove front-end support paths for deleted node types, including template expansion for `TEMPLATE_NODE`.
- Keep `SUB_FLOW_REF` configuration controls only; remove `CANVAS_TRIGGER` references.

## Testing

- Add governance tests proving deleted node constants are absent.
- Add split handler tests for stable weighted routing and empty branch handling.
- Add send-message handler tests for channel-driven delivery.
- Update frontend tests for default names, category behavior, and `SPLIT` branch handles.
- Run backend targeted tests for retained handlers and governance tests.
- Run frontend targeted Vitest suites for node constants, branch handles, and editor routing.

