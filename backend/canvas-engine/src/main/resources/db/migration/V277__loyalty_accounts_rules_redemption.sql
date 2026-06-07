CREATE TABLE IF NOT EXISTS loyalty_member_account (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  user_id VARCHAR(128) NOT NULL,
  member_no VARCHAR(128) NULL,
  tier_code VARCHAR(32) NOT NULL DEFAULT 'BASIC',
  points_balance INT NOT NULL DEFAULT 0,
  lifetime_points INT NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  enrolled_at DATETIME NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_loyalty_account_user (tenant_id, user_id),
  KEY idx_loyalty_account_tier (tenant_id, tier_code, status),
  KEY idx_loyalty_account_member_no (tenant_id, member_no)
);

CREATE TABLE IF NOT EXISTS loyalty_rule (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  rule_key VARCHAR(128) NOT NULL,
  rule_type VARCHAR(32) NOT NULL,
  points_delta INT NULL,
  reward_key VARCHAR(128) NULL,
  points_cost INT NULL,
  benefit_key VARCHAR(128) NULL,
  benefit_name VARCHAR(255) NULL,
  min_tier_code VARCHAR(32) NULL,
  config_json JSON NULL,
  enabled TINYINT NOT NULL DEFAULT 1,
  starts_at DATETIME NULL,
  ends_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_loyalty_rule_key (tenant_id, rule_key),
  KEY idx_loyalty_rule_type_enabled (tenant_id, rule_type, enabled),
  KEY idx_loyalty_rule_window (tenant_id, starts_at, ends_at)
);

CREATE TABLE IF NOT EXISTS loyalty_transaction_journal (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  user_id VARCHAR(128) NOT NULL,
  account_id BIGINT NOT NULL,
  transaction_key VARCHAR(160) NOT NULL,
  transaction_type VARCHAR(32) NOT NULL,
  points_delta INT NOT NULL,
  points_type VARCHAR(64) NULL,
  balance_after INT NOT NULL,
  source_type VARCHAR(64) NULL,
  source_id VARCHAR(128) NULL,
  reason VARCHAR(255) NULL,
  occurred_at DATETIME NOT NULL,
  expires_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_loyalty_journal_transaction (tenant_id, transaction_key),
  KEY idx_loyalty_journal_user_time (tenant_id, user_id, occurred_at),
  KEY idx_loyalty_journal_account_time (account_id, occurred_at)
);

CREATE TABLE IF NOT EXISTS loyalty_redemption (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  user_id VARCHAR(128) NOT NULL,
  account_id BIGINT NOT NULL,
  redemption_key VARCHAR(160) NOT NULL,
  reward_key VARCHAR(128) NOT NULL,
  points_cost INT NOT NULL,
  status VARCHAR(32) NOT NULL,
  failure_reason VARCHAR(512) NULL,
  redeemed_at DATETIME NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_loyalty_redemption_key (tenant_id, redemption_key),
  KEY idx_loyalty_redemption_user_time (tenant_id, user_id, redeemed_at),
  KEY idx_loyalty_redemption_reward_status (tenant_id, reward_key, status)
);
