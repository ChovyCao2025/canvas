-- V11: API 定义管理表（供 API_CALL 节点下拉选择）

CREATE TABLE `api_definition` (
  `id`               BIGINT       NOT NULL AUTO_INCREMENT,
  `name`             VARCHAR(64)  NOT NULL COMMENT 'API 显示名称',
  `api_key`          VARCHAR(64)  NOT NULL COMMENT 'API 唯一标识 Key',
  `url`              VARCHAR(255) NOT NULL COMMENT '接口地址（含路径）',
  `method`           VARCHAR(8)   NOT NULL DEFAULT 'POST' COMMENT 'HTTP 方法',
  `biz_line`         VARCHAR(32)  NULL     COMMENT '所属业务线',
  `request_schema`   TEXT         NULL     COMMENT '入参字段定义 JSON [{name,type,required,desc}]',
  `response_schema`  TEXT         NULL     COMMENT '出参字段定义 JSON [{name,type,desc}]',
  `description`      VARCHAR(200) NULL     COMMENT '接口说明',
  `enabled`          TINYINT      NOT NULL DEFAULT 1,
  `created_by`       VARCHAR(64)  NULL,
  `created_at`       DATETIME     NULL,
  `updated_at`       DATETIME     NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY uk_api_key (`api_key`),
  INDEX idx_biz_line_enabled (`biz_line`, `enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='API 接口定义库';

-- 同步更新 API_CALL 节点 config_schema，使用 api_definition 下拉
UPDATE `node_type_registry`
SET `config_schema` = '[
  {"key":"apiKey","label":"选择接口","type":"select","dataSource":"/meta/api-definitions","required":true},
  {"key":"inputParams","label":"入参映射","type":"key-value","required":false},
  {"key":"outputPrefix","label":"出参前缀","type":"input","required":false},
  {"key":"nextNodeId","label":"后继节点","type":"input","required":false}
]'
WHERE `type_key` = 'API_CALL';
