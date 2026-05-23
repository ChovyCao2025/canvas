CREATE TABLE `canvas_wait_subscription` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT,
  `execution_id`   VARCHAR(64)  NOT NULL,
  `canvas_id`      BIGINT       NOT NULL,
  `version_id`     BIGINT       NOT NULL,
  `user_id`        VARCHAR(128) NOT NULL,
  `node_id`        VARCHAR(64)  NOT NULL,
  `wait_type`      VARCHAR(32)  NOT NULL,
  `event_code`     VARCHAR(128) NULL,
  `event_filters`  TEXT         NULL,
  `resume_payload` TEXT         NULL,
  `expires_at`     DATETIME     NULL,
  `status`         VARCHAR(32)  NOT NULL,
  `created_at`     DATETIME     NOT NULL,
  `updated_at`     DATETIME     NOT NULL,
  PRIMARY KEY (`id`),
  INDEX `idx_event_wait` (`event_code`, `user_id`, `status`),
  INDEX `idx_expire` (`status`, `expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='等待订阅，用于 WAIT/GOAL_CHECK 挂起、唤醒和超时恢复';
