ALTER TABLE bi_export_job
  ADD COLUMN storage_provider VARCHAR(64) NULL AFTER file_url,
  ADD COLUMN storage_key VARCHAR(1000) NULL AFTER storage_provider;

ALTER TABLE bi_delivery_attachment
  ADD COLUMN storage_provider VARCHAR(64) NULL AFTER file_url,
  ADD COLUMN storage_key VARCHAR(1000) NULL AFTER storage_provider;
