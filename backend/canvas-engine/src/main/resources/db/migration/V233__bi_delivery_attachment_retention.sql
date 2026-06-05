ALTER TABLE bi_delivery_attachment
  ADD COLUMN retention_days INT NULL AFTER size_bytes,
  ADD COLUMN expires_at DATETIME NULL AFTER retention_days,
  ADD COLUMN download_count INT NOT NULL DEFAULT 0 AFTER expires_at,
  ADD COLUMN last_downloaded_at DATETIME NULL AFTER download_count;

CREATE INDEX idx_bi_delivery_attachment_expiry
  ON bi_delivery_attachment (tenant_id, status, expires_at);
