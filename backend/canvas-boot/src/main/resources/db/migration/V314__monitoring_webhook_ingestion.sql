ALTER TABLE marketing_monitor_source
  ADD COLUMN webhook_enabled TINYINT NOT NULL DEFAULT 0 AFTER metadata_json,
  ADD COLUMN webhook_secret_prefix VARCHAR(16) NULL AFTER webhook_enabled,
  ADD COLUMN webhook_secret_hash VARCHAR(120) NULL AFTER webhook_secret_prefix,
  ADD COLUMN webhook_secret_ciphertext VARCHAR(1000) NULL AFTER webhook_secret_hash,
  ADD COLUMN webhook_signature_tolerance_seconds INT NOT NULL DEFAULT 300 AFTER webhook_secret_ciphertext,
  ADD INDEX idx_marketing_monitor_source_webhook (tenant_id, source_key, webhook_enabled, enabled);
