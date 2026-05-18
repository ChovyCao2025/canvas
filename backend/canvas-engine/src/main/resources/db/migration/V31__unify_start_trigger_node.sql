-- V31: 迁移存量测试画布至统一 START 节点结构，禁用已废弃的 BEHAVIOR_TRIGGER 节点类型
--
-- 背景：后端 findTriggerNode 已只识别 START 节点（由 config.triggerType 路由），
--       独立的 BEHAVIOR_TRIGGER 触发节点不再被识别为 DAG 入口。
--       前端配置面板已通过 START 节点的 triggerType 字段提供等价配置能力（V23 起）。

-- 1. 禁用 BEHAVIOR_TRIGGER（前端节点面板不再显示，新画布不可拖拽）
UPDATE node_type_registry SET enabled = 0 WHERE type_key = 'BEHAVIOR_TRIGGER';

-- 2. 迁移测试画布 canvas_id=7：去掉 BEHAVIOR_IN_APP 独立节点，
--    将 triggerType 和 eventCode 直接写入 START 节点 config
UPDATE canvas_version
SET graph_json = '{"nodes":[{"id":"9eaa4de33073","type":"START","name":"START","category":"流程控制","x":0,"y":0,"config":{"triggerType":"EVENT","eventCode":"ORDER_COMPLETE","nextNodeId":"ev_api"}},{"id":"ev_api","type":"API_CALL","name":"Echo 接口验证","category":"其他","x":0,"y":148,"config":{"apiKey":"query_user_info","outputPrefix":"echo","nextNodeId":"ev_end","inputParams":{"name":"${orderId}","orderId":"${amount}","time":"${orderId}"}}},{"id":"ev_end","type":"END","name":"结束","category":"流程控制","x":0,"y":296,"config":{}}]}'
WHERE canvas_id = 7;
