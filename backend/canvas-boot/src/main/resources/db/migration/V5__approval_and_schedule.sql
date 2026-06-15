-- V5 人工审批表 + 画布调度表
CREATE TABLE `canvas_manual_approval` (
  `id`            VARCHAR(64)  NOT NULL COMMENT 'approvalId = executionId:nodeId',
  `execution_id`  VARCHAR(64)  NOT NULL,
  `canvas_id`     BIGINT       NOT NULL,
  `node_id`       VARCHAR(64)  NOT NULL,
  `user_id`       VARCHAR(64)  NOT NULL COMMENT '待审批用户',
  `approvers`     TEXT         NOT NULL COMMENT '审批人 JSON 数组',
  `on_timeout`    VARCHAR(16)  NOT NULL DEFAULT 'REJECT' COMMENT 'REJECT/APPROVE/KEEP_WAITING',
  `timeout_at`    DATETIME     NOT NULL,
  `status`        VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED/TIMEOUT',
  `result_by`     VARCHAR(64)  NULL,
  `result_at`     DATETIME     NULL,
  `created_at`    DATETIME     NOT NULL,
  PRIMARY KEY (`id`),
  INDEX `idx_execution_node` (`execution_id`, `node_id`),
  INDEX `idx_timeout` (`status`, `timeout_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='人工审批挂起记录';

CREATE TABLE `canvas_schedule` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `canvas_id`   BIGINT       NOT NULL,
  `node_id`     VARCHAR(64)  NOT NULL,
  `cron`        VARCHAR(64)  NULL     COMMENT 'CRON 表达式',
  `trigger_time`DATETIME     NULL     COMMENT 'ONCE 模式触发时间',
  `schedule_type`VARCHAR(8)  NOT NULL COMMENT 'CRON/ONCE',
  `enabled`     TINYINT      NOT NULL DEFAULT 1,
  `created_at`  DATETIME     NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_canvas_node` (`canvas_id`, `node_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='画布调度任务';
