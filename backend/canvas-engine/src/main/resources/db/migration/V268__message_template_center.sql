CREATE TABLE IF NOT EXISTS message_template (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  template_code VARCHAR(128) NOT NULL,
  display_name VARCHAR(128) NOT NULL,
  channel VARCHAR(64) NOT NULL,
  body TEXT NOT NULL,
  variable_schema_json JSON NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
  created_by VARCHAR(128) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_message_template_code (tenant_id, template_code),
  INDEX idx_message_template_search (tenant_id, channel, status, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
