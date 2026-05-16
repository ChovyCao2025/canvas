-- V29: MQ_TRIGGER topics now loaded from mq_message_definition table.
-- Seed original 3 hardcoded topics so existing canvases keep working.
INSERT INTO mq_message_definition
  (name, message_code, topic, request_schema, description, enabled, created_by, created_at, updated_at)
VALUES
  ('机票订单状态变化',  'flight_order_status_change',  'flight_order_status_change',  '[]', '机票订单状态变更事件', 1, 'system', NOW(), NOW()),
  ('酒店订单状态变化',  'hotel_order_status_change',   'hotel_order_status_change',   '[]', '酒店订单状态变更事件', 1, 'system', NOW(), NOW()),
  ('火车票订单状态变化','train_order_status_change',   'train_order_status_change',   '[]', '火车票订单状态变更事件',1, 'system', NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

UPDATE node_type_registry
SET config_schema = '[{"key":"messageCodeKey","label":"消息类型","type":"select","dataSource":"/meta/mq-definitions","required":true},{"key":"validateResult","label":"开启消息校验","type":"toggle"},{"key":"validateRules","label":"校验规则","type":"condition-rule-list","visible":"validateResult==true"}]'
WHERE type_key = 'MQ_TRIGGER';
