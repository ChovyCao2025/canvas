CREATE TABLE IF NOT EXISTS risk_scene (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  scene_key VARCHAR(128) NOT NULL,
  name VARCHAR(255) NOT NULL,
  event_schema_key VARCHAR(128) NOT NULL,
  status VARCHAR(32) NOT NULL,
  default_mode VARCHAR(32) NOT NULL,
  fail_policy VARCHAR(32) NOT NULL,
  latency_budget_ms INT NOT NULL,
  owner VARCHAR(128) NOT NULL,
  created_at DATETIME(3) NOT NULL,
  updated_at DATETIME(3) NOT NULL,
  UNIQUE KEY uk_risk_scene_tenant_key (tenant_id, scene_key),
  KEY idx_risk_scene_tenant_status (tenant_id, status)
);

CREATE TABLE IF NOT EXISTS risk_strategy (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  scene_key VARCHAR(128) NOT NULL,
  strategy_key VARCHAR(128) NOT NULL,
  name VARCHAR(255) NOT NULL,
  status VARCHAR(32) NOT NULL,
  active_version INT NULL,
  draft_version INT NULL,
  risk_level VARCHAR(32) NOT NULL,
  owner VARCHAR(128) NOT NULL,
  created_at DATETIME(3) NOT NULL,
  updated_at DATETIME(3) NOT NULL,
  UNIQUE KEY uk_risk_strategy_tenant_key (tenant_id, strategy_key),
  KEY idx_risk_strategy_tenant_scene (tenant_id, scene_key)
);

CREATE TABLE IF NOT EXISTS risk_strategy_version (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  strategy_key VARCHAR(128) NOT NULL,
  version INT NOT NULL,
  mode VARCHAR(32) NOT NULL,
  traffic_percent DECIMAL(5,2) NOT NULL DEFAULT 0,
  compiled_hash VARCHAR(128) NOT NULL,
  definition_json JSON NOT NULL,
  validation_json JSON NULL,
  created_by VARCHAR(128) NOT NULL,
  approved_by VARCHAR(128) NULL,
  approved_at DATETIME(3) NULL,
  effective_from DATETIME(3) NULL,
  effective_to DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL,
  updated_at DATETIME(3) NOT NULL,
  UNIQUE KEY uk_risk_strategy_version (tenant_id, strategy_key, version),
  KEY idx_risk_strategy_version_mode (tenant_id, strategy_key, mode)
);

CREATE TABLE IF NOT EXISTS risk_list (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  list_key VARCHAR(128) NOT NULL,
  list_type VARCHAR(32) NOT NULL,
  subject_type VARCHAR(32) NOT NULL,
  status VARCHAR(32) NOT NULL,
  requires_approval BOOLEAN NOT NULL DEFAULT TRUE,
  owner VARCHAR(128) NOT NULL,
  created_at DATETIME(3) NOT NULL,
  updated_at DATETIME(3) NOT NULL,
  UNIQUE KEY uk_risk_list_tenant_key (tenant_id, list_key)
);

CREATE TABLE IF NOT EXISTS risk_list_entry (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  list_key VARCHAR(128) NOT NULL,
  subject_hash VARCHAR(128) NOT NULL,
  subject_masked VARCHAR(255) NOT NULL,
  reason VARCHAR(512) NOT NULL,
  source VARCHAR(128) NOT NULL,
  effective_from DATETIME(3) NOT NULL,
  expires_at DATETIME(3) NULL,
  created_by VARCHAR(128) NOT NULL,
  approval_id BIGINT NULL,
  created_at DATETIME(3) NOT NULL,
  UNIQUE KEY uk_risk_list_entry_subject (tenant_id, list_key, subject_hash),
  KEY idx_risk_list_entry_expiry (tenant_id, list_key, expires_at)
);

CREATE TABLE IF NOT EXISTS risk_decision_run (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  request_id VARCHAR(128) NOT NULL,
  request_hash VARCHAR(128) NOT NULL,
  scene_key VARCHAR(128) NOT NULL,
  strategy_key VARCHAR(128) NOT NULL,
  strategy_version INT NOT NULL,
  subject_hash VARCHAR(128) NOT NULL,
  decision VARCHAR(32) NOT NULL,
  score INT NOT NULL,
  risk_band VARCHAR(32) NOT NULL,
  mode VARCHAR(32) NOT NULL,
  latency_ms INT NOT NULL,
  status VARCHAR(32) NOT NULL,
  input_snapshot_json JSON NOT NULL,
  output_json JSON NOT NULL,
  created_at DATETIME(3) NOT NULL,
  UNIQUE KEY uk_risk_decision_request (tenant_id, request_id),
  KEY idx_risk_decision_scene_time (tenant_id, scene_key, created_at),
  KEY idx_risk_decision_subject_time (tenant_id, subject_hash, created_at)
);

CREATE TABLE IF NOT EXISTS risk_rule_hit (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  decision_run_id BIGINT NOT NULL,
  strategy_key VARCHAR(128) NOT NULL,
  strategy_version INT NOT NULL,
  group_key VARCHAR(128) NOT NULL,
  rule_key VARCHAR(128) NOT NULL,
  mode VARCHAR(32) NOT NULL,
  action VARCHAR(32) NOT NULL,
  score_delta INT NOT NULL,
  reason_code VARCHAR(128) NOT NULL,
  evidence_json JSON NOT NULL,
  created_at DATETIME(3) NOT NULL,
  KEY idx_risk_rule_hit_run (tenant_id, decision_run_id),
  KEY idx_risk_rule_hit_rule_time (tenant_id, rule_key, created_at)
);
