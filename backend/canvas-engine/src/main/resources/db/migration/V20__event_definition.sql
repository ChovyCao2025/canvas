-- V20: 事件定义表 + 事件上报日志表
CREATE TABLE `event_definition` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `name`          VARCHAR(64)  NOT NULL COMMENT '事件显示名称',
  `event_code`    VARCHAR(64)  NOT NULL COMMENT '事件编码（唯一，业务上报时使用）',
  `attributes`    TEXT         NULL     COMMENT '属性定义 JSON [{name,displayName,type,required}]',
  `description`   VARCHAR(200) NULL,
  `enabled`       TINYINT      NOT NULL DEFAULT 1,
  `created_by`    VARCHAR(64)  NULL,
  `created_at`    DATETIME     NULL,
  `updated_at`    DATETIME     NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY uk_event_code (`event_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='事件定义库';

CREATE TABLE `event_log` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `event_code`    VARCHAR(64)  NOT NULL,
  `user_id`       VARCHAR(64)  NOT NULL,
  `attributes`    TEXT         NULL     COMMENT '事件属性 JSON',
  `canvas_triggered` TINYINT   NOT NULL DEFAULT 0,
  `canvas_count`  INT          NOT NULL DEFAULT 0,
  `created_at`    DATETIME     NULL,
  PRIMARY KEY (`id`),
  INDEX idx_event_user (`event_code`, `user_id`),
  INDEX idx_created (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='事件上报日志';

-- BEHAVIOR_IN_APP：eventCode 改为从事件定义下拉，新增事件属性入参
UPDATE `node_type_registry`
SET `config_schema` = '[
  {"key":"eventCode","label":"触发事件","type":"select","dataSource":"/meta/event-definitions","required":true},
  {"key":"eventParams","label":"事件属性","type":"api-input-params","apiKeyField":"eventCode","defsSource":"/meta/event-definitions","required":false}
]'
WHERE `type_key` = 'BEHAVIOR_IN_APP';

-- 示例事件定义
INSERT INTO `event_definition` (`name`, `event_code`, `attributes`, `description`, `enabled`, `created_by`, `created_at`, `updated_at`) VALUES
('订单完成', 'ORDER_COMPLETE', '[{"name":"orderId","displayName":"订单号","type":"STRING","required":true},{"name":"amount","displayName":"订单金额","type":"NUMBER","required":true},{"name":"orderTime","displayName":"下单时间","type":"DATE","required":false}]', '用户完成订单支付后上报', 1, 'system', NOW(), NOW()),
('用户注册', 'USER_REGISTER',  '[{"name":"channel","displayName":"注册渠道","type":"STRING","required":false}]', '新用户注册成功后上报', 1, 'system', NOW(), NOW());
