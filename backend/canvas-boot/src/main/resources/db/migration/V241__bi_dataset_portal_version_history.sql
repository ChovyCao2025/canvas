CREATE TABLE IF NOT EXISTS bi_dataset_version (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  workspace_id BIGINT NOT NULL,
  dataset_id BIGINT NOT NULL,
  dataset_key VARCHAR(128) NOT NULL,
  version INT NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED',
  resource_json JSON NOT NULL,
  published_by VARCHAR(128) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_bi_dataset_version (tenant_id, dataset_id, version),
  INDEX idx_bi_dataset_version_key (tenant_id, workspace_id, dataset_key, version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bi_portal_version (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  workspace_id BIGINT NOT NULL,
  portal_id BIGINT NOT NULL,
  portal_key VARCHAR(128) NOT NULL,
  version INT NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED',
  resource_json JSON NOT NULL,
  published_by VARCHAR(128) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_bi_portal_version (tenant_id, portal_id, version),
  INDEX idx_bi_portal_version_key (tenant_id, workspace_id, portal_key, version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
