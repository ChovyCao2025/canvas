-- V4 DLQ 表
CREATE TABLE `canvas_execution_dlq` (
  `id`               BIGINT       NOT NULL AUTO_INCREMENT,
  `execution_id`     VARCHAR(64)  NOT NULL,
  `canvas_id`        BIGINT       NOT NULL,
  `user_id`          VARCHAR(64),
  `failed_node_id`   VARCHAR(64),
  `failed_node_type` VARCHAR(32),
  `error_msg`        VARCHAR(500),
  `retry_count`      INT          NOT NULL DEFAULT 0,
  `trigger_payload`  TEXT,
  `failed_at`        DATETIME     NOT NULL,
  PRIMARY KEY (`id`),
  INDEX `idx_canvas_id` (`canvas_id`),
  INDEX `idx_failed_at` (`failed_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='执行死信队列';
