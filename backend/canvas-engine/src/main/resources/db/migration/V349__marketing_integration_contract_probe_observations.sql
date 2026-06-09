CREATE TABLE IF NOT EXISTS `marketing_integration_contract_probe_observation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `contract_id` BIGINT NOT NULL,
  `probe_run_id` BIGINT NULL,
  `contract_key` VARCHAR(128) NOT NULL,
  `provider_family` VARCHAR(64) NOT NULL,
  `probe_key` VARCHAR(128) NOT NULL,
  `environment` VARCHAR(32) NOT NULL DEFAULT 'PRODUCTION',
  `status` VARCHAR(32) NOT NULL,
  `http_status_code` INT NULL,
  `latency_ms` BIGINT NULL,
  `error_type` VARCHAR(255) NULL,
  `problem_type_uri` VARCHAR(512) NULL,
  `problem_title` VARCHAR(255) NULL,
  `problem_detail` VARCHAR(1000) NULL,
  `error_message` VARCHAR(1000) NULL,
  `summary` VARCHAR(512) NULL,
  `observed_at` DATETIME NOT NULL,
  `evidence_json` JSON NULL,
  `created_by` VARCHAR(128) NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_marketing_integration_probe_observation_window`
    (`tenant_id`, `contract_id`, `probe_key`, `environment`, `observed_at`),
  KEY `idx_marketing_integration_probe_observation_status`
    (`tenant_id`, `environment`, `status`, `observed_at`),
  KEY `idx_marketing_integration_probe_observation_provider`
    (`tenant_id`, `provider_family`, `status`, `observed_at`),
  CONSTRAINT `fk_marketing_integration_probe_observation_contract`
    FOREIGN KEY (`contract_id`) REFERENCES `marketing_integration_contract` (`id`)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Append-only runtime observations for marketing integration contract probe SLOs';
