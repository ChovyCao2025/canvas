ALTER TABLE cdp_warehouse_e2e_certification_run
  ADD COLUMN require_realtime TINYINT NOT NULL DEFAULT 0 AFTER require_physical,
  ADD COLUMN realtime_pipeline_status_json MEDIUMTEXT NULL AFTER live_table_inspection_json,
  ADD COLUMN realtime_job_status_json MEDIUMTEXT NULL AFTER realtime_pipeline_status_json;
