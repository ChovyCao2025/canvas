-- V26: Merge TAGGER_OFFLINE + TAGGER_REALTIME into TAGGER node type.
-- Old types kept with enabled=0 so existing canvases continue to render.
INSERT INTO node_type_registry
  (type_key, type_name, category, handler_class,
   config_schema, output_schema, is_trigger, is_terminal, description, enabled)
VALUES (
  'TAGGER',
  'Tagger 标签',
  '行为策略',
  'org.chovy.canvas.engine.handlers.TaggerHandler',
  '[{"key":"mode","label":"标签模式","type":"radio","required":true,"options":[{"label":"实时触发（监听 MQ 事件）","value":"realtime"},{"label":"离线打标（流程内执行）","value":"offline"}]},{"key":"tagCodeKey","label":"标签","type":"select","dataSource":"/meta/tagger-tags","required":true}]',
  '[]',
  0, 0,
  '实时或离线方式对用户打 Tagger 标签。',
  1
);

UPDATE node_type_registry SET enabled = 0
WHERE type_key IN ('TAGGER_OFFLINE', 'TAGGER_REALTIME');
