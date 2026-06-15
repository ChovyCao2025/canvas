CREATE TABLE `connected_content_cache` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT,
  `tenant_id`      BIGINT       NULL,
  `cache_key`      VARCHAR(128) NOT NULL,
  `url_hash`       VARCHAR(64)  NOT NULL,
  `request_hash`   VARCHAR(64)  NOT NULL,
  `response_json`  JSON         NULL,
  `status`         VARCHAR(32)  NOT NULL,
  `expires_at`     DATETIME     NOT NULL,
  `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_connected_content_cache_key` (`tenant_id`, `cache_key`),
  KEY `idx_connected_content_url` (`tenant_id`, `url_hash`, `expires_at`),
  KEY `idx_connected_content_expire` (`status`, `expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Connected Content 节点响应缓存';

INSERT INTO node_type_registry
  (type_key, type_name, category, handler_class, config_schema, output_schema, outlet_schema, summary_template, runtime_policy_schema, risk_level, is_trigger, is_terminal, description, enabled)
VALUES
  ('CONNECTED_CONTENT','连接内容','数据操作','org.chovy.canvas.engine.handlers.ConnectedContentHandler',
   '[{"key":"url","label":"内容 URL","type":"text","required":true},{"key":"method","label":"请求方法","type":"select","required":true,"defaultValue":"GET","options":[{"label":"GET","value":"GET"},{"label":"POST","value":"POST"}]},{"key":"headers","label":"请求头","type":"key-value","required":false},{"key":"requestBody","label":"请求体","type":"multi-text","required":false},{"key":"cacheTtlSeconds","label":"缓存秒数","type":"number","defaultValue":300},{"key":"timeoutMs","label":"超时毫秒","type":"number","defaultValue":2000},{"key":"maxBytes","label":"最大字节","type":"number","defaultValue":65536},{"key":"jsonPathMappings","label":"字段映射","type":"json-path-mapping-list","required":false},{"key":"timeoutNodeId","label":"超时分支","type":"node-select","required":false},{"key":"failNodeId","label":"失败分支","type":"node-select","required":false}]',
   '[{"fieldKey":"connectedContentStatus","fieldName":"内容状态","dataType":"STRING"},{"fieldKey":"connectedContentCacheHit","fieldName":"缓存命中","dataType":"BOOLEAN"},{"fieldKey":"connectedContentBody","fieldName":"内容响应","dataType":"OBJECT"}]',
   '[{"id":"success","label":"成功","color":"#52c41a","targetField":"nextNodeId"},{"id":"timeout","label":"超时","color":"#faad14","targetField":"timeoutNodeId"},{"id":"fail","label":"失败","color":"#f5222d","targetField":"failNodeId"}]',
   '连接内容（{{url}}）',
   '[]',
   'MEDIUM',
   0,0,'安全拉取外部内容，支持缓存、超时、大小上限和 JSON 字段映射。',1)
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
  enabled = VALUES(enabled);
