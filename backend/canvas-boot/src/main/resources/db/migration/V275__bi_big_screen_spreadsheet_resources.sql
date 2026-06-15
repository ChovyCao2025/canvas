CREATE TABLE IF NOT EXISTS bi_big_screen (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  workspace_id BIGINT NOT NULL,
  screen_key VARCHAR(128) NOT NULL,
  name VARCHAR(128) NOT NULL,
  description VARCHAR(1000) NULL,
  size_json JSON NOT NULL,
  background_json JSON NULL,
  layout_json JSON NOT NULL,
  refresh_json JSON NULL,
  mobile_layout_json JSON NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
  version INT NOT NULL DEFAULT 1,
  created_by VARCHAR(128) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_bi_big_screen_key (tenant_id, workspace_id, screen_key),
  INDEX idx_bi_big_screen_status (tenant_id, workspace_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bi_big_screen_version (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  workspace_id BIGINT NOT NULL,
  screen_id BIGINT NOT NULL,
  screen_key VARCHAR(128) NOT NULL,
  version INT NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED',
  resource_json JSON NOT NULL,
  published_by VARCHAR(128) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_bi_big_screen_version (tenant_id, screen_id, version),
  INDEX idx_bi_big_screen_version_key (tenant_id, workspace_id, screen_key, version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bi_spreadsheet (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  workspace_id BIGINT NOT NULL,
  spreadsheet_key VARCHAR(128) NOT NULL,
  name VARCHAR(128) NOT NULL,
  description VARCHAR(1000) NULL,
  sheet_json JSON NOT NULL,
  data_binding_json JSON NULL,
  style_json JSON NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
  version INT NOT NULL DEFAULT 1,
  created_by VARCHAR(128) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_bi_spreadsheet_key (tenant_id, workspace_id, spreadsheet_key),
  INDEX idx_bi_spreadsheet_status (tenant_id, workspace_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bi_spreadsheet_version (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  workspace_id BIGINT NOT NULL,
  spreadsheet_id BIGINT NOT NULL,
  spreadsheet_key VARCHAR(128) NOT NULL,
  version INT NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED',
  resource_json JSON NOT NULL,
  published_by VARCHAR(128) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_bi_spreadsheet_version (tenant_id, spreadsheet_id, version),
  INDEX idx_bi_spreadsheet_version_key (tenant_id, workspace_id, spreadsheet_key, version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
