CREATE TABLE IF NOT EXISTS approval_lark_user_identity (
  id BIGINT NOT NULL AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  username VARCHAR(128) NOT NULL,
  lark_open_id VARCHAR(128) NULL,
  lark_user_id VARCHAR(128) NULL,
  lark_department_id VARCHAR(128) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_approval_lark_user_identity_user (tenant_id, username),
  KEY idx_approval_lark_user_identity_open_id (tenant_id, lark_open_id),
  KEY idx_approval_lark_user_identity_user_id (tenant_id, lark_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Canvas approval user mapping to Lark approval identity';
