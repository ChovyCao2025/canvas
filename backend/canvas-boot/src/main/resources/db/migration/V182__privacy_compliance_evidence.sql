CREATE TABLE IF NOT EXISTS privacy_compliance_evidence (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  capability_key VARCHAR(128) NOT NULL,
  owner_id VARCHAR(128) NOT NULL,
  regulation_profile VARCHAR(128) NOT NULL,
  affected_data_classes TEXT NOT NULL,
  audit_artifact_notes TEXT NOT NULL,
  residency_impact_notes TEXT NOT NULL,
  threat_model_notes TEXT NOT NULL,
  proof_command VARCHAR(1000) NOT NULL,
  rollback_note VARCHAR(1000) NOT NULL,
  decision_status VARCHAR(32) NOT NULL DEFAULT 'BLOCKED_PENDING_REVIEW',
  reviewed_by VARCHAR(128) NULL,
  reviewed_at DATETIME NULL,
  child_spec VARCHAR(255) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_privacy_evidence_capability_status (capability_key, decision_status),
  INDEX idx_privacy_evidence_regulation (regulation_profile, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
