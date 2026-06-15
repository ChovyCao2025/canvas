ALTER TABLE `canvas`
  ADD COLUMN `control_group_percent` INT NOT NULL DEFAULT 0 AFTER `cooldown_seconds`,
  ADD COLUMN `control_group_salt` VARCHAR(64) NULL AFTER `control_group_percent`,
  ADD COLUMN `conversion_event_code` VARCHAR(128) NULL AFTER `control_group_salt`,
  ADD COLUMN `attribution_window_days` INT NOT NULL DEFAULT 7 AFTER `conversion_event_code`;

CREATE TABLE `canvas_control_group_holdout` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `canvas_id` BIGINT NOT NULL,
  `user_id` VARCHAR(128) NOT NULL,
  `event_id` VARCHAR(128) NULL,
  `reason` VARCHAR(64) NOT NULL,
  `created_at` DATETIME NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_canvas_control_holdout` (`canvas_id`, `user_id`, `event_id`),
  KEY `idx_canvas_control_canvas_created` (`canvas_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Control group holdout audit records';

CREATE TABLE `canvas_conversion_attribution` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `canvas_id` BIGINT NOT NULL,
  `user_id` VARCHAR(128) NOT NULL,
  `event_log_id` BIGINT NOT NULL,
  `send_record_id` BIGINT NULL,
  `conversion_event_code` VARCHAR(128) NOT NULL,
  `conversion_amount` DECIMAL(18,2) NULL,
  `attribution_model` VARCHAR(32) NOT NULL DEFAULT 'LAST_TOUCH',
  `attributed_at` DATETIME NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_canvas_attr_event` (`canvas_id`, `event_log_id`),
  KEY `idx_canvas_attr_canvas_time` (`canvas_id`, `attributed_at`),
  KEY `idx_canvas_attr_send_record` (`send_record_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Last-touch conversion attribution';
