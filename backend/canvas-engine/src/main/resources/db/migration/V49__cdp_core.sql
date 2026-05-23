-- V49: CDP core user profile, tag instances, tag history, batch tagging, and tag write node

ALTER TABLE tag_definition
    ADD COLUMN value_type VARCHAR(20) NOT NULL DEFAULT 'STRING' COMMENT 'STRING/NUMBER/BOOLEAN/JSON',
    ADD COLUMN manual_enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否允许人工打标',
    ADD COLUMN default_ttl_days INT NULL COMMENT '默认有效期天数，null=长期有效',
    ADD COLUMN category VARCHAR(64) NULL COMMENT '标签分类',
    ADD COLUMN owner VARCHAR(64) NULL COMMENT '负责人',
    ADD COLUMN write_policy VARCHAR(20) NOT NULL DEFAULT 'UPSERT' COMMENT '第一批仅启用UPSERT，APPEND为后续多值标签预留';

CREATE TABLE cdp_user_profile (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id          VARCHAR(128) NOT NULL COMMENT '系统内统一用户ID',
    display_name     VARCHAR(128) NULL,
    phone            VARCHAR(128) NULL,
    email            VARCHAR(256) NULL,
    status           VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    properties_json  JSON NULL COMMENT '轻量扩展属性',
    first_seen_at    DATETIME NULL,
    last_seen_at     DATETIME NULL,
    created_by       VARCHAR(64) NULL,
    created_at       DATETIME NULL,
    updated_at       DATETIME NULL,
    UNIQUE KEY uk_cdp_user_id (user_id),
    INDEX idx_last_seen (last_seen_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CDP用户档案';

CREATE TABLE cdp_user_identity (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id          VARCHAR(128) NOT NULL,
    identity_type    VARCHAR(32) NOT NULL COMMENT 'USER_ID/PHONE/EMAIL/DEVICE_ID/OPEN_ID',
    identity_value   VARCHAR(256) NOT NULL,
    source_type      VARCHAR(32) NULL,
    source_ref_id    VARCHAR(128) NULL,
    verified         TINYINT NOT NULL DEFAULT 0,
    created_at       DATETIME NULL,
    updated_at       DATETIME NULL,
    UNIQUE KEY uk_identity (identity_type, identity_value),
    INDEX idx_user_identity (user_id, identity_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CDP用户身份映射';

CREATE TABLE cdp_user_tag (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id          VARCHAR(128) NOT NULL,
    tag_code         VARCHAR(64) NOT NULL,
    tag_value        VARCHAR(1000) NULL,
    value_type       VARCHAR(20) NOT NULL DEFAULT 'STRING',
    source_type      VARCHAR(32) NOT NULL COMMENT 'MANUAL/CANVAS/BATCH/RULE/API/IMPORT',
    source_ref_id    VARCHAR(128) NULL COMMENT 'executionId/nodeId/jobId等来源引用',
    status           VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    effective_at     DATETIME NULL,
    expires_at       DATETIME NULL,
    created_by       VARCHAR(64) NULL,
    created_at       DATETIME NULL,
    updated_at       DATETIME NULL,
    UNIQUE KEY uk_user_tag (user_id, tag_code),
    INDEX idx_tag_user (tag_code, user_id),
    INDEX idx_user_status (user_id, status),
    INDEX idx_expires (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CDP用户当前标签';

CREATE TABLE cdp_user_tag_history (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id          VARCHAR(128) NOT NULL,
    tag_code         VARCHAR(64) NOT NULL,
    old_value        VARCHAR(1000) NULL,
    new_value        VARCHAR(1000) NULL,
    operation        VARCHAR(20) NOT NULL COMMENT 'SET/REMOVE/EXPIRE',
    source_type      VARCHAR(32) NOT NULL,
    source_ref_id    VARCHAR(128) NULL,
    idempotency_key  VARCHAR(256) NULL COMMENT '幂等键，画布来源使用executionId:nodeId:userId:tagCode',
    reason           VARCHAR(500) NULL,
    operator         VARCHAR(64) NULL,
    operated_at      DATETIME NULL,
    UNIQUE KEY uk_tag_history_idempotency (idempotency_key),
    INDEX idx_user_tag_history (user_id, tag_code, operated_at),
    INDEX idx_source_history (source_type, source_ref_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CDP用户标签历史';

CREATE TABLE cdp_tag_operation (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    operation_type  VARCHAR(20) NOT NULL COMMENT 'BATCH_SET/BATCH_REMOVE',
    tag_code        VARCHAR(64) NOT NULL,
    tag_value       VARCHAR(1000) NULL,
    total_count     INT NOT NULL DEFAULT 0,
    success_count   INT NOT NULL DEFAULT 0,
    fail_count      INT NOT NULL DEFAULT 0,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    error_msg       VARCHAR(1000) NULL,
    created_by      VARCHAR(64) NULL,
    created_at      DATETIME NULL,
    updated_at      DATETIME NULL,
    INDEX idx_tag_operation_status (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CDP批量标签任务';

INSERT INTO node_type_registry
(type_key, type_name, category, handler_class, config_schema, output_schema, is_trigger, is_terminal, description, enabled)
VALUES
(
  'CDP_TAG_WRITE',
  '写用户标签',
  '行为策略',
  'org.chovy.canvas.engine.handlers.CdpTagWriteHandler',
  '[{"key":"tagCode","label":"标签","type":"select","dataSource":"/meta/tagger-tags","required":true},{"key":"valueMode","label":"标签值来源","type":"radio","required":true,"options":[{"label":"固定值","value":"fixed"},{"label":"上下文字段","value":"context"}]},{"key":"tagValue","label":"标签值","type":"text","showWhen":"valueMode==fixed"},{"key":"tagValueField","label":"上下文字段","type":"select","dataSource":"/meta/context-fields","showWhen":"valueMode==context"},{"key":"reason","label":"原因","type":"text"},{"key":"nextNodeId","label":"下一节点","type":"node-select"}]',
  '[{"fieldKey":"tagCode","fieldName":"标签编码","dataType":"STRING"},{"fieldKey":"tagValue","fieldName":"标签值","dataType":"STRING"},{"fieldKey":"tagWriteStatus","fieldName":"标签写入状态","dataType":"STRING"}]',
  0,
  0,
  '将当前执行用户写入CDP标签实例表',
  1
)
ON DUPLICATE KEY UPDATE
  type_name = VALUES(type_name),
  category = VALUES(category),
  handler_class = VALUES(handler_class),
  config_schema = VALUES(config_schema),
  output_schema = VALUES(output_schema),
  description = VALUES(description),
  enabled = VALUES(enabled);
