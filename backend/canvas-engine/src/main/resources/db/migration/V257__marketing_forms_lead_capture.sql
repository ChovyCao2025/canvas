CREATE TABLE IF NOT EXISTS marketing_form_definition (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  public_key VARCHAR(96) NOT NULL,
  name VARCHAR(128) NOT NULL,
  description VARCHAR(500) NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  field_schema_json JSON NOT NULL,
  submit_action_json JSON NULL,
  success_message VARCHAR(500) NULL,
  created_by VARCHAR(128) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_marketing_form_public_key (public_key),
  INDEX idx_marketing_form_tenant_status (tenant_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS marketing_form_submission (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  form_id BIGINT NOT NULL,
  public_key VARCHAR(96) NOT NULL,
  user_id VARCHAR(128) NULL,
  anonymous_id VARCHAR(128) NULL,
  response_json JSON NOT NULL,
  utm_json JSON NULL,
  consent_channel VARCHAR(64) NULL,
  consent_status VARCHAR(32) NULL,
  idempotency_key VARCHAR(128) NOT NULL,
  user_agent VARCHAR(512) NULL,
  submit_ip_hash VARCHAR(128) NULL,
  trigger_event_code VARCHAR(128) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_marketing_form_submission_idempotency (tenant_id, form_id, idempotency_key),
  INDEX idx_marketing_form_submission_form (form_id, created_at),
  INDEX idx_marketing_form_submission_user (tenant_id, user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
