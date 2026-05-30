-- V82: Restore DIRECT_CALL as the visible external API entry node.
--
-- The runtime direct execution endpoint triggers DIRECT_CALL nodes. API_TRIGGER
-- is registered but not wired to a public execution endpoint, so keep it hidden
-- until it has a complete product flow.

UPDATE node_type_registry
SET type_name = 'API入口',
    category = '入口节点',
    handler_class = 'org.chovy.canvas.engine.handlers.DirectCallHandler',
    config_schema = '[
      {"key":"eventCode","label":"关联事件（可选）","type":"select","dataSource":"/meta/event-definitions","required":false},
      {"key":"eventParams","label":"入参说明","type":"api-input-params","apiKeyField":"eventCode","defsSource":"/meta/event-definitions","required":false}
    ]',
    output_schema = '[]',
    outlet_schema = '[{"id":"success","label":"继续","color":"#52c41a","targetField":"nextNodeId"}]',
    summary_template = 'API入口',
    risk_level = 'MEDIUM',
    is_trigger = 1,
    is_terminal = 0,
    description = '外部系统通过直调接口触发画布；可选绑定事件定义以说明入参。',
    enabled = 1
WHERE type_key = 'DIRECT_CALL';

UPDATE node_type_registry
SET enabled = 0
WHERE type_key = 'API_TRIGGER';
