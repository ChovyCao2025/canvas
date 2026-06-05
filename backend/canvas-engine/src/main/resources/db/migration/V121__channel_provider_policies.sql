CREATE TABLE IF NOT EXISTS channel_provider_limit (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  channel VARCHAR(32) NOT NULL,
  provider VARCHAR(64) NOT NULL,
  operation VARCHAR(64) NOT NULL DEFAULT 'SEND',
  per_second_limit INT NOT NULL DEFAULT 100,
  daily_limit BIGINT NULL,
  fail_closed TINYINT NOT NULL DEFAULT 1,
  updated_by VARCHAR(128) NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_channel_provider_limit (tenant_id, channel, provider, operation)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS channel_fallback_policy (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  channel VARCHAR(32) NOT NULL,
  provider VARCHAR(64) NOT NULL,
  fallback_channel VARCHAR(32) NOT NULL,
  fallback_provider VARCHAR(64) NOT NULL,
  enabled TINYINT NOT NULL DEFAULT 1,
  reason VARCHAR(128) NOT NULL DEFAULT 'PROVIDER_UNAVAILABLE',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_channel_fallback_policy (tenant_id, channel, provider)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS channel_fallback_decision (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  execution_id VARCHAR(128) NULL,
  node_id VARCHAR(128) NULL,
  original_channel VARCHAR(32) NOT NULL,
  original_provider VARCHAR(64) NOT NULL,
  final_channel VARCHAR(32) NULL,
  final_provider VARCHAR(64) NULL,
  decision_reason VARCHAR(128) NOT NULL,
  attempt_chain_json JSON NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_channel_fallback_decision_time (tenant_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS channel_dedupe_record (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  dedupe_group VARCHAR(128) NOT NULL,
  content_hash VARCHAR(128) NOT NULL,
  channel VARCHAR(32) NOT NULL,
  user_id VARCHAR(128) NOT NULL,
  expires_at DATETIME NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_channel_dedupe (tenant_id, dedupe_group, content_hash, channel, user_id),
  INDEX idx_channel_dedupe_expire (tenant_id, expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
