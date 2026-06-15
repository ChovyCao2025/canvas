CREATE TABLE IF NOT EXISTS cdp_warehouse_enterprise_olap_evidence_collection_run (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  trigger_type VARCHAR(32) NOT NULL,
  status VARCHAR(32) NOT NULL,
  started_at DATETIME NOT NULL,
  finished_at DATETIME NULL,
  evidence_count INT NOT NULL DEFAULT 0,
  pass_count INT NOT NULL DEFAULT 0,
  warn_count INT NOT NULL DEFAULT 0,
  fail_count INT NOT NULL DEFAULT 0,
  reason VARCHAR(2000) NOT NULL,
  created_by VARCHAR(128) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_enterprise_olap_collection_tenant_time (tenant_id, started_at, id),
  KEY idx_enterprise_olap_collection_status (tenant_id, status, finished_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Enterprise OLAP evidence collection run history';
