ALTER TABLE bi_export_job
  ADD COLUMN approval_status VARCHAR(32) NULL AFTER last_downloaded_at,
  ADD COLUMN approval_reason VARCHAR(1000) NULL AFTER approval_status,
  ADD COLUMN requested_by VARCHAR(128) NULL AFTER approval_reason,
  ADD COLUMN requested_at DATETIME NULL AFTER requested_by,
  ADD COLUMN reviewed_by VARCHAR(128) NULL AFTER requested_at,
  ADD COLUMN reviewed_at DATETIME NULL AFTER reviewed_by,
  ADD COLUMN review_comment VARCHAR(1000) NULL AFTER reviewed_at;

CREATE INDEX idx_bi_export_job_approval
  ON bi_export_job (tenant_id, approval_status, requested_at);
