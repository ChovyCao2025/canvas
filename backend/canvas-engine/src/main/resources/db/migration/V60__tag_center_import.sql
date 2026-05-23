ALTER TABLE tag_definition
  DROP INDEX uk_tag_code_type,
  ADD COLUMN value_type VARCHAR(16) NOT NULL DEFAULT 'STRING' COMMENT 'STRING / NUMBER / BOOLEAN' AFTER tag_type,
  MODIFY COLUMN name VARCHAR(100) NOT NULL,
  MODIFY COLUMN description VARCHAR(500) NULL;

ALTER TABLE tag_definition
  ADD UNIQUE KEY uk_tag_code (tag_code);

CREATE TABLE identity_type (
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

CREATE TABLE tag_value_definition (
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

CREATE TABLE user_tag_current (
  id BIGINT NOT NULL AUTO_INCREMENT,
  id_type VARCHAR(64) NOT NULL,
  id_value VARCHAR(255) NOT NULL,
  tag_code VARCHAR(64) NOT NULL,
  tag_value VARCHAR(255) NOT NULL,
  tag_time DATETIME NULL,
  source_type VARCHAR(32) NOT NULL,
  source_batch_id BIGINT NULL,
  updated_at DATETIME NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_tag_current (id_type, id_value, tag_code),
  INDEX idx_user_tag_tag (tag_code, tag_value),
  INDEX idx_user_tag_identity (id_type, id_value)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户当前标签';

CREATE TABLE tag_import_batch (
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

CREATE TABLE tag_import_error (
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

CREATE TABLE tag_import_source (
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

INSERT INTO identity_type
  (code, name, description, enabled, allow_import, multi_value, priority, participate_mapping, created_by, created_at, updated_at)
VALUES
  ('user_id', '用户ID', '系统内部用户ID', 1, 1, 0, 10, 0, 'system', NOW(), NOW()),
  ('mobile', '手机号', '用户手机号', 1, 1, 0, 20, 0, 'system', NOW(), NOW()),
  ('open_id', 'OpenID', '渠道OpenID', 1, 1, 1, 30, 0, 'system', NOW(), NOW()),
  ('email', '邮箱', '用户邮箱', 1, 1, 0, 40, 0, 'system', NOW(), NOW()),
  ('member_no', '会员号', '业务会员号', 1, 1, 0, 50, 0, 'system', NOW(), NOW());

INSERT INTO tag_value_definition (tag_code, value, label, sort_order, enabled, source, created_at, updated_at)
VALUES
  ('new_user', '1', '是', 10, 1, 'MANUAL', NOW(), NOW()),
  ('new_user', '0', '否', 20, 1, 'MANUAL', NOW(), NOW()),
  ('high_value', 'VIP', 'VIP', 10, 1, 'MANUAL', NOW(), NOW()),
  ('high_value', 'NORMAL', '普通', 20, 1, 'MANUAL', NOW(), NOW()),
  ('churn_risk', 'HIGH', '高风险', 10, 1, 'MANUAL', NOW(), NOW()),
  ('churn_risk', 'LOW', '低风险', 20, 1, 'MANUAL', NOW(), NOW());
