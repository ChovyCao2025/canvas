CREATE TABLE IF NOT EXISTS `growth_activity` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `activity_key` VARCHAR(128) NOT NULL,
  `activity_name` VARCHAR(255) NOT NULL,
  `activity_type` VARCHAR(64) NOT NULL,
  `status` VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
  `campaign_id` BIGINT DEFAULT NULL,
  `objective` VARCHAR(64) NULL,
  `owner_team` VARCHAR(128) NULL,
  `start_at` DATETIME NULL,
  `end_at` DATETIME NULL,
  `channel_scope` VARCHAR(64) NULL,
  `audience_refs_json` JSON NULL,
  `risk_policy_ref` VARCHAR(128) NULL,
  `experiment_ref` VARCHAR(128) NULL,
  `dashboard_ref` VARCHAR(128) NULL,
  `metadata_json` JSON NULL,
  `created_by` VARCHAR(128) NULL,
  `updated_by` VARCHAR(128) NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_growth_activity_key` (`tenant_id`, `activity_key`),
  KEY `idx_growth_activity_status` (`tenant_id`, `status`),
  KEY `idx_growth_activity_campaign` (`tenant_id`, `campaign_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Growth activity center master activity records';

CREATE TABLE IF NOT EXISTS `growth_activity_rule_set` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `activity_id` BIGINT NOT NULL,
  `rule_set_key` VARCHAR(128) NOT NULL,
  `rule_set_type` VARCHAR(64) NOT NULL,
  `status` VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
  `rule_json` JSON NULL,
  `created_by` VARCHAR(128) NULL,
  `updated_by` VARCHAR(128) NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_growth_activity_rule_set_key` (`tenant_id`, `activity_id`, `rule_set_key`),
  KEY `idx_growth_activity_rule_set_status` (`tenant_id`, `activity_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Growth activity rule sets';

