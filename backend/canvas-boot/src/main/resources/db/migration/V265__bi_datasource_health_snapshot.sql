CREATE TABLE IF NOT EXISTS bi_datasource_health_snapshot (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  source_key VARCHAR(128) NOT NULL,
  source_type VARCHAR(64) NOT NULL,
  available TINYINT(1) NOT NULL,
  message VARCHAR(1024),
  checked_at DATETIME NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_bi_datasource_health_checked_at (checked_at),
  KEY idx_bi_datasource_health_source_checked_at (source_key, checked_at)
);
