ALTER TABLE bi_export_job
  ADD COLUMN retention_days INT NULL AFTER file_url,
  ADD COLUMN expires_at DATETIME NULL AFTER retention_days,
  ADD COLUMN download_count INT NOT NULL DEFAULT 0 AFTER expires_at,
  ADD COLUMN last_downloaded_at DATETIME NULL AFTER download_count;

CREATE INDEX idx_bi_export_job_expiry
  ON bi_export_job (tenant_id, status, expires_at);
