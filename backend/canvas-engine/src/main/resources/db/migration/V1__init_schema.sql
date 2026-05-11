-- ============================================================
-- V1 初始化建表
-- ============================================================

-- 画布主表
CREATE TABLE `canvas` (
  `id`                   BIGINT        NOT NULL AUTO_INCREMENT,
  `name`                 VARCHAR(100)  NOT NULL COMMENT '画布名称',
  `description`          VARCHAR(500)  NULL     COMMENT '描述',
  `status`               TINYINT       NOT NULL DEFAULT 0 COMMENT '0草稿 1已发布 2已下线',
  `published_version_id` BIGINT        NULL     COMMENT '当前生效版本ID',
  `created_by`           VARCHAR(64)   NULL,
  `created_at`           DATETIME      NULL,
  `updated_at`           DATETIME      NULL,
  PRIMARY KEY (`id`),
  INDEX idx_status_created (`status`, `created_at`),
  INDEX idx_created_by (`created_by`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='画布主表';

-- 版本快照（每次发布生成一条）
CREATE TABLE `canvas_version` (
  `id`          BIGINT        NOT NULL AUTO_INCREMENT,
  `canvas_id`   BIGINT        NOT NULL,
  `version`     INT           NOT NULL COMMENT '版本号，从1递增',
  `graph_json`  MEDIUMTEXT    NOT NULL COMMENT '完整画布JSON',
  `status`      TINYINT       NOT NULL COMMENT '0草稿 1已发布',
  `created_by`  VARCHAR(64)   NULL,
  `created_at`  DATETIME      NULL,
  PRIMARY KEY (`id`),
  INDEX idx_canvas_version (`canvas_id`, `version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='版本快照';

-- 执行记录
CREATE TABLE `canvas_execution` (
  `id`              VARCHAR(64)   NOT NULL COMMENT 'UUID',
  `canvas_id`       BIGINT        NOT NULL,
  `version_id`      BIGINT        NOT NULL,
  `user_id`         VARCHAR(64)   NULL,
  `trigger_type`    VARCHAR(32)   NULL COMMENT 'MQ/DIRECT_CALL/BEHAVIOR',
  `status`          TINYINT       NULL COMMENT '0执行中 1暂停 2成功 3失败',
  `result`          TEXT          NULL COMMENT '执行结果JSON',
  `last_dedup_key`  VARCHAR(200)  NULL COMMENT '最近一次dedup key（Watchdog清理用）',
  `created_at`      DATETIME      NULL,
  `updated_at`      DATETIME      NULL,
  PRIMARY KEY (`id`),
  INDEX idx_canvas_user (`canvas_id`, `user_id`, `created_at`),
  INDEX idx_status_created (`status`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='执行记录';

-- 节点执行轨迹
CREATE TABLE `canvas_execution_trace` (
  `id`            BIGINT        NOT NULL AUTO_INCREMENT,
  `execution_id`  VARCHAR(64)   NOT NULL,
  `node_id`       VARCHAR(64)   NOT NULL,
  `node_type`     VARCHAR(32)   NULL,
  `node_name`     VARCHAR(100)  NULL,
  `status`        TINYINT       NULL COMMENT '0执行中 1成功 2失败 3跳过',
  `input_data`    TEXT          NULL,
  `output_data`   TEXT          NULL,
  `error_msg`     VARCHAR(500)  NULL,
  `started_at`    DATETIME      NULL,
  `finished_at`   DATETIME      NULL,
  PRIMARY KEY (`id`),
  INDEX idx_execution_started (`execution_id`, `started_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='节点执行轨迹';

-- 全局上下文字段注册表
CREATE TABLE `context_field` (
  `id`               BIGINT       NOT NULL AUTO_INCREMENT,
  `field_key`        VARCHAR(64)  NOT NULL COMMENT '字段标识，如 orderId',
  `field_name`       VARCHAR(64)  NOT NULL COMMENT '显示名称',
  `data_type`        VARCHAR(16)  NOT NULL COMMENT 'STRING/NUMBER/BOOLEAN/LIST',
  `source_node_type` VARCHAR(32)  NULL     COMMENT '由哪类节点产出',
  `description`      VARCHAR(200) NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY uk_field_key (`field_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='上下文字段注册表';

-- 节点类型注册表（插件化核心）
CREATE TABLE `node_type_registry` (
  `type_key`      VARCHAR(64)   NOT NULL COMMENT '节点类型标识',
  `type_name`     VARCHAR(64)   NOT NULL COMMENT '显示名称',
  `category`      VARCHAR(32)   NOT NULL COMMENT '行为策略/逻辑分支/人群圈选/权益发放/用户触达/其他',
  `handler_class` VARCHAR(200)  NOT NULL COMMENT 'Handler全限定类名',
  `config_schema` TEXT          NOT NULL COMMENT '前端表单Schema（JSON数组）',
  `output_schema` TEXT          NULL     COMMENT '节点产出的上下文字段定义',
  `is_trigger`    TINYINT       NOT NULL DEFAULT 0 COMMENT '1=触发器节点（无入边）',
  `is_terminal`   TINYINT       NOT NULL DEFAULT 0 COMMENT '1=终止节点（无出边）',
  `description`   VARCHAR(500)  NULL,
  `enabled`       TINYINT       NOT NULL DEFAULT 1,
  PRIMARY KEY (`type_key`),
  INDEX idx_category_enabled (`category`, `enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='节点类型注册表';
