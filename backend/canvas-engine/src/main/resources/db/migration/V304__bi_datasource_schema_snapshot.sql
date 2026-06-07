CREATE TABLE IF NOT EXISTS bi_datasource_schema_snapshot (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  data_source_config_id BIGINT NOT NULL,
  source_key VARCHAR(128) NOT NULL,
  connector_type VARCHAR(64) NOT NULL,
  schema_json JSON NOT NULL,
  sync_status VARCHAR(32) NOT NULL,
  error_message VARCHAR(1024),
  table_count INT NOT NULL DEFAULT 0,
  column_count INT NOT NULL DEFAULT 0,
  synced_by VARCHAR(128),
  synced_at DATETIME NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_bi_datasource_schema_snapshot_source_synced (tenant_id, data_source_config_id, synced_at),
  KEY idx_bi_datasource_schema_snapshot_key_synced (tenant_id, source_key, synced_at),
  KEY idx_bi_datasource_schema_snapshot_status (tenant_id, sync_status, synced_at)
);
