CREATE TABLE IF NOT EXISTS built_in_plugin_registry (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  plugin_key VARCHAR(128) NOT NULL,
  display_name VARCHAR(128) NOT NULL,
  extension_point VARCHAR(64) NOT NULL,
  compatibility_json JSON NOT NULL,
  config_schema_json JSON NULL,
  enabled TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_builtin_plugin_key (plugin_key),
  INDEX idx_builtin_plugin_extension (extension_point, enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO built_in_plugin_registry(plugin_key, display_name, extension_point, compatibility_json, config_schema_json, enabled)
VALUES
  ('wecom-channel', 'WeCom Channel Adapter', 'CHANNEL_ADAPTER', JSON_OBJECT('minCanvasVersion', '1.0.0'), JSON_OBJECT('corpId', 'string'), 0),
  ('csv-export', 'CSV Data Exporter', 'DATA_EXPORTER', JSON_OBJECT('minCanvasVersion', '1.0.0'), JSON_OBJECT('delimiter', 'string'), 1),
  ('batch-operations', 'Batch Operation Pack', 'RULE_TEMPLATE_PACK', JSON_OBJECT('minCanvasVersion', '1.0.0'), JSON_OBJECT(), 1),
  ('ai-gateway', 'AI Gateway Adapter', 'AI_GATEWAY', JSON_OBJECT('minCanvasVersion', '1.0.0'), JSON_OBJECT('provider', 'string'), 0),
  ('form-collect-node', 'Form Collect Node Handler', 'NODE_HANDLER', JSON_OBJECT('minCanvasVersion', '1.0.0'), JSON_OBJECT('fieldSchema', 'json', 'publicSubmit', 'boolean'), 1)
ON DUPLICATE KEY UPDATE
  display_name = VALUES(display_name),
  extension_point = VALUES(extension_point),
  compatibility_json = VALUES(compatibility_json),
  config_schema_json = VALUES(config_schema_json);
