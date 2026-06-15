CREATE TABLE `test_user_set` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `tenant_id`   BIGINT       NULL,
  `name`        VARCHAR(128) NOT NULL,
  `description` VARCHAR(500) NULL,
  `created_by`  VARCHAR(64)  NULL,
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_test_user_set_tenant` (`tenant_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='测试用户集合';

CREATE TABLE `test_user` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `tenant_id`     BIGINT       NULL,
  `set_id`        BIGINT       NOT NULL,
  `user_id`       VARCHAR(128) NOT NULL,
  `display_name`  VARCHAR(128) NULL,
  `profile_json`  JSON         NULL,
  `input_params`  JSON         NULL,
  `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_test_user_set_user` (`tenant_id`, `set_id`, `user_id`),
  KEY `idx_test_user_tenant_set` (`tenant_id`, `set_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='测试用户';

CREATE TABLE `execution_rerun_audit` (
  `id`                    BIGINT       NOT NULL AUTO_INCREMENT,
  `tenant_id`             BIGINT       NULL,
  `canvas_id`             BIGINT       NOT NULL,
  `user_id`               VARCHAR(128) NOT NULL,
  `test_user_id`          BIGINT       NULL,
  `original_execution_id` VARCHAR(64)  NULL,
  `mode`                  VARCHAR(32)  NOT NULL,
  `reason`                VARCHAR(500) NOT NULL,
  `operator`              VARCHAR(64)  NULL,
  `status`                VARCHAR(32)  NOT NULL,
  `input_params`          JSON         NULL,
  `created_at`            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_rerun_audit_canvas_created` (`tenant_id`, `canvas_id`, `created_at`),
  KEY `idx_rerun_audit_original` (`original_execution_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='单用户重跑审计';
