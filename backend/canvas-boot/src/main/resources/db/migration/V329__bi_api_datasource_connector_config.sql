ALTER TABLE data_source_config
  ADD COLUMN connector_config_json JSON NULL AFTER connection_mode;
