-- V33: 简化 EVENT_TRIGGER 节点 schema，移除 triggerType 字段
--
-- EVENT_TRIGGER 只负责「订阅指定 eventCode 的业务事件」，
-- 触发方式（HTTP上报 / MQ）由上层基础设施决定，节点本身无需配置。

UPDATE node_type_registry
SET config_schema = '[
  {"key":"eventCode","label":"触发事件","type":"select","dataSource":"/meta/event-definitions","required":true},
  {"key":"_attrHint","label":"可用上下文变量","type":"event-attr-preview"}
]'
WHERE type_key = 'EVENT_TRIGGER';

-- 清理测试画布中 EVENT_TRIGGER 节点 config 里残留的 triggerType 字段
UPDATE canvas_version
SET graph_json = '{"nodes":[{"id":"9eaa4de33073","type":"START","name":"START","category":"流程控制","x":0,"y":0,"config":{"nextNodeId":"ev_trigger"}},{"id":"ev_trigger","type":"EVENT_TRIGGER","name":"订单完成事件","category":"行为策略","x":0,"y":148,"config":{"eventCode":"ORDER_COMPLETE","nextNodeId":"ev_api"}},{"id":"ev_api","type":"API_CALL","name":"Echo 接口验证","category":"其他","x":0,"y":296,"config":{"apiKey":"query_user_info","outputPrefix":"echo","nextNodeId":"ev_end","inputParams":{"name":"${orderId}","orderId":"${amount}","time":"${orderId}"}}},{"id":"ev_end","type":"END","name":"结束","category":"流程控制","x":0,"y":444,"config":{}}]}'
WHERE canvas_id = 7;
