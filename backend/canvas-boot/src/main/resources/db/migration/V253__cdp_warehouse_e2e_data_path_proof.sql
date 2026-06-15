ALTER TABLE cdp_warehouse_e2e_certification_run
  ADD COLUMN require_data_path_proof TINYINT NOT NULL DEFAULT 0 AFTER require_realtime,
  ADD COLUMN data_path_proof_json MEDIUMTEXT NULL AFTER realtime_job_status_json;
