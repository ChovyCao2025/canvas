SET @ai_audit_canvas_id_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'ai_usage_audit'
      AND column_name = 'canvas_id'
);
SET @ai_audit_canvas_id_sql := IF(
    @ai_audit_canvas_id_exists = 0,
    "ALTER TABLE ai_usage_audit ADD COLUMN canvas_id BIGINT NULL AFTER tenant_id",
    "SELECT 1"
);
PREPARE ai_audit_canvas_id_stmt FROM @ai_audit_canvas_id_sql;
EXECUTE ai_audit_canvas_id_stmt;
DEALLOCATE PREPARE ai_audit_canvas_id_stmt;

SET @ai_audit_execution_id_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'ai_usage_audit'
      AND column_name = 'execution_id'
);
SET @ai_audit_execution_id_sql := IF(
    @ai_audit_execution_id_exists = 0,
    "ALTER TABLE ai_usage_audit ADD COLUMN execution_id VARCHAR(100) NULL AFTER canvas_id",
    "SELECT 1"
);
PREPARE ai_audit_execution_id_stmt FROM @ai_audit_execution_id_sql;
EXECUTE ai_audit_execution_id_stmt;
DEALLOCATE PREPARE ai_audit_execution_id_stmt;

SET @ai_audit_node_id_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'ai_usage_audit'
      AND column_name = 'node_id'
);
SET @ai_audit_node_id_sql := IF(
    @ai_audit_node_id_exists = 0,
    "ALTER TABLE ai_usage_audit ADD COLUMN node_id VARCHAR(100) NULL AFTER execution_id",
    "SELECT 1"
);
PREPARE ai_audit_node_id_stmt FROM @ai_audit_node_id_sql;
EXECUTE ai_audit_node_id_stmt;
DEALLOCATE PREPARE ai_audit_node_id_stmt;

SET @ai_audit_prompt_tokens_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'ai_usage_audit'
      AND column_name = 'prompt_tokens'
);
SET @ai_audit_prompt_tokens_sql := IF(
    @ai_audit_prompt_tokens_exists = 0,
    "ALTER TABLE ai_usage_audit ADD COLUMN prompt_tokens INT NULL AFTER latency_ms",
    "SELECT 1"
);
PREPARE ai_audit_prompt_tokens_stmt FROM @ai_audit_prompt_tokens_sql;
EXECUTE ai_audit_prompt_tokens_stmt;
DEALLOCATE PREPARE ai_audit_prompt_tokens_stmt;

SET @ai_audit_completion_tokens_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'ai_usage_audit'
      AND column_name = 'completion_tokens'
);
SET @ai_audit_completion_tokens_sql := IF(
    @ai_audit_completion_tokens_exists = 0,
    "ALTER TABLE ai_usage_audit ADD COLUMN completion_tokens INT NULL AFTER prompt_tokens",
    "SELECT 1"
);
PREPARE ai_audit_completion_tokens_stmt FROM @ai_audit_completion_tokens_sql;
EXECUTE ai_audit_completion_tokens_stmt;
DEALLOCATE PREPARE ai_audit_completion_tokens_stmt;

SET @ai_audit_runtime_idx_exists := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ai_usage_audit'
      AND index_name = 'idx_ai_audit_runtime'
);
SET @ai_audit_runtime_idx_sql := IF(
    @ai_audit_runtime_idx_exists = 0,
    "ALTER TABLE ai_usage_audit ADD INDEX idx_ai_audit_runtime (tenant_id, canvas_id, execution_id, node_id, created_at)",
    "SELECT 1"
);
PREPARE ai_audit_runtime_idx_stmt FROM @ai_audit_runtime_idx_sql;
EXECUTE ai_audit_runtime_idx_stmt;
DEALLOCATE PREPARE ai_audit_runtime_idx_stmt;

SET @canvas_node_type_column_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'canvas_node'
      AND column_name = 'type'
);
SET @canvas_node_ai_type_sql := IF(
    @canvas_node_type_column_exists = 0,
    "SELECT 1",
    "UPDATE canvas_node SET type = 'AI_LLM' WHERE type = 'AI_NEXT_BEST_ACTION'"
);
PREPARE canvas_node_ai_type_stmt FROM @canvas_node_ai_type_sql;
EXECUTE canvas_node_ai_type_stmt;
DEALLOCATE PREPARE canvas_node_ai_type_stmt;

DELETE FROM node_type_registry WHERE type_key = 'AI_NEXT_BEST_ACTION';

INSERT INTO node_type_registry
  (type_key, type_name, category, handler_class, config_schema, output_schema, outlet_schema, summary_template, runtime_policy_schema, risk_level, is_trigger, is_terminal, description, enabled)
VALUES
  ('AI_LLM','AI 智能节点','AI智能','org.chovy.canvas.engine.handlers.AiLlmHandler',
   '[{"key":"providerId","label":"AI 服务商","type":"select","dataSource":"/meta/ai-providers","required":false},{"key":"templateId","label":"提示词模板","type":"select","dataSource":"/meta/ai-templates","required":true},{"key":"modelKey","label":"模型","type":"select","dataSource":"/meta/ai-models","required":false},{"key":"promptOverride","label":"提示词覆盖","type":"textarea","required":false},{"key":"variables","label":"变量映射","type":"key-value-list","required":false},{"key":"temperature","label":"温度","type":"number","required":false},{"key":"timeoutMs","label":"超时毫秒","type":"number","required":false},{"key":"outputPrefix","label":"输出前缀","type":"input","required":false},{"key":"nextNodeId","label":"下一节点","type":"node-select","required":false},{"key":"failNodeId","label":"失败分支","type":"node-select","required":false}]',
   '[{"fieldKey":"ai_output","fieldName":"AI输出","dataType":"OBJECT"},{"fieldKey":"ai_status","fieldName":"AI状态","dataType":"STRING"},{"fieldKey":"ai_fallback_used","fieldName":"是否使用降级","dataType":"BOOLEAN"},{"fieldKey":"ai_latency_ms","fieldName":"AI耗时毫秒","dataType":"NUMBER"}]',
   '[{"id":"success","label":"成功","color":"#52c41a","targetField":"nextNodeId"},{"id":"fail","label":"失败","color":"#ff4d4f","targetField":"failNodeId"}]',
   '使用 AI 模板 {{templateId}} 生成结构化输出',
   '[]',
   'HIGH',
   0,0,'调用租户 AI 服务商，按提示词模板生成结构化 JSON，并在异常时返回模板默认值。',1)
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
  is_trigger = VALUES(is_trigger),
  is_terminal = VALUES(is_terminal),
  description = VALUES(description),
  enabled = VALUES(enabled);
