CREATE TABLE IF NOT EXISTS `marketing_campaign_master` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `campaign_key` VARCHAR(128) NOT NULL,
  `campaign_name` VARCHAR(255) NOT NULL,
  `objective` VARCHAR(64) NOT NULL DEFAULT 'UNSPECIFIED',
  `status` VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
  `primary_channel` VARCHAR(64) NULL,
  `owner_team` VARCHAR(128) NULL,
  `start_at` DATETIME NULL,
  `end_at` DATETIME NULL,
  `budget_amount` DECIMAL(18,4) NOT NULL DEFAULT 0.0000,
  `currency` VARCHAR(16) NOT NULL DEFAULT 'CNY',
  `brief_json` JSON NULL,
  `created_by` VARCHAR(128) NULL,
  `updated_by` VARCHAR(128) NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_marketing_campaign_master_key` (`tenant_id`, `campaign_key`),
  KEY `idx_marketing_campaign_master_status` (`tenant_id`, `status`, `start_at`, `end_at`),
  KEY `idx_marketing_campaign_master_owner` (`tenant_id`, `owner_team`, `updated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Unified marketing campaign master ledger across journeys, content, paid media, creator, DSP, and BI';

CREATE TABLE IF NOT EXISTS `marketing_campaign_link` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `campaign_id` BIGINT NOT NULL,
  `resource_type` VARCHAR(64) NOT NULL,
  `resource_id` BIGINT NULL,
  `resource_key` VARCHAR(191) NOT NULL,
  `resource_name` VARCHAR(255) NULL,
  `resource_route` VARCHAR(512) NULL,
  `dependency_role` VARCHAR(64) NOT NULL DEFAULT 'SUPPORTING',
  `link_status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  `required_for_launch` TINYINT NOT NULL DEFAULT 0,
  `metadata_json` JSON NULL,
  `created_by` VARCHAR(128) NULL,
  `updated_by` VARCHAR(128) NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_marketing_campaign_link_resource` (`tenant_id`, `campaign_id`, `resource_type`, `resource_key`),
  KEY `idx_marketing_campaign_link_campaign` (`tenant_id`, `campaign_id`, `link_status`),
  KEY `idx_marketing_campaign_link_resource` (`tenant_id`, `resource_type`, `resource_key`),
  CONSTRAINT `fk_marketing_campaign_link_campaign`
    FOREIGN KEY (`campaign_id`) REFERENCES `marketing_campaign_master` (`id`)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Resource links attached to a unified marketing campaign master record';
