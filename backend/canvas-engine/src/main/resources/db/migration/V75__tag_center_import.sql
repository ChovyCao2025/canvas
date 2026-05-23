-- V75: Tag center import metadata.
-- V74 already added the CDP tag columns; this migration only adds import
-- management tables and switches tag_definition to a single tag_code key.

ALTER TABLE tag_definition
  MODIFY COLUMN name VARCHAR(100) NOT NULL,
  MODIFY COLUMN description VARCHAR(500) NULL;

SET @idx_exists := (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'tag_definition'
    AND INDEX_NAME = 'uk_tag_code'
);
SET @ddl := IF(
  @idx_exists = 0,
  'ALTER TABLE `tag_definition` ADD UNIQUE KEY `uk_tag_code` (`tag_code`)',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists := (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'tag_definition'
    AND INDEX_NAME = 'uk_tag_code_type'
);
SET @ddl := IF(
  @idx_exists > 0,
  'ALTER TABLE `tag_definition` DROP INDEX `uk_tag_code_type`',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS identity_type (
  id BIGINT NOT NULL AUTO_INCREMENT,
  code VARCHAR(64) NOT NULL,
  name VARCHAR(100) NOT NULL,
  description VARCHAR(500) NULL,
  enabled TINYINT NOT NULL DEFAULT 1,
  allow_import TINYINT NOT NULL DEFAULT 1,
  multi_value TINYINT NOT NULL DEFAULT 0,
  priority INT NOT NULL DEFAULT 100,
  participate_mapping TINYINT NOT NULL DEFAULT 0,
  created_by VARCHAR(64) NULL,
  created_at DATETIME NULL,
  updated_at DATETIME NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_identity_type_code (code),
  INDEX idx_identity_type_enabled (enabled, allow_import)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='身份类型配置';

CREATE TABLE IF NOT EXISTS tag_value_definition (
  id BIGINT NOT NULL AUTO_INCREMENT,
  tag_code VARCHAR(64) NOT NULL,
  value VARCHAR(255) NOT NULL,
  label VARCHAR(255) NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  enabled TINYINT NOT NULL DEFAULT 1,
  source VARCHAR(32) NOT NULL DEFAULT 'MANUAL',
  description VARCHAR(500) NULL,
  created_at DATETIME NULL,
  updated_at DATETIME NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_tag_value (tag_code, value),
  INDEX idx_tag_value_enabled (tag_code, enabled, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标签值字典';

CREATE TABLE IF NOT EXISTS tag_import_batch (
  id BIGINT NOT NULL AUTO_INCREMENT,
  source_type VARCHAR(32) NOT NULL,
  status VARCHAR(32) NOT NULL,
  file_name VARCHAR(255) NULL,
  external_url VARCHAR(1000) NULL,
  total_rows INT NOT NULL DEFAULT 0,
  success_rows INT NOT NULL DEFAULT 0,
  failed_rows INT NOT NULL DEFAULT 0,
  created_by VARCHAR(64) NULL,
  started_at DATETIME NULL,
  finished_at DATETIME NULL,
  error_message VARCHAR(1000) NULL,
  created_at DATETIME NULL,
  updated_at DATETIME NULL,
  PRIMARY KEY (id),
  INDEX idx_import_batch_status (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标签导入批次';

CREATE TABLE IF NOT EXISTS tag_import_error (
  id BIGINT NOT NULL AUTO_INCREMENT,
  batch_id BIGINT NOT NULL,
  row_no INT NOT NULL,
  raw_payload TEXT NULL,
  error_code VARCHAR(64) NOT NULL,
  error_msg VARCHAR(1000) NOT NULL,
  created_at DATETIME NULL,
  PRIMARY KEY (id),
  INDEX idx_import_error_batch (batch_id, row_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标签导入错误行';

CREATE TABLE IF NOT EXISTS tag_import_source (
  id BIGINT NOT NULL AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL,
  url VARCHAR(1000) NOT NULL,
  method VARCHAR(16) NOT NULL DEFAULT 'GET',
  headers_json TEXT NULL,
  body_template TEXT NULL,
  page_param VARCHAR(64) NULL,
  page_size_param VARCHAR(64) NULL,
  page_size INT NOT NULL DEFAULT 500,
  records_path VARCHAR(255) NOT NULL DEFAULT '$',
  field_mapping TEXT NOT NULL,
  enabled TINYINT NOT NULL DEFAULT 1,
  created_by VARCHAR(64) NULL,
  created_at DATETIME NULL,
  updated_at DATETIME NULL,
  PRIMARY KEY (id),
  INDEX idx_tag_import_source_enabled (enabled, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标签外部拉取源';

INSERT IGNORE INTO identity_type
  (code, name, description, enabled, allow_import, multi_value, priority, participate_mapping, created_by, created_at, updated_at)
VALUES
  ('user_id', '用户ID', '系统内部用户ID', 1, 1, 0, 10, 0, 'system', NOW(), NOW()),
  ('mobile', '手机号', '用户手机号', 1, 1, 0, 20, 0, 'system', NOW(), NOW()),
  ('open_id', 'OpenID', '渠道OpenID', 1, 1, 1, 30, 0, 'system', NOW(), NOW()),
  ('email', '邮箱', '用户邮箱', 1, 1, 0, 40, 0, 'system', NOW(), NOW()),
  ('member_no', '会员号', '业务会员号', 1, 1, 0, 50, 0, 'system', NOW(), NOW());

INSERT IGNORE INTO tag_value_definition (tag_code, value, label, sort_order, enabled, source, created_at, updated_at)
VALUES
  ('new_user', '1', '是', 10, 1, 'MANUAL', NOW(), NOW()),
  ('new_user', '0', '否', 20, 1, 'MANUAL', NOW(), NOW()),
  ('high_value', 'VIP', 'VIP', 10, 1, 'MANUAL', NOW(), NOW()),
  ('high_value', 'NORMAL', '普通', 20, 1, 'MANUAL', NOW(), NOW()),
  ('churn_risk', 'HIGH', '高风险', 10, 1, 'MANUAL', NOW(), NOW()),
  ('churn_risk', 'LOW', '低风险', 20, 1, 'MANUAL', NOW(), NOW());
