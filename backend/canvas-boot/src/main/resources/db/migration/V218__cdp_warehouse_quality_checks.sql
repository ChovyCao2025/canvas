CREATE TABLE IF NOT EXISTS cdp_warehouse_quality_check (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    check_type VARCHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL,
    source_count BIGINT NULL,
    warehouse_count BIGINT NULL,
    diff_count BIGINT NULL,
    window_start DATETIME NULL,
    window_end DATETIME NULL,
    threshold_value BIGINT NULL,
    details_json TEXT NULL,
    checked_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(128) NULL,
    INDEX idx_cdp_warehouse_quality_status (tenant_id, status, checked_at),
    INDEX idx_cdp_warehouse_quality_type (tenant_id, check_type, checked_at),
    INDEX idx_cdp_warehouse_quality_window (tenant_id, check_type, window_start, window_end)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CDP warehouse quality and reconciliation check ledger';
