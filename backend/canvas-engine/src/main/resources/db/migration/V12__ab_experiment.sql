-- V12: AB 实验管理表

CREATE TABLE `ab_experiment` (
  `id`               BIGINT       NOT NULL AUTO_INCREMENT,
  `name`             VARCHAR(64)  NOT NULL COMMENT '实验显示名称',
  `experiment_key`   VARCHAR(64)  NOT NULL COMMENT '实验唯一 Key',
  `description`      VARCHAR(200) NULL     COMMENT '实验说明',
  `enabled`          TINYINT      NOT NULL DEFAULT 1,
  `created_by`       VARCHAR(64)  NULL,
  `created_at`       DATETIME     NULL,
  `updated_at`       DATETIME     NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY uk_experiment_key (`experiment_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AB 实验定义表';

-- 更新 AB_SPLIT 节点 config_schema，使用 ab_experiment 下拉
UPDATE `node_type_registry`
SET `config_schema` = '[
  {"key":"experimentKey","label":"实验","type":"select","dataSource":"/meta/ab-experiments","required":true},
  {"key":"groups","label":"分组列表","type":"ab-group-list","required":true}
]'
WHERE `type_key` = 'AB_SPLIT';
