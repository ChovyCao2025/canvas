CREATE TABLE IF NOT EXISTS analytics_funnel_definition (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  funnel_key VARCHAR(128) NOT NULL,
  version INT NOT NULL,
  name VARCHAR(128) NOT NULL,
  steps_json JSON NOT NULL,
  enabled TINYINT NOT NULL DEFAULT 1,
  created_by VARCHAR(128) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_analytics_funnel_version (tenant_id, funnel_key, version),
  INDEX idx_analytics_funnel_lookup (tenant_id, funnel_key, enabled, version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS analytics_alert_rule (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  rule_key VARCHAR(128) NOT NULL,
  name VARCHAR(128) NOT NULL,
  threshold_json JSON NOT NULL,
  enabled TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_analytics_alert_rule (tenant_id, rule_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS analytics_export_job (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  report_type VARCHAR(64) NOT NULL,
  query_json JSON NOT NULL,
  row_limit INT NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'QUEUED',
  file_url VARCHAR(500) NULL,
  error_message VARCHAR(1000) NULL,
  created_by VARCHAR(128) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_analytics_export_job (tenant_id, status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
