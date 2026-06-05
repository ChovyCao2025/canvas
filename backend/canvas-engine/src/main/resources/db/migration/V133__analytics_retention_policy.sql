CREATE TABLE IF NOT EXISTS analytics_retention_policy (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  record_kind VARCHAR(32) NOT NULL,
  retention_days INT NOT NULL,
  action VARCHAR(20) NOT NULL,
  max_batch_size INT NOT NULL DEFAULT 1000,
  legal_hold_behavior VARCHAR(20) NOT NULL DEFAULT 'SKIP',
  enabled TINYINT NOT NULL DEFAULT 1,
  updated_by VARCHAR(128) NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_analytics_retention_policy (tenant_id, record_kind)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS analytics_retention_run (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  record_kind VARCHAR(32) NOT NULL,
  action VARCHAR(20) NOT NULL,
  dry_run TINYINT NOT NULL DEFAULT 1,
  scanned_count BIGINT NOT NULL DEFAULT 0,
  archived_count BIGINT NOT NULL DEFAULT 0,
  deleted_count BIGINT NOT NULL DEFAULT 0,
  skipped_count BIGINT NOT NULL DEFAULT 0,
  failed_count BIGINT NOT NULL DEFAULT 0,
  started_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  finished_at DATETIME NULL,
  INDEX idx_analytics_retention_run (tenant_id, record_kind, started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
