ALTER TABLE cdp_warehouse_synthetic_data_path_probe_run
    ADD COLUMN source_mode VARCHAR(32) NOT NULL DEFAULT 'DIRECT_SINK' AFTER probe_key;

ALTER TABLE cdp_warehouse_synthetic_data_path_probe_run
    ADD COLUMN source_status VARCHAR(32) DEFAULT NULL AFTER status;
