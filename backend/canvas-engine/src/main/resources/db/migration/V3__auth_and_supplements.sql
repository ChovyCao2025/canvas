-- ============================================================
-- V3 认证表 + canvas 补充字段 + 补充业务表
-- ============================================================

-- 用户表（JWT 登录）
CREATE TABLE `sys_user` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT,
  `username`     VARCHAR(64)  NOT NULL COMMENT '登录用户名（唯一）',
  `password`     VARCHAR(128) NOT NULL COMMENT 'BCrypt 加密密码',
  `display_name` VARCHAR(64)  NOT NULL COMMENT '展示名称',
  `role`         VARCHAR(16)  NOT NULL COMMENT 'ADMIN / OPERATOR',
  `enabled`      TINYINT      NOT NULL DEFAULT 1 COMMENT '0=禁用',
  `created_at`   DATETIME     NOT NULL,
  `updated_at`   DATETIME     NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- 初始化管理员账号（密码: Admin@123，BCrypt 已加密）
INSERT INTO `sys_user` (`username`, `password`, `display_name`, `role`, `enabled`, `created_at`, `updated_at`)
VALUES ('admin', '$2a$10$49Y8JwhyOUHL0Yv./Y/mK.ldJWZD0owkuSxZ6.QpP/trR0MZRNyNq',
        '系统管理员', 'ADMIN', 1, NOW(), NOW());

-- canvas 表追加字段（活动生命周期 + 灰度 + 乐观锁）
ALTER TABLE `canvas`
  ADD COLUMN `valid_start`           DATETIME  NULL COMMENT '活动开始时间，null=立即生效',
  ADD COLUMN `valid_end`             DATETIME  NULL COMMENT '活动结束时间，null=永不过期',
  ADD COLUMN `per_user_total_limit`  INT       NULL COMMENT '单用户总触发上限，null=不限',
  ADD COLUMN `per_user_daily_limit`  INT       NULL COMMENT '单用户每日触发上限，null=不限',
  ADD COLUMN `cooldown_seconds`      INT       NULL COMMENT '同用户两次触发最短间隔(秒)，null=不限',
  ADD COLUMN `max_total_executions`  INT       NULL COMMENT '全局总触发量上限，null=不限',
  ADD COLUMN `canary_version_id`     BIGINT    NULL COMMENT '灰度版本ID',
  ADD COLUMN `canary_percent`        INT       NULL COMMENT '灰度流量比例 0~100',
  ADD COLUMN `previous_version_id`   BIGINT    NULL COMMENT '上一个稳定版本（快速回滚用）',
  ADD COLUMN `edit_version`          INT       NOT NULL DEFAULT 0 COMMENT '乐观锁版本号';

-- 操作审计日志
CREATE TABLE `canvas_audit_log` (
  `id`           BIGINT      NOT NULL AUTO_INCREMENT,
  `canvas_id`    BIGINT      NOT NULL,
  `operator`     VARCHAR(64) NOT NULL COMMENT '操作人 username',
  `operator_role`VARCHAR(16) NOT NULL,
  `action`       VARCHAR(32) NOT NULL COMMENT 'CREATED/PUBLISHED/OFFLINE/KILLED/CLONED/...',
  `from_version` BIGINT      NULL     COMMENT '变更前版本',
  `to_version`   BIGINT      NULL     COMMENT '变更后版本',
  `detail`       TEXT        NULL     COMMENT '变更详情 JSON',
  `ip`           VARCHAR(64) NULL     COMMENT '操作来源 IP',
  `created_at`   DATETIME    NOT NULL,
  PRIMARY KEY (`id`),
  INDEX `idx_canvas_created` (`canvas_id`, `created_at` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='画布操作审计日志';

-- 用户级执行用量表
CREATE TABLE `canvas_user_quota` (
  `canvas_id`       BIGINT      NOT NULL,
  `user_id`         VARCHAR(64) NOT NULL,
  `trigger_date`    DATE        NOT NULL COMMENT '日期（用于每日限制）',
  `daily_count`     INT         NOT NULL DEFAULT 0 COMMENT '当日触发次数',
  `total_count`     INT         NOT NULL DEFAULT 0 COMMENT '历史总触发次数',
  `last_trigger_at` DATETIME    NULL,
  PRIMARY KEY (`canvas_id`, `user_id`, `trigger_date`),
  INDEX `idx_canvas_user` (`canvas_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户级执行用量';

-- 执行统计（按画布/日汇总）
CREATE TABLE `canvas_execution_stats` (
  `id`              BIGINT  NOT NULL AUTO_INCREMENT,
  `canvas_id`       BIGINT  NOT NULL,
  `stat_date`       DATE    NOT NULL,
  `total_count`     INT     NOT NULL DEFAULT 0 COMMENT '总触发次数',
  `success_count`   INT     NOT NULL DEFAULT 0,
  `fail_count`      INT     NOT NULL DEFAULT 0,
  `paused_count`    INT     NOT NULL DEFAULT 0 COMMENT '当前挂起中',
  `timeout_count`   INT     NOT NULL DEFAULT 0,
  `unique_users`    INT     NOT NULL DEFAULT 0 COMMENT '触达唯一用户数',
  `avg_duration_ms` BIGINT  NULL,
  `p99_duration_ms` BIGINT  NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_canvas_date` (`canvas_id`, `stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='执行统计（按画布/日）';

-- 节点漏斗统计
CREATE TABLE `canvas_node_funnel_stats` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT,
  `canvas_id`      BIGINT       NOT NULL,
  `node_id`        VARCHAR(64)  NOT NULL,
  `node_type`      VARCHAR(32)  NULL,
  `stat_date`      DATE         NOT NULL,
  `total_entered`  INT          NOT NULL DEFAULT 0,
  `total_success`  INT          NOT NULL DEFAULT 0,
  `total_failed`   INT          NOT NULL DEFAULT 0,
  `total_skipped`  INT          NOT NULL DEFAULT 0,
  `avg_duration_ms`INT          NULL,
  PRIMARY KEY (`id`),
  INDEX `idx_canvas_node_date` (`canvas_id`, `node_id`, `stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='节点漏斗统计';

-- 画布模板
CREATE TABLE `canvas_template` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `name`        VARCHAR(100) NOT NULL,
  `description` VARCHAR(500) NULL,
  `category`    VARCHAR(50)  NULL COMMENT '新客获取/老客召回/节日促销/其他',
  `graph_json`  MEDIUMTEXT   NOT NULL,
  `thumbnail`   VARCHAR(500) NULL COMMENT '预览图 URL',
  `is_official` TINYINT      NOT NULL DEFAULT 0 COMMENT '1=平台官方模板',
  `use_count`   INT          NOT NULL DEFAULT 0,
  `created_by`  VARCHAR(64)  NULL,
  `created_at`  DATETIME     NULL,
  PRIMARY KEY (`id`),
  INDEX `idx_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='画布模板';
