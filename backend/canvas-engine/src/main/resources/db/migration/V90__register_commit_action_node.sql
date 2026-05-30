INSERT INTO node_type_registry
  (type_key, type_name, category, handler_class,
   config_schema, output_schema, outlet_schema, summary_template, runtime_policy_schema,
   risk_level, is_trigger, is_terminal, description, enabled)
VALUES
('COMMIT_ACTION','提交动作','外部动作','org.chovy.canvas.engine.handlers.CommitActionHandler',
 '[{"key":"actionType","label":"动作类型","type":"select","required":true,"options":[{"label":"发放代金券","value":"COUPON"},{"label":"积分操作","value":"POINTS_OPERATION"}]},{"key":"couponTypeKey","label":"券类型","type":"select","dataSource":"/meta/coupon-types"},{"key":"params","label":"券参数","type":"dynamic-params","paramsSource":"couponTypeKey"},{"key":"operation","label":"积分操作","type":"select","defaultValue":"GRANT","options":[{"label":"发放","value":"GRANT"},{"label":"扣减","value":"DEDUCT"}]},{"key":"points","label":"积分","type":"number"},{"key":"pointsType","label":"积分类型","type":"text","defaultValue":"MARKETING"},{"key":"idempotencyKey","label":"幂等键","type":"text"}]',
 '[{"name":"couponAmount","type":"NUMBER","label":"券面额"},{"name":"couponId","type":"STRING","label":"券ID"},{"name":"pointsLedgerId","type":"NUMBER","label":"积分流水ID"},{"name":"duplicate","type":"BOOLEAN","label":"是否重复"}]',
 '[{"id":"success","label":"成功","color":"#52c41a","targetField":"nextNodeId"}]',
 '提交动作（{{actionType}}）','[]','HIGH',0,0,'执行已提交即产生关键副作用的动作；成功后后续失败按防资损规则收敛。',1)
ON DUPLICATE KEY UPDATE
  type_name = VALUES(type_name),
  category = VALUES(category),
  handler_class = VALUES(handler_class),
  config_schema = VALUES(config_schema),
  output_schema = VALUES(output_schema),
  outlet_schema = VALUES(outlet_schema),
  summary_template = VALUES(summary_template),
  runtime_policy_schema = VALUES(runtime_policy_schema),
  risk_level = VALUES(risk_level),
  is_trigger = VALUES(is_trigger),
  is_terminal = VALUES(is_terminal),
  description = VALUES(description),
  enabled = VALUES(enabled);

UPDATE node_type_registry
SET enabled = 0,
    description = CONCAT(COALESCE(description, ''), '（已合并到 COMMIT_ACTION，保留用于历史画布兼容）')
WHERE type_key IN ('COUPON', 'POINTS_OPERATION');
