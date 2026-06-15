CREATE TABLE IF NOT EXISTS bi_permission_request (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  workspace_id BIGINT NOT NULL,
  resource_type VARCHAR(64) NOT NULL,
  resource_key VARCHAR(128) NOT NULL,
  requested_action VARCHAR(64) NOT NULL,
  requested_by VARCHAR(128) NOT NULL,
  requested_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  reason VARCHAR(512) NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  reviewed_by VARCHAR(128) NULL,
  reviewed_at DATETIME NULL,
  review_comment VARCHAR(512) NULL,
  granted_permission_id BIGINT NULL,
  KEY idx_bi_permission_request_status (tenant_id, workspace_id, status, requested_at),
  KEY idx_bi_permission_request_resource (tenant_id, workspace_id, resource_type, resource_key, status),
  KEY idx_bi_permission_request_requester (tenant_id, requested_by, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
