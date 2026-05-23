-- Compatibility migration for runtime support classes that landed after V48.
-- Keep this migration idempotent because some local worktrees may still carry
-- earlier untracked V44/V46/V47/V49 drafts with the same schema changes.

SET @column_exists := (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'api_definition'
    AND COLUMN_NAME = 'include_context_payload'
);
SET @ddl := IF(
  @column_exists = 0,
  'ALTER TABLE `api_definition` ADD COLUMN `include_context_payload` TINYINT NOT NULL DEFAULT 0 COMMENT ''是否携带旅程环境信息，1=携带，0=不携带'' AFTER `response_schema`',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'api_definition'
    AND COLUMN_NAME = 'receipt_enabled'
);
SET @ddl := IF(
  @column_exists = 0,
  'ALTER TABLE `api_definition` ADD COLUMN `receipt_enabled` TINYINT NOT NULL DEFAULT 0 COMMENT ''是否开启回执等待'' AFTER `include_context_payload`',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'api_definition'
    AND COLUMN_NAME = 'receipt_expire_minutes'
);
SET @ddl := IF(
  @column_exists = 0,
  'ALTER TABLE `api_definition` ADD COLUMN `receipt_expire_minutes` INT NOT NULL DEFAULT 1440 COMMENT ''回执等待过期时间（分钟）'' AFTER `receipt_enabled`',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'api_definition'
    AND COLUMN_NAME = 'receipt_statuses'
);
SET @ddl := IF(
  @column_exists = 0,
  'ALTER TABLE `api_definition` ADD COLUMN `receipt_statuses` JSON NULL COMMENT ''视为回执完成的状态列表'' AFTER `receipt_expire_minutes`',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS `canvas_execution_request` (
  `id` VARCHAR(80) NOT NULL,
  `canvas_id` BIGINT NOT NULL,
  `user_id` VARCHAR(128) NOT NULL,
  `trigger_type` VARCHAR(64) NOT NULL,
  `trigger_node_type` VARCHAR(64) NOT NULL,
  `match_key` VARCHAR(255) DEFAULT NULL,
  `payload_json` JSON DEFAULT NULL,
  `source_msg_id` VARCHAR(255) DEFAULT NULL,
  `status` VARCHAR(32) NOT NULL,
  `attempt_count` INT NOT NULL DEFAULT 0,
  `next_retry_at` DATETIME DEFAULT NULL,
  `last_error` VARCHAR(500) DEFAULT NULL,
  `result_json` JSON DEFAULT NULL,
  `run_token` VARCHAR(80) DEFAULT NULL,
  `replay_count` INT NOT NULL DEFAULT 0,
  `last_replay_at` DATETIME DEFAULT NULL,
  `last_replay_by` VARCHAR(128) DEFAULT NULL,
  `last_replay_reason` VARCHAR(500) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_execution_request_due` (`status`, `next_retry_at`, `updated_at`),
  KEY `idx_execution_request_canvas` (`canvas_id`, `status`, `updated_at`),
  KEY `idx_execution_request_msg` (`source_msg_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @column_exists := (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'canvas_execution_request'
    AND COLUMN_NAME = 'run_token'
);
SET @ddl := IF(
  @column_exists = 0,
  'ALTER TABLE `canvas_execution_request` ADD COLUMN `run_token` VARCHAR(80) DEFAULT NULL AFTER `result_json`',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'canvas_execution_request'
    AND COLUMN_NAME = 'replay_count'
);
SET @ddl := IF(
  @column_exists = 0,
  'ALTER TABLE `canvas_execution_request` ADD COLUMN `replay_count` INT NOT NULL DEFAULT 0 AFTER `run_token`',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'canvas_execution_request'
    AND COLUMN_NAME = 'last_replay_at'
);
SET @ddl := IF(
  @column_exists = 0,
  'ALTER TABLE `canvas_execution_request` ADD COLUMN `last_replay_at` DATETIME DEFAULT NULL AFTER `replay_count`',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'canvas_execution_request'
    AND COLUMN_NAME = 'last_replay_by'
);
SET @ddl := IF(
  @column_exists = 0,
  'ALTER TABLE `canvas_execution_request` ADD COLUMN `last_replay_by` VARCHAR(128) DEFAULT NULL AFTER `last_replay_at`',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'canvas_execution_request'
    AND COLUMN_NAME = 'last_replay_reason'
);
SET @ddl := IF(
  @column_exists = 0,
  'ALTER TABLE `canvas_execution_request` ADD COLUMN `last_replay_reason` VARCHAR(500) DEFAULT NULL AFTER `last_replay_by`',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS `canvas_mq_trigger_rejected` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `msg_id` VARCHAR(255) DEFAULT NULL,
  `tag` VARCHAR(128) DEFAULT NULL,
  `reason` VARCHAR(64) NOT NULL,
  `error_msg` VARCHAR(500) DEFAULT NULL,
  `body` TEXT,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_mq_rejected_msg` (`msg_id`),
  KEY `idx_mq_rejected_tag_reason` (`tag`, `reason`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MQ触发消费端拒绝消息';
