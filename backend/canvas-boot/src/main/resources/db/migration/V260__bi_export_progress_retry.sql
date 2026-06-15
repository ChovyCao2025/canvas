ALTER TABLE bi_export_job
  ADD COLUMN progress_percent INT NOT NULL DEFAULT 0 AFTER status,
  ADD COLUMN retry_count INT NOT NULL DEFAULT 0 AFTER error_message,
  ADD COLUMN max_retry_count INT NOT NULL DEFAULT 3 AFTER retry_count,
  ADD COLUMN next_retry_at DATETIME NULL AFTER max_retry_count,
  ADD COLUMN last_retry_at DATETIME NULL AFTER next_retry_at,
  ADD COLUMN retry_exhausted_at DATETIME NULL AFTER last_retry_at;

CREATE INDEX idx_bi_export_job_retry_due
  ON bi_export_job (tenant_id, status, next_retry_at, retry_exhausted_at, id);
