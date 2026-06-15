-- V32: 恢复清晰的节点职责划分，统一事件触发节点命名为 EVENT_TRIGGER
--   - START = 纯流程入口节点，无业务含义，schema 不含触发方式配置
--   - EVENT_TRIGGER = 事件触发节点（独立节点，配置 eventCode），原名 BEHAVIOR_TRIGGER
--   - 测试画布 canvas_id=7 恢复为 START → EVENT_TRIGGER → API_CALL → END 结构

-- 1. 恢复 START 节点 schema：移除 triggerType 等业务字段，回归纯流程入口
UPDATE node_type_registry
SET config_schema  = '[]',
    description    = '流程入口节点，每个画布必须包含且仅包含一个 START 节点。本节点无业务配置，直接连接下游触发节点。'
WHERE type_key = 'START';

-- 2. 重命名 BEHAVIOR_TRIGGER → EVENT_TRIGGER（语义更清晰）
UPDATE node_type_registry
SET type_key   = 'EVENT_TRIGGER',
    type_name  = '事件触发',
    enabled    = 1,
    description = '监听业务事件（如下单、支付完成）触发画布执行。配置 eventCode 选择订阅的事件类型。'
WHERE type_key = 'BEHAVIOR_TRIGGER';

-- 3. 修复测试画布 canvas_id=7：使用 EVENT_TRIGGER 节点承载事件配置
UPDATE canvas_version
SET graph_json = '{"nodes":[{"id":"9eaa4de33073","type":"START","name":"START","category":"流程控制","x":0,"y":0,"config":{"nextNodeId":"ev_trigger"}},{"id":"ev_trigger","type":"EVENT_TRIGGER","name":"订单完成事件","category":"行为策略","x":0,"y":148,"config":{"triggerType":"inapp","eventCode":"ORDER_COMPLETE","nextNodeId":"ev_api"}},{"id":"ev_api","type":"API_CALL","name":"Echo 接口验证","category":"其他","x":0,"y":296,"config":{"apiKey":"query_user_info","outputPrefix":"echo","nextNodeId":"ev_end","inputParams":{"name":"$${orderId}","orderId":"$${amount}","time":"$${orderId}"}}},{"id":"ev_end","type":"END","name":"结束","category":"流程控制","x":0,"y":444,"config":{}}]}'
WHERE canvas_id = 7;
