INSERT INTO node_type_registry
  (type_key, type_name, category, handler_class,
   config_schema, output_schema, outlet_schema, summary_template, runtime_policy_schema,
   risk_level, is_trigger, is_terminal, description, enabled)
VALUES
('SUBFLOW','子流程','结构复用','org.chovy.canvas.engine.handlers.SubflowHandler',
 '[{"key":"subflowId","label":"子流程ID","type":"number","required":true},{"key":"outputPrefix","label":"输出前缀","type":"text","defaultValue":"sf"},{"key":"inputMapping","label":"入参映射","type":"key-value"}]',
 '[]','[{"id":"success","label":"继续","color":"#52c41a","targetField":"nextNodeId"}]',
 '子流程（{{subflowId}}）','[]','HIGH',0,0,'产品化子流程节点，复用现有 SUB_FLOW_REF 执行能力。',1),
('GROUP','分组','结构复用','org.chovy.canvas.engine.handlers.GroupHandler',
 '[]','[]','[{"id":"success","label":"继续","color":"#52c41a","targetField":"nextNodeId"}]',
 '分组','[]','LOW',0,0,'画布 UI 分组节点，不承载业务副作用。',1),
('TEMPLATE_NODE','模板节点','结构复用','org.chovy.canvas.engine.handlers.TemplateNodeHandler',
 '[{"key":"templateKey","label":"模板","type":"text","required":true}]',
 '[]','[{"id":"success","label":"继续","color":"#52c41a","targetField":"nextNodeId"}]',
 '模板节点（{{templateKey}}）','[]','LOW',0,0,'流程模板占位节点；前端可在插入时展开为真实节点。',1);
