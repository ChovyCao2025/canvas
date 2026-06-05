CREATE TABLE IF NOT EXISTS cdp_warehouse_sync_run (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    job_type VARCHAR(32) NOT NULL,
    source_table VARCHAR(128) DEFAULT NULL,
    source_start_id BIGINT DEFAULT NULL,
    source_end_id BIGINT DEFAULT NULL,
    window_start DATETIME DEFAULT NULL,
    window_end DATETIME DEFAULT NULL,
    status VARCHAR(32) NOT NULL,
    loaded_rows BIGINT NOT NULL DEFAULT 0,
    failed_rows BIGINT NOT NULL DEFAULT 0,
    error_message VARCHAR(1000) DEFAULT NULL,
    started_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at DATETIME DEFAULT NULL,
    created_by VARCHAR(128) DEFAULT NULL,
    INDEX idx_cdp_warehouse_sync_status (tenant_id, job_type, status, started_at),
    INDEX idx_cdp_warehouse_sync_source (tenant_id, source_table, source_start_id, source_end_id),
    INDEX idx_cdp_warehouse_sync_window (tenant_id, job_type, window_start, window_end)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CDP warehouse sync and aggregation run ledger';

CREATE TABLE IF NOT EXISTS cdp_warehouse_watermark (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    job_name VARCHAR(128) NOT NULL,
    watermark_type VARCHAR(64) NOT NULL,
    watermark_value VARCHAR(256) NOT NULL,
    watermark_time DATETIME DEFAULT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_cdp_warehouse_watermark (tenant_id, job_name, watermark_type),
    INDEX idx_cdp_warehouse_watermark_time (tenant_id, job_name, watermark_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CDP warehouse replay and aggregation watermarks';
