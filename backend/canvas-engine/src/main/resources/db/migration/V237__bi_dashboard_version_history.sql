CREATE TABLE IF NOT EXISTS bi_dashboard_version (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  workspace_id BIGINT NOT NULL,
  dashboard_id BIGINT NOT NULL,
  dashboard_key VARCHAR(128) NOT NULL,
  version INT NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED',
  preset_json JSON NOT NULL,
  published_by VARCHAR(128) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_bi_dashboard_version (tenant_id, dashboard_id, version),
  INDEX idx_bi_dashboard_version_key (tenant_id, workspace_id, dashboard_key, version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
