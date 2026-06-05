CREATE TABLE IF NOT EXISTS bi_resource_favorite (
  id BIGINT NOT NULL AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  workspace_id BIGINT NOT NULL,
  resource_type VARCHAR(32) NOT NULL,
  resource_key VARCHAR(128) NOT NULL,
  username VARCHAR(128) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_bi_resource_favorite (tenant_id, workspace_id, resource_type, resource_key, username),
  INDEX idx_bi_resource_favorite_user (tenant_id, workspace_id, username, resource_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='BI resource favorites by user';
