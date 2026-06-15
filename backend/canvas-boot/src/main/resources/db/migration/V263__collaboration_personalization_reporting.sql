CREATE TABLE IF NOT EXISTS user_workspace_preference (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  user_id VARCHAR(128) NOT NULL,
  preference_key VARCHAR(128) NOT NULL,
  preference_json JSON NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_workspace_preference (tenant_id, user_id, preference_key),
  INDEX idx_user_workspace_preference_user (tenant_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
