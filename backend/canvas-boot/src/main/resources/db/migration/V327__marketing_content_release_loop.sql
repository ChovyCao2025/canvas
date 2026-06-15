CREATE TABLE IF NOT EXISTS marketing_content_release (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  release_key VARCHAR(128) NOT NULL,
  source_type VARCHAR(32) NOT NULL,
  source_key VARCHAR(128) NOT NULL,
  source_version INT NOT NULL,
  channel VARCHAR(32) NOT NULL,
  status VARCHAR(32) NOT NULL,
  snapshot_json JSON NOT NULL,
  asset_refs_json JSON NOT NULL,
  checksum_sha256 VARCHAR(128) NOT NULL,
  rollback_reason TEXT NULL,
  created_by VARCHAR(128) NOT NULL,
  published_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_marketing_content_release_version (tenant_id, release_key, source_version),
  INDEX idx_marketing_content_release_latest (tenant_id, source_type, source_key, status, published_at),
  INDEX idx_marketing_content_release_key (tenant_id, release_key, status, published_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS marketing_content_release_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  release_id BIGINT NULL,
  item_type VARCHAR(32) NOT NULL,
  item_key VARCHAR(128) NOT NULL,
  item_status VARCHAR(32) NOT NULL,
  snapshot_json JSON NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_marketing_content_release_item_release (tenant_id, release_id),
  INDEX idx_marketing_content_release_item_key (tenant_id, item_type, item_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS marketing_content_audit_event (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  event_type VARCHAR(64) NOT NULL,
  target_type VARCHAR(32) NOT NULL,
  target_key VARCHAR(128) NOT NULL,
  actor VARCHAR(128) NOT NULL,
  old_value_json JSON NULL,
  new_value_json JSON NULL,
  note TEXT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_marketing_content_audit_target (tenant_id, target_type, target_key, created_at),
  INDEX idx_marketing_content_audit_event_type (tenant_id, event_type, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
