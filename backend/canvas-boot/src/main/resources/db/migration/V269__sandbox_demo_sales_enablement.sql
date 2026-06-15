CREATE TABLE IF NOT EXISTS demo_sandbox (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  demo_name VARCHAR(128) NOT NULL,
  demo_marker VARCHAR(128) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  expires_at DATETIME NOT NULL,
  last_reset_at DATETIME NULL,
  last_reset_by VARCHAR(128) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_demo_sandbox_tenant (tenant_id),
  UNIQUE KEY uk_demo_sandbox_marker (demo_marker),
  INDEX idx_demo_sandbox_expiry (status, expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
