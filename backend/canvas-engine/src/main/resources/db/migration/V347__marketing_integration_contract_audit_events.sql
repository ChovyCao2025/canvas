CREATE TABLE IF NOT EXISTS `marketing_integration_contract_audit_event` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `contract_id` BIGINT NOT NULL,
  `contract_key` VARCHAR(128) NOT NULL,
  `revision` INT NOT NULL,
  `event_type` VARCHAR(32) NOT NULL,
  `previous_status` VARCHAR(32) NULL,
  `new_status` VARCHAR(32) NULL,
  `snapshot_json` JSON NOT NULL,
  `changed_fields_json` JSON NULL,
  `changed_by` VARCHAR(128) NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_marketing_integration_contract_revision` (`tenant_id`, `contract_id`, `revision`),
  KEY `idx_marketing_integration_audit_contract` (`tenant_id`, `contract_id`, `revision`),
  KEY `idx_marketing_integration_audit_type` (`tenant_id`, `event_type`, `created_at`),
  CONSTRAINT `fk_marketing_integration_audit_contract`
    FOREIGN KEY (`contract_id`) REFERENCES `marketing_integration_contract` (`id`)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Audit events for marketing integration contract registry changes';
