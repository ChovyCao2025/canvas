ALTER TABLE bi_dataset_extract_refresh_run
  ADD COLUMN retention_status VARCHAR(32) NULL AFTER materialized_table,
  ADD COLUMN dropped_at DATETIME NULL AFTER finished_at;

CREATE INDEX idx_bi_dataset_extract_refresh_run_retention
  ON bi_dataset_extract_refresh_run (tenant_id, dataset_key, retention_status, finished_at);
