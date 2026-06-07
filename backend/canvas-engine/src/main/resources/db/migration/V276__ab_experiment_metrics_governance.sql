CREATE TABLE IF NOT EXISTS `ab_experiment_layer` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `layer_key` VARCHAR(120) NOT NULL,
  `name` VARCHAR(200) NOT NULL,
  `description` VARCHAR(500) NULL,
  `traffic_pct` DECIMAL(7,4) NOT NULL DEFAULT 100.0000,
  `salt` VARCHAR(128) NOT NULL DEFAULT 'default',
  `status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_ab_experiment_layer_tenant_key` (`tenant_id`, `layer_key`),
  KEY `idx_ab_experiment_layer_status` (`tenant_id`, `status`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='A/B experiment mutually exclusive traffic layer';

CREATE TABLE IF NOT EXISTS `ab_experiment_allocation` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `experiment_id` BIGINT NOT NULL,
  `layer_id` BIGINT NOT NULL,
  `variant_key` VARCHAR(64) NOT NULL,
  `allocation_pct` DECIMAL(7,4) NOT NULL,
  `bucket_start` INT NOT NULL,
  `bucket_end` INT NOT NULL,
  `status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_ab_experiment_allocation_variant` (`experiment_id`, `variant_key`),
  KEY `idx_ab_experiment_allocation_layer_bucket` (`layer_id`, `status`, `bucket_start`, `bucket_end`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='A/B experiment variant allocation inside a layer';

CREATE TABLE IF NOT EXISTS `ab_experiment_metric` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `experiment_id` BIGINT NOT NULL,
  `metric_key` VARCHAR(128) NOT NULL,
  `display_name` VARCHAR(200) NOT NULL,
  `metric_role` VARCHAR(32) NOT NULL DEFAULT 'PRIMARY',
  `direction` VARCHAR(32) NOT NULL DEFAULT 'INCREASE',
  `minimum_detectable_effect` DECIMAL(12,8) NULL,
  `guardrail_max_regression` DECIMAL(12,8) NULL,
  `minimum_sample_size` INT NULL,
  `enabled` TINYINT NOT NULL DEFAULT 1,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_ab_experiment_metric` (`experiment_id`, `metric_key`),
  KEY `idx_ab_experiment_metric_role` (`experiment_id`, `metric_role`, `enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='A/B experiment primary and guardrail metric contract';

CREATE TABLE IF NOT EXISTS `ab_experiment_metric_snapshot` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `experiment_id` BIGINT NOT NULL,
  `variant_key` VARCHAR(64) NOT NULL,
  `metric_key` VARCHAR(128) NOT NULL,
  `sample_size` BIGINT NOT NULL DEFAULT 0,
  `conversions` BIGINT NULL,
  `metric_value` DECIMAL(18,8) NOT NULL DEFAULT 0.00000000,
  `variance` DECIMAL(18,8) NULL,
  `observed_at` DATETIME NOT NULL,
  `source` VARCHAR(64) NOT NULL DEFAULT 'MANUAL',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_ab_experiment_snapshot` (`experiment_id`, `variant_key`, `metric_key`, `observed_at`),
  KEY `idx_ab_experiment_snapshot_latest` (`experiment_id`, `metric_key`, `observed_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='A/B experiment metric observations by variant';

CREATE TABLE IF NOT EXISTS `ab_experiment_governance_decision` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `experiment_id` BIGINT NOT NULL,
  `status` VARCHAR(32) NOT NULL,
  `winner_variant_key` VARCHAR(64) NULL,
  `primary_metric_key` VARCHAR(128) NULL,
  `confidence` DECIMAL(12,8) NULL,
  `required_sample_size` BIGINT NULL,
  `reason` VARCHAR(2000) NOT NULL,
  `writeback_status` VARCHAR(32) NOT NULL DEFAULT 'NOT_READY',
  `evaluated_at` DATETIME NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_ab_experiment_decision_latest` (`experiment_id`, `evaluated_at`, `id`),
  KEY `idx_ab_experiment_decision_status` (`experiment_id`, `status`, `writeback_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='A/B experiment sequential governance decision audit';
