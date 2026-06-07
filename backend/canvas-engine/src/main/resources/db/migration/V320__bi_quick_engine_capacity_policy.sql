CREATE TABLE IF NOT EXISTS bi_quick_engine_capacity_policy (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  enabled TINYINT(1) NOT NULL DEFAULT 0,
  capacity_limit_rows BIGINT NOT NULL DEFAULT 1000000,
  warning_threshold_percent INT NOT NULL DEFAULT 80,
  critical_threshold_percent INT NOT NULL DEFAULT 95,
  notification_channels JSON NULL,
  notification_receivers JSON NULL,
  updated_by VARCHAR(128) NULL,
  created_at DATETIME NULL,
  updated_at DATETIME NULL,
  UNIQUE KEY uk_bi_quick_engine_capacity_policy_tenant (tenant_id),
  KEY idx_bi_quick_engine_capacity_policy_updated (tenant_id, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='BI quick engine capacity alert policy';
