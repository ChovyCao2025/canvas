CREATE TABLE IF NOT EXISTS bi_publish_approval (
  id BIGINT NOT NULL AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  workspace_id BIGINT NOT NULL,
  resource_type VARCHAR(32) NOT NULL,
  resource_key VARCHAR(128) NOT NULL,
  status VARCHAR(32) NOT NULL,
  reason VARCHAR(512) NULL,
  requested_by VARCHAR(128) NOT NULL,
  requested_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  reviewed_by VARCHAR(128) NULL,
  reviewed_at DATETIME NULL,
  review_comment VARCHAR(512) NULL,
  PRIMARY KEY (id),
  KEY idx_bi_publish_approval_resource (tenant_id, workspace_id, resource_type, resource_key, status, requested_at),
  KEY idx_bi_publish_approval_status (tenant_id, workspace_id, status, requested_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='BI resource publish approval requests';
