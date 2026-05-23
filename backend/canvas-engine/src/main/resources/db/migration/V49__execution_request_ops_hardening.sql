ALTER TABLE `canvas_execution_request`
  ADD COLUMN `run_token` VARCHAR(80) DEFAULT NULL AFTER `result_json`,
  ADD COLUMN `replay_count` INT NOT NULL DEFAULT 0 AFTER `run_token`,
  ADD COLUMN `last_replay_at` DATETIME DEFAULT NULL AFTER `replay_count`,
  ADD COLUMN `last_replay_by` VARCHAR(128) DEFAULT NULL AFTER `last_replay_at`,
  ADD COLUMN `last_replay_reason` VARCHAR(500) DEFAULT NULL AFTER `last_replay_by`;

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
