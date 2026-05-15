-- V18: 标签定义管理表（TAGGER_OFFLINE/REALTIME 节点标签来源）
CREATE TABLE `tag_definition` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `name`        VARCHAR(64)  NOT NULL COMMENT '标签显示名称',
  `tag_code`    VARCHAR(64)  NOT NULL COMMENT '标签编码（唯一标识）',
  `tag_type`    VARCHAR(16)  NOT NULL DEFAULT 'offline' COMMENT 'offline / realtime',
  `description` VARCHAR(200) NULL,
  `enabled`     TINYINT      NOT NULL DEFAULT 1,
  `created_by`  VARCHAR(64)  NULL,
  `created_at`  DATETIME     NULL,
  `updated_at`  DATETIME     NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY uk_tag_code_type (`tag_code`, `tag_type`),
  INDEX idx_type_enabled (`tag_type`, `enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标签定义库';

-- 将 /meta/tagger-tags 改为从 tag_definition 表读取
-- （MetaController 兜底：若 tag_definition 为空则 fallback 到 Tagger 服务）

-- 插入示例数据
INSERT INTO `tag_definition` (`name`, `tag_code`, `tag_type`, `description`, `enabled`, `created_by`, `created_at`, `updated_at`)
VALUES
  ('新客标签',   'new_user',      'offline', '首次下单30天内的用户', 1, 'system', NOW(), NOW()),
  ('高价值用户', 'high_value',    'offline', '近90天消费金额 TOP 20%', 1, 'system', NOW(), NOW()),
  ('流失风险',   'churn_risk',    'offline', '30天未访问的活跃用户', 1, 'system', NOW(), NOW()),
  ('实时活跃',   'realtime_active','realtime','最近1小时有行为的用户', 1, 'system', NOW(), NOW());
