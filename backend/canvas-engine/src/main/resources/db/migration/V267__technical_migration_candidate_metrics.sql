CREATE TABLE IF NOT EXISTS technical_migration_candidate_evidence (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  candidate_key VARCHAR(128) NOT NULL,
  proof_command VARCHAR(1000) NOT NULL,
  baseline_result_json JSON NOT NULL,
  rollback_command VARCHAR(1000) NOT NULL,
  decision_status VARCHAR(32) NOT NULL DEFAULT 'BLOCKED_PENDING_REVIEW',
  submitted_by VARCHAR(128) NOT NULL,
  reviewed_by VARCHAR(128) NULL,
  reviewed_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_migration_candidate_latest (tenant_id, candidate_key, created_at),
  INDEX idx_migration_candidate_status (tenant_id, decision_status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