CREATE TABLE IF NOT EXISTS `growth_reward_pool` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `activity_id` BIGINT NOT NULL,
  `pool_key` VARCHAR(128) NOT NULL,
  `reward_type` VARCHAR(64) NOT NULL,
  `grant_channel` VARCHAR(64) NOT NULL,
  `coupon_type_key` VARCHAR(128) NULL,
  `loyalty_reward_key` VARCHAR(128) NULL,
  `points_type` VARCHAR(64) NULL,
  `external_contract_key` VARCHAR(128) NULL,
  `inventory_mode` VARCHAR(32) NOT NULL DEFAULT 'LIMITED',
  `total_inventory` BIGINT NOT NULL DEFAULT 0,
  `reserved_inventory` BIGINT NOT NULL DEFAULT 0,
  `granted_inventory` BIGINT NOT NULL DEFAULT 0,
  `per_user_limit` INT NULL,
  `per_referral_limit` INT NULL,
  `budget_amount` DECIMAL(18,4) NOT NULL DEFAULT 0.0000,
  `reserved_amount` DECIMAL(18,4) NOT NULL DEFAULT 0.0000,
  `granted_amount` DECIMAL(18,4) NOT NULL DEFAULT 0.0000,
  `cost_currency` VARCHAR(16) NOT NULL DEFAULT 'CNY',
  `status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  `metadata_json` JSON NULL,
  `created_by` VARCHAR(128) NULL,
  `updated_by` VARCHAR(128) NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_growth_reward_pool_key` (`tenant_id`, `activity_id`, `pool_key`),
  KEY `idx_growth_reward_pool_status` (`tenant_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Growth reward pool budget and inventory';

CREATE TABLE IF NOT EXISTS `growth_budget_counter` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `activity_id` BIGINT NOT NULL,
  `counter_key` VARCHAR(128) NOT NULL,
  `counter_value` DECIMAL(18,4) NOT NULL DEFAULT 0.0000,
  `limit_value` DECIMAL(18,4) NOT NULL DEFAULT 0.0000,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_growth_budget_counter_key` (`tenant_id`, `activity_id`, `counter_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Growth activity budget counters';

CREATE TABLE IF NOT EXISTS `growth_activity_participant` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `activity_id` BIGINT NOT NULL,
  `user_id` VARCHAR(128) NOT NULL,
  `status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  `joined_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `attributes_json` JSON NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_growth_activity_participant_user` (`tenant_id`, `activity_id`, `user_id`),
  KEY `idx_growth_activity_participant_status` (`tenant_id`, `activity_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Growth activity participants';

CREATE TABLE IF NOT EXISTS `growth_reward_grant` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `activity_id` BIGINT NOT NULL,
  `pool_id` BIGINT NOT NULL,
  `participant_id` BIGINT DEFAULT NULL,
  `referral_relation_id` BIGINT DEFAULT NULL,
  `task_progress_id` BIGINT DEFAULT NULL,
  `grant_reason` VARCHAR(64) NOT NULL,
  `status` VARCHAR(32) NOT NULL DEFAULT 'RESERVED',
  `idempotency_key` VARCHAR(191) NOT NULL,
  `provider_request_json` JSON NULL,
  `provider_response_json` JSON NULL,
  `cost_amount` DECIMAL(18,4) NOT NULL DEFAULT 0.0000,
  `created_by` VARCHAR(128) NULL,
  `updated_by` VARCHAR(128) NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_growth_reward_grant_idempotency` (`tenant_id`, `idempotency_key`),
  KEY `idx_growth_reward_grant_activity` (`tenant_id`, `activity_id`, `status`),
  KEY `idx_growth_reward_grant_pool` (`tenant_id`, `pool_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Growth reward grants';

CREATE TABLE IF NOT EXISTS `growth_activity_event` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `activity_id` BIGINT NOT NULL,
  `participant_id` BIGINT DEFAULT NULL,
  `event_type` VARCHAR(64) NOT NULL,
  `event_key` VARCHAR(191) NOT NULL,
  `source_type` VARCHAR(64) NULL,
  `source_id` BIGINT DEFAULT NULL,
  `payload_json` JSON NULL,
  `created_by` VARCHAR(128) NULL,
  `occurred_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_growth_activity_event_key` (`tenant_id`, `event_key`),
  KEY `idx_growth_activity_event_activity` (`tenant_id`, `activity_id`, `event_type`, `occurred_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Growth activity events';

CREATE TABLE IF NOT EXISTS `growth_referral_code` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `activity_id` BIGINT NOT NULL,
  `participant_id` BIGINT NOT NULL,
  `code` VARCHAR(128) NOT NULL,
  `status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  `created_by` VARCHAR(128) NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_growth_referral_code` (`tenant_id`, `code`),
  KEY `idx_growth_referral_code_participant` (`tenant_id`, `activity_id`, `participant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Growth referral invitation codes';

CREATE TABLE IF NOT EXISTS `growth_referral_relation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `activity_id` BIGINT NOT NULL,
  `referral_code_id` BIGINT NOT NULL,
  `referrer_participant_id` BIGINT NOT NULL,
  `invitee_user_id` VARCHAR(128) NOT NULL,
  `status` VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  `risk_evidence_json` JSON NULL,
  `inviter_reward_grant_id` BIGINT DEFAULT NULL,
  `invitee_reward_grant_id` BIGINT DEFAULT NULL,
  `created_by` VARCHAR(128) NULL,
  `updated_by` VARCHAR(128) NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_growth_referral_relation_invitee` (`tenant_id`, `activity_id`, `invitee_user_id`),
  KEY `idx_growth_referral_relation_referrer` (`tenant_id`, `activity_id`, `referrer_participant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Growth referral relationships';

CREATE TABLE IF NOT EXISTS `growth_task_definition` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `activity_id` BIGINT NOT NULL,
  `task_key` VARCHAR(128) NOT NULL,
  `task_type` VARCHAR(64) NOT NULL,
  `completion_policy` VARCHAR(32) NOT NULL DEFAULT 'EVENT',
  `reset_policy` VARCHAR(32) NOT NULL DEFAULT 'ONCE',
  `reward_pool_id` BIGINT DEFAULT NULL,
  `target_value` DECIMAL(18,4) NOT NULL DEFAULT 1.0000,
  `status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  `rule_json` JSON NULL,
  `created_by` VARCHAR(128) NULL,
  `updated_by` VARCHAR(128) NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_growth_task_definition_key` (`tenant_id`, `activity_id`, `task_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Growth task definitions';

CREATE TABLE IF NOT EXISTS `growth_task_progress` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `activity_id` BIGINT NOT NULL,
  `participant_id` BIGINT NOT NULL,
  `task_id` BIGINT NOT NULL,
  `progress_value` DECIMAL(18,4) NOT NULL DEFAULT 0.0000,
  `target_value` DECIMAL(18,4) NOT NULL DEFAULT 1.0000,
  `status` VARCHAR(32) NOT NULL DEFAULT 'IN_PROGRESS',
  `last_event_key` VARCHAR(191) NULL,
  `evidence_json` JSON NULL,
  `reward_grant_id` BIGINT DEFAULT NULL,
  `updated_by` VARCHAR(128) NULL,
  `completed_at` DATETIME NULL,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_growth_task_progress` (`tenant_id`, `activity_id`, `participant_id`, `task_id`),
  KEY `idx_growth_task_progress_status` (`tenant_id`, `activity_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Growth task participant progress';
