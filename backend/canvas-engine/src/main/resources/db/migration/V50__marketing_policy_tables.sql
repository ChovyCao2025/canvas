CREATE TABLE `customer_profile` (
  `id`              BIGINT        NOT NULL AUTO_INCREMENT,
  `user_id`         VARCHAR(128)  NOT NULL,
  `timezone`        VARCHAR(64)   NULL,
  `region`          VARCHAR(64)   NULL,
  `lifecycle_stage` VARCHAR(64)   NULL,
  `attributes`      TEXT          NULL,
  `created_at`      DATETIME      NOT NULL,
  `updated_at`      DATETIME      NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_customer_profile_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='营销用户基础画像默认表';

CREATE TABLE `customer_channel` (
  `id`         BIGINT        NOT NULL AUTO_INCREMENT,
  `user_id`    VARCHAR(128)  NOT NULL,
  `channel`    VARCHAR(32)   NOT NULL,
  `address`    VARCHAR(255)  NULL,
  `enabled`    TINYINT       NOT NULL DEFAULT 1,
  `verified`   TINYINT       NOT NULL DEFAULT 0,
  `metadata`   TEXT          NULL,
  `created_at` DATETIME      NOT NULL,
  `updated_at` DATETIME      NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_customer_channel` (`user_id`, `channel`),
  INDEX `idx_customer_channel_enabled` (`channel`, `enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='营销渠道可达性默认表';

CREATE TABLE `marketing_consent` (
  `id`             BIGINT        NOT NULL AUTO_INCREMENT,
  `user_id`        VARCHAR(128)  NOT NULL,
  `channel`        VARCHAR(32)   NOT NULL,
  `consent_status` VARCHAR(32)   NOT NULL,
  `source`         VARCHAR(64)   NULL,
  `created_at`     DATETIME      NOT NULL,
  `updated_at`     DATETIME      NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_marketing_consent` (`user_id`, `channel`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='营销授权与退订状态';

CREATE TABLE `marketing_suppression` (
  `id`         BIGINT        NOT NULL AUTO_INCREMENT,
  `user_id`    VARCHAR(128)  NOT NULL,
  `channel`    VARCHAR(32)   NULL,
  `reason`     VARCHAR(128)  NOT NULL,
  `active`     TINYINT       NOT NULL DEFAULT 1,
  `expires_at` DATETIME      NULL,
  `created_at` DATETIME      NOT NULL,
  `updated_at` DATETIME      NOT NULL,
  PRIMARY KEY (`id`),
  INDEX `idx_suppression_user_channel` (`user_id`, `channel`, `active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='营销抑制名单';

CREATE TABLE `marketing_frequency_counter` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT,
  `counter_key`  VARCHAR(255) NOT NULL,
  `count_value`  INT          NOT NULL,
  `window_start` DATETIME     NOT NULL,
  `window_end`   DATETIME     NOT NULL,
  `updated_at`   DATETIME     NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_frequency_counter_key` (`counter_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='营销频控计数快照；运行时以 Redis 固定窗口为准';
