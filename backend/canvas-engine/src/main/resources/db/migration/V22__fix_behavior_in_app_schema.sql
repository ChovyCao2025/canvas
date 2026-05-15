-- V22: 修正 BEHAVIOR_IN_APP 配置 schema
-- 事件属性不允许用户填值，改为只读预览（运行时由上报内容决定）
UPDATE `node_type_registry`
SET `config_schema` = '[
  {"key":"eventCode","label":"触发事件","type":"select","dataSource":"/meta/event-definitions","required":true},
  {"key":"_attrHint","label":"可用上下文变量","type":"event-attr-preview"}
]'
WHERE `type_key` = 'BEHAVIOR_IN_APP';
