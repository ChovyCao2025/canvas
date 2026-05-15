-- V19: MQ 消息定义管理表
CREATE TABLE `mq_message_definition` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT,
  `name`           VARCHAR(64)  NOT NULL COMMENT '消息显示名称',
  `message_code`   VARCHAR(64)  NOT NULL COMMENT '消息类型编码（唯一）',
  `topic`          VARCHAR(128) NOT NULL COMMENT 'MQ Topic',
  `request_schema` TEXT         NULL     COMMENT '消息参数定义 [{name,displayName,type,required}]',
  `description`    VARCHAR(200) NULL,
  `enabled`        TINYINT      NOT NULL DEFAULT 1,
  `created_by`     VARCHAR(64)  NULL,
  `created_at`     DATETIME     NULL,
  `updated_at`     DATETIME     NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY uk_message_code (`message_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MQ消息定义库';

-- 更新 SEND_MQ 节点 config_schema，参数改为 api-input-params 类型（复用 API_CALL 动态参数渲染）
UPDATE `node_type_registry`
SET `config_schema` = '[
  {"key":"messageCodeKey","label":"消息类型","type":"select","dataSource":"/meta/mq-definitions","required":true},
  {"key":"params","label":"消息参数","type":"api-input-params","required":false},
  {"key":"nextNodeId","label":"后继节点","type":"input","required":false}
]'
WHERE `type_key` = 'SEND_MQ';
