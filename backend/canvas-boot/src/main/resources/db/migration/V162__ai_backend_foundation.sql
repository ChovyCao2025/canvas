CREATE TABLE IF NOT EXISTS ai_provider (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  provider_key VARCHAR(128) NOT NULL,
  display_name VARCHAR(128) NOT NULL,
  provider_type VARCHAR(64) NOT NULL,
  endpoint VARCHAR(500) NOT NULL,
  encrypted_api_key VARCHAR(1000) NULL,
  default_model VARCHAR(128) NULL,
  default_params JSON NULL,
  enabled TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_ai_provider_tenant_key (tenant_id, provider_key),
  INDEX idx_ai_provider_tenant_enabled (tenant_id, enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ai_model_registry (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NULL,
  provider_id BIGINT NULL,
  model_key VARCHAR(128) NOT NULL,
  display_name VARCHAR(128) NOT NULL,
  capability VARCHAR(64) NOT NULL DEFAULT 'TEXT_JSON',
  context_window INT NOT NULL DEFAULT 8192,
  default_params JSON NULL,
  enabled TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_ai_model_tenant_provider (tenant_id, provider_id, enabled),
  INDEX idx_ai_model_key (model_key, enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ai_prompt_template (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NULL,
  name VARCHAR(128) NOT NULL,
  category VARCHAR(64) NOT NULL,
  prompt_template TEXT NOT NULL,
  output_schema JSON NOT NULL,
  default_values JSON NOT NULL,
  enabled TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_ai_template_tenant_category (tenant_id, category, enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ai_usage_audit (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  provider_id BIGINT NULL,
  template_id BIGINT NULL,
  model_key VARCHAR(128) NULL,
  status VARCHAR(32) NOT NULL,
  fallback_used TINYINT NOT NULL DEFAULT 0,
  latency_ms BIGINT NULL,
  rendered_prompt_hash VARCHAR(128) NULL,
  output_json JSON NULL,
  error_code VARCHAR(80) NULL,
  error_message VARCHAR(500) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_ai_audit_tenant_template (tenant_id, template_id, created_at),
  INDEX idx_ai_audit_tenant_provider (tenant_id, provider_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO ai_provider
  (tenant_id, provider_key, display_name, provider_type, endpoint, encrypted_api_key, default_model, default_params, enabled)
VALUES
  (0, 'mock-ai', 'Mock AI Provider', 'MOCK', 'mock://local', NULL, 'mock-marketing-v1', JSON_OBJECT('temperature', 0.2), 1)
ON DUPLICATE KEY UPDATE
  display_name = VALUES(display_name),
  provider_type = VALUES(provider_type),
  endpoint = VALUES(endpoint),
  default_model = VALUES(default_model),
  default_params = VALUES(default_params),
  enabled = VALUES(enabled);

INSERT INTO ai_model_registry
  (tenant_id, provider_id, model_key, display_name, capability, context_window, default_params, enabled)
SELECT
  NULL, p.id, 'mock-marketing-v1', 'Mock Marketing V1', 'TEXT_JSON', 8192, JSON_OBJECT('stub', true), 1
FROM ai_provider p
WHERE p.tenant_id = 0 AND p.provider_key = 'mock-ai'
  AND NOT EXISTS (
    SELECT 1 FROM ai_model_registry m
    WHERE m.provider_id = p.id AND m.model_key = 'mock-marketing-v1'
  );

INSERT INTO ai_prompt_template
  (tenant_id, name, category, prompt_template, output_schema, default_values, enabled)
SELECT NULL, 'Text Generation', 'text_generate',
       'Create a ${channelType} message for ${userProfile.name} about ${productInfo.name}.',
       JSON_OBJECT(
         'type', 'object',
         'properties', JSON_OBJECT(
           'text', JSON_OBJECT('type', 'string'),
           'tone', JSON_OBJECT('type', 'string')
         ),
         'required', JSON_ARRAY('text', 'tone')
       ),
       JSON_OBJECT('text', 'Your exclusive benefit is ready.', 'tone', 'warm'),
       1
WHERE NOT EXISTS (
  SELECT 1 FROM ai_prompt_template
  WHERE tenant_id IS NULL AND category = 'text_generate' AND name = 'Text Generation'
);

INSERT INTO ai_prompt_template
  (tenant_id, name, category, prompt_template, output_schema, default_values, enabled)
SELECT NULL, 'Smart Scoring', 'scoring',
       'Score user ${userId} from behavior ${behaviorData}.',
       JSON_OBJECT(
         'type', 'object',
         'properties', JSON_OBJECT(
           'score', JSON_OBJECT('type', 'number'),
           'band', JSON_OBJECT('type', 'string')
         ),
         'required', JSON_ARRAY('score', 'band')
       ),
       JSON_OBJECT('score', 50, 'band', 'medium'),
       1
WHERE NOT EXISTS (
  SELECT 1 FROM ai_prompt_template
  WHERE tenant_id IS NULL AND category = 'scoring' AND name = 'Smart Scoring'
);
