CREATE TABLE `user_input_form` (
  `id`                BIGINT       NOT NULL AUTO_INCREMENT,
  `tenant_id`         BIGINT       NULL,
  `canvas_id`         BIGINT       NOT NULL,
  `version_id`        BIGINT       NOT NULL,
  `execution_id`      VARCHAR(64)  NOT NULL,
  `node_id`           VARCHAR(64)  NOT NULL,
  `user_id`           VARCHAR(128) NOT NULL,
  `schema_json`       JSON         NOT NULL,
  `completed_node_id` VARCHAR(64)  NULL,
  `timeout_node_id`   VARCHAR(64)  NULL,
  `expires_at`        DATETIME     NULL,
  `created_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_input_form_runtime` (`execution_id`, `node_id`, `user_id`),
  KEY `idx_user_input_form_tenant_canvas` (`tenant_id`, `canvas_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户输入节点表单快照';

CREATE TABLE `user_input_response` (
  `id`                BIGINT       NOT NULL AUTO_INCREMENT,
  `tenant_id`         BIGINT       NULL,
  `form_id`           BIGINT       NOT NULL,
  `canvas_id`         BIGINT       NOT NULL,
  `version_id`        BIGINT       NOT NULL,
  `execution_id`      VARCHAR(64)  NOT NULL,
  `node_id`           VARCHAR(64)  NOT NULL,
  `user_id`           VARCHAR(128) NOT NULL,
  `response_json`     JSON         NULL,
  `status`            VARCHAR(32)  NOT NULL,
  `idempotency_key`   VARCHAR(128) NOT NULL,
  `completed_node_id` VARCHAR(64)  NULL,
  `timeout_node_id`   VARCHAR(64)  NULL,
  `expires_at`        DATETIME     NULL,
  `created_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_input_response_idempotency` (`tenant_id`, `idempotency_key`),
  KEY `idx_user_input_response_execution` (`execution_id`, `node_id`, `user_id`),
  KEY `idx_user_input_response_status_expire` (`status`, `expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户输入节点响应状态';

CREATE TABLE `user_input_resume_audit` (
  `id`                BIGINT       NOT NULL AUTO_INCREMENT,
  `tenant_id`         BIGINT       NULL,
  `response_id`       BIGINT       NOT NULL,
  `execution_id`      VARCHAR(64)  NOT NULL,
  `node_id`           VARCHAR(64)  NOT NULL,
  `user_id`           VARCHAR(128) NOT NULL,
  `resume_status`     VARCHAR(32)  NOT NULL,
  `resume_payload`    JSON         NULL,
  `created_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_input_audit_response` (`response_id`, `created_at`),
  KEY `idx_user_input_audit_execution` (`execution_id`, `node_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户输入恢复审计';

INSERT INTO node_type_registry
  (type_key, type_name, category, handler_class, config_schema, output_schema, outlet_schema, summary_template, runtime_policy_schema, risk_level, is_trigger, is_terminal, description, enabled)
VALUES
  ('USER_INPUT','用户输入','等待与汇聚','org.chovy.canvas.engine.handlers.UserInputHandler',
   '[{"key":"formSchema","label":"表单字段","type":"user-input-field-list","required":true},{"key":"maxWait","label":"最长等待","type":"duration","required":false},{"key":"completedNodeId","label":"完成分支","type":"node-select","required":false},{"key":"timeoutNodeId","label":"超时分支","type":"node-select","required":false}]',
   '[{"fieldKey":"inputStatus","fieldName":"输入状态","dataType":"STRING"},{"fieldKey":"inputResponseId","fieldName":"响应ID","dataType":"NUMBER"},{"fieldKey":"inputResponse","fieldName":"输入响应","dataType":"OBJECT"}]',
   '[{"id":"completed","label":"完成","color":"#52c41a","targetField":"completedNodeId"},{"id":"timeout","label":"超时","color":"#faad14","targetField":"timeoutNodeId"}]',
   '等待用户输入（{{formSchema}}）',
   '[]',
   'MEDIUM',
   0,0,'等待用户提交表单后继续，支持超时分支和响应审计。',1)
ON DUPLICATE KEY UPDATE
  type_name = VALUES(type_name),
  category = VALUES(category),
  handler_class = VALUES(handler_class),
  config_schema = VALUES(config_schema),
  output_schema = VALUES(output_schema),
  outlet_schema = VALUES(outlet_schema),
  summary_template = VALUES(summary_template),
  runtime_policy_schema = VALUES(runtime_policy_schema),
  risk_level = VALUES(risk_level),
  enabled = VALUES(enabled);
