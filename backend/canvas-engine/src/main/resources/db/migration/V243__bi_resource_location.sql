CREATE TABLE IF NOT EXISTS bi_resource_location (
  id BIGINT NOT NULL AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  workspace_id BIGINT NOT NULL,
  resource_type VARCHAR(32) NOT NULL,
  resource_key VARCHAR(128) NOT NULL,
  folder_key VARCHAR(255) NULL,
  sort_order INT NOT NULL DEFAULT 0,
  moved_by VARCHAR(128) NULL,
  moved_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_bi_resource_location (tenant_id, workspace_id, resource_type, resource_key),
  INDEX idx_bi_resource_location_folder (tenant_id, workspace_id, resource_type, folder_key, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='BI resource folder and ordering location';
