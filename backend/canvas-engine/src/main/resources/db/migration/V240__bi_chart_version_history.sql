CREATE TABLE IF NOT EXISTS bi_chart_version (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  workspace_id BIGINT NOT NULL,
  chart_id BIGINT NOT NULL,
  chart_key VARCHAR(128) NOT NULL,
  version INT NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED',
  resource_json JSON NOT NULL,
  published_by VARCHAR(128) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_bi_chart_version (tenant_id, chart_id, version),
  INDEX idx_bi_chart_version_key (tenant_id, workspace_id, chart_key, version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
