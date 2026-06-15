CREATE TABLE `customer_tag` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`    VARCHAR(128) NOT NULL,
  `tag`        VARCHAR(128) NOT NULL,
  `source`     VARCHAR(128) NULL,
  `expires_at` DATETIME     NULL,
  `created_at` DATETIME     NOT NULL,
  `updated_at` DATETIME     NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_customer_tag` (`user_id`, `tag`),
  INDEX `idx_customer_tag_expire` (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='营销旅程用户标签';

CREATE TABLE `customer_points_ledger` (
  `id`              BIGINT        NOT NULL AUTO_INCREMENT,
  `user_id`         VARCHAR(128)  NOT NULL,
  `operation`       VARCHAR(32)   NOT NULL,
  `points`          INT           NOT NULL,
  `points_type`     VARCHAR(64)   NULL,
  `reason`          VARCHAR(255)  NULL,
  `idempotency_key` VARCHAR(255)  NOT NULL,
  `expires_at`      DATETIME      NULL,
  `created_at`      DATETIME      NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_points_idempotency` (`idempotency_key`),
  INDEX `idx_points_user` (`user_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='营销积分流水默认表';

CREATE TABLE `customer_task_record` (
  `id`          BIGINT        NOT NULL AUTO_INCREMENT,
  `user_id`     VARCHAR(128)  NOT NULL,
  `task_type`   VARCHAR(64)   NOT NULL,
  `title`       VARCHAR(255)  NOT NULL,
  `description` TEXT          NULL,
  `priority`    VARCHAR(32)   NULL,
  `assignee`    VARCHAR(128)  NULL,
  `due_at`      DATETIME      NULL,
  `status`      VARCHAR(32)   NOT NULL,
  `created_at`  DATETIME      NOT NULL,
  `updated_at`  DATETIME      NOT NULL,
  PRIMARY KEY (`id`),
  INDEX `idx_customer_task_user` (`user_id`, `status`, `due_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='营销旅程人工任务默认表';
