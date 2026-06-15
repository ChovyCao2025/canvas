CREATE TABLE IF NOT EXISTS cdp_warehouse_job_lease (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    lease_key VARCHAR(128) NOT NULL,
    owner_id VARCHAR(256) NOT NULL,
    lease_until DATETIME NOT NULL,
    last_acquired_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_cdp_warehouse_job_lease (tenant_id, lease_key),
    INDEX idx_cdp_warehouse_job_lease_until (tenant_id, lease_until)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CDP warehouse scheduler distributed lease';
