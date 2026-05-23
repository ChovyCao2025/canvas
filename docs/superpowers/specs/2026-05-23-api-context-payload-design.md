# API 环境信息请求体设计

## 背景

API 配置当前只定义接口基础信息和请求参数 Schema。API_CALL 节点执行时会把节点入参直接组装为请求 body。产品需要对齐现有回调格式，让 API 配置者可以决定是否在请求中携带旅程执行环境信息，并在配置页实时看到最终请求 JSON 示例。

本功能尚未上线，不需要兼容旧的平铺请求体格式。

## 目标

- API 定义支持配置“是否携带环境信息”。
- API 配置弹窗根据请求参数定义和环境信息开关实时生成请求示例 JSON。
- API_CALL 执行时统一发送数组包装格式。
- 开关关闭时只发送 `params`。
- 开关打开时发送 `params`、`user_profile`、`callback_params`、`process_info`。

## 非目标

- 不实现用户画像服务查询；`user_profile` 只使用执行上下文已有字段。
- 不实现真实 webhook 回调注册；`callback_params.webhook_id` 暂为空字符串。
- 不保留旧格式 `{ "define_item1": "value" }`。

## API 定义模型

在 `api_definition` 增加布尔字段：

- `include_context_payload`: 是否携带环境信息，`0` 表示关闭，`1` 表示开启。

Java domain 使用 `Integer includeContextPayload`，前端表单使用 boolean，提交时转换成 `0/1`。

## 请求体格式

API_CALL 请求体统一为 JSON 数组，数组内当前只包含一个对象。

开关关闭：

```json
[
  {
    "params": {
      "define_item1": "优惠券ID",
      "define_item2": "优惠券内容示例"
    }
  }
]
```

开关打开：

```json
[
  {
    "user_profile": {
      "target_type": "OPEN_ID",
      "target_id": "1917810",
      "customer_id": "1917810"
    },
    "params": {
      "define_item1": "优惠券ID",
      "define_item2": "优惠券内容示例"
    },
    "callback_params": {
      "webhook_id": "",
      "send_time": "1625037472000",
      "nodeId": "节点Id",
      "instanceId": "实例Id",
      "batchId": "执行动作批次Id，可做批次幂等ID",
      "actionId": "执行动作实例Id，可做单条幂等ID",
      "customerId": "1917810"
    },
    "process_info": {
      "processInstanceId": "新版：旅程周期中，每个用户的旅程实例ID",
      "processInstanceStartTime": "新版：旅程周期中，每个用户的旅程实例开始时间，时间戳格式",
      "processNodeInstanceId": "新版：旅程节点实例ID（每次不同）",
      "processNodeInstanceStartTime": "新版：旅程周期中，每个用户的旅程的节点实例开始时间，时间戳格式",
      "groupName": "nodeId:nodeName:groupResult(node.result),groupName(node.resultExt)"
    }
  }
]
```

运行时字段映射：

- `user_profile.target_type`: 固定为 `OPEN_ID`。
- `user_profile.target_id`: `ExecutionContext.userId`。
- `user_profile.customer_id`: `ExecutionContext.userId`。
- `callback_params.webhook_id`: 空字符串。
- `callback_params.send_time`: 当前请求组装时间戳毫秒。
- `callback_params.nodeId`: 当前 API_CALL 节点 ID。
- `callback_params.instanceId`: `ExecutionContext.executionId`。
- `callback_params.batchId`: `ExecutionContext.executionId`。
- `callback_params.actionId`: `ExecutionContext.executionId + ":" + nodeId`。
- `callback_params.customerId`: `ExecutionContext.userId`。
- `process_info.processInstanceId`: `ExecutionContext.executionId`。
- `process_info.processInstanceStartTime`: 当前上下文没有实例开始时间字段，使用当前请求组装时间戳毫秒。
- `process_info.processNodeInstanceId`: `ExecutionContext.executionId + ":" + nodeId`。
- `process_info.processNodeInstanceStartTime`: 当前请求组装时间戳毫秒。
- `process_info.groupName`: 空字符串，后续接入 AB 分组路径时再补真实链路。

## 前端配置页

API 配置弹窗新增：

- `是否携带环境信息` 开关。
- `请求示例 JSON` 只读代码预览。

示例 JSON 从表单当前值实时生成：

- `requestSchema` 中每个参数进入 `params`。
- 示例值优先使用参数的 `displayName`，否则使用参数名。
- 打开环境信息时展示环境块，示例里的动态值使用易懂占位值。
- 关闭环境信息时只展示数组内对象的 `params`。

API 列表新增“环境信息”列，显示“携带”或“不携带”。

## 后端执行

`ApiCallHandler` 不再直接发送 `inputParams` map。它先解析节点配置中的 `inputParams`，再构造：

- `List.of(Map.of("params", resolvedParams))`，如果关闭环境信息。
- `List.of(Map.of("user_profile", ..., "params", resolvedParams, "callback_params", ..., "process_info", ...))`，如果开启环境信息。

`DagEngine` 已经能给特殊节点注入 nodeId。为了 API_CALL 填充 `nodeId/actionId/processNodeInstanceId`，API_CALL 也需要拿到当前节点 ID。实现方式是在 `resolveConfigWithNodeId` 的使用条件里包含 `API_CALL`。

GET 请求暂保持现有行为，不发送 body。由于环境格式是请求体数组，配置了该格式的 API 应使用 POST。

## 测试

后端：

- API_CALL 在 `includeContextPayload=0` 时发送 `[{"params": {...}}]`。
- API_CALL 在 `includeContextPayload=1` 时发送包含 `user_profile`、`params`、`callback_params`、`process_info` 的数组。
- 节点 ID 能进入 `callback_params.nodeId` 和 `process_info.processNodeInstanceId`。

前端：

- 示例 JSON 生成器在关闭环境信息时只包含 `params`。
- 示例 JSON 生成器在打开环境信息时包含环境块。
- API 配置提交时把开关转换为 `0/1`。

## 风险与处理

- 旧的 API 接收方如果期待平铺 body 会不兼容。当前版本未上线，接受破坏式调整。
- `processInstanceStartTime` 暂无真实字段。先使用组装请求时的毫秒时间戳，后续如果 ExecutionContext 增加实例开始时间，可替换为真实值。
- GET 不适合该请求体格式。配置页不强制限制，但产品说明和默认值保持 POST。
