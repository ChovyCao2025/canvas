CREATE TABLE IF NOT EXISTS risk_simulation_run (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  simulation_id VARCHAR(128) NOT NULL,
  scene_key VARCHAR(128) NOT NULL,
  strategy_key VARCHAR(128) NOT NULL,
  baseline_version INT NOT NULL,
  candidate_version INT NOT NULL,
  status VARCHAR(32) NOT NULL,
  sample_size INT NOT NULL,
  changed_action_count INT NOT NULL,
  action_distribution_json JSON NOT NULL,
  action_changes_json JSON NOT NULL,
  created_at DATETIME(3) NOT NULL,
  UNIQUE KEY uk_risk_simulation_run (tenant_id, simulation_id),
  KEY idx_risk_simulation_scene_time (tenant_id, scene_key, created_at),
  KEY idx_risk_simulation_strategy_time (tenant_id, strategy_key, created_at)
);
