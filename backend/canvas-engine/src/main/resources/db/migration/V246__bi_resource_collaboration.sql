CREATE TABLE IF NOT EXISTS bi_resource_comment (
  id BIGINT NOT NULL AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  workspace_id BIGINT NOT NULL,
  resource_type VARCHAR(32) NOT NULL,
  resource_key VARCHAR(128) NOT NULL,
  widget_key VARCHAR(128) NULL,
  comment_text TEXT NOT NULL,
  created_by VARCHAR(128) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at DATETIME NULL,
  PRIMARY KEY (id),
  INDEX idx_bi_resource_comment_resource (tenant_id, workspace_id, resource_type, resource_key, deleted_at, created_at),
  INDEX idx_bi_resource_comment_widget (tenant_id, workspace_id, resource_type, resource_key, widget_key, deleted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='BI resource comments and widget annotations';

CREATE TABLE IF NOT EXISTS bi_resource_lock (
  id BIGINT NOT NULL AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  workspace_id BIGINT NOT NULL,
  resource_type VARCHAR(32) NOT NULL,
  resource_key VARCHAR(128) NOT NULL,
  lock_token VARCHAR(128) NOT NULL,
  locked_by VARCHAR(128) NOT NULL,
  locked_at DATETIME NOT NULL,
  expires_at DATETIME NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_bi_resource_lock (tenant_id, workspace_id, resource_type, resource_key),
  INDEX idx_bi_resource_lock_expiry (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='BI resource edit locks';
