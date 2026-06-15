CREATE TABLE IF NOT EXISTS bi_resource_ownership (
  id BIGINT NOT NULL AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  workspace_id BIGINT NOT NULL,
  resource_type VARCHAR(32) NOT NULL,
  resource_key VARCHAR(128) NOT NULL,
  owner_user VARCHAR(128) NOT NULL,
  transferred_by VARCHAR(128) NULL,
  transferred_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_bi_resource_ownership (tenant_id, workspace_id, resource_type, resource_key),
  INDEX idx_bi_resource_ownership_owner (tenant_id, workspace_id, owner_user)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='BI resource owner assignment and transfer audit';
