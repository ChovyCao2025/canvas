-- V27: Merge BEHAVIOR_IN_APP + DIRECT_CALL into BEHAVIOR_TRIGGER node type.
INSERT INTO node_type_registry
  (type_key, type_name, category, handler_class,
   config_schema, output_schema, is_trigger, is_terminal, description, enabled)
VALUES (
  'BEHAVIOR_TRIGGER',
  '行为触发',
  '行为策略',
  'org.chovy.canvas.engine.handlers.BehaviorTriggerHandler',
  '[{"key":"triggerType","label":"触发方式","type":"radio","required":true,"options":[{"label":"端内行为事件（监听 MQ）","value":"inapp"},{"label":"业务直调（HTTP 推送）","value":"direct"}]},{"key":"eventCode","label":"触发事件","type":"select","dataSource":"/meta/event-definitions","required":true,"showWhen":"triggerType==inapp"},{"key":"_attrHint","label":"可用上下文变量","type":"event-attr-preview","showWhen":"triggerType==inapp"},{"key":"inputParams","label":"入参定义","type":"param-define-list","showWhen":"triggerType==direct"}]',
  '[]',
  1, 0,
  '通过端内行为事件或业务 HTTP 直调触发旅程。',
  1
);

UPDATE node_type_registry SET enabled = 0
WHERE type_key IN ('BEHAVIOR_IN_APP', 'DIRECT_CALL');
