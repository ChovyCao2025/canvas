ALTER TABLE bi_delivery_log
  ADD COLUMN retry_count INT NOT NULL DEFAULT 0 AFTER error_message,
  ADD COLUMN max_retry_count INT NOT NULL DEFAULT 4 AFTER retry_count,
  ADD COLUMN next_retry_at DATETIME NULL AFTER max_retry_count,
  ADD COLUMN last_retry_at DATETIME NULL AFTER next_retry_at,
  ADD COLUMN retry_exhausted_at DATETIME NULL AFTER last_retry_at;

CREATE INDEX idx_bi_delivery_log_retry_due
  ON bi_delivery_log (tenant_id, status, next_retry_at, retry_exhausted_at, id);
