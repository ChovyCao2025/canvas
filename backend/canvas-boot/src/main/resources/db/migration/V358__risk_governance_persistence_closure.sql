ALTER TABLE risk_strategy_version
  ADD COLUMN status VARCHAR(32) NOT NULL DEFAULT 'DRAFT' AFTER version,
  ADD COLUMN submitted_by VARCHAR(128) NULL AFTER created_by,
  ADD COLUMN submitted_at DATETIME(3) NULL AFTER submitted_by,
  ADD KEY idx_risk_strategy_version_status (tenant_id, strategy_key, status);
