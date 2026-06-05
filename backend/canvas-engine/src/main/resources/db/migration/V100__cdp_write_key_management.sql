CREATE TABLE IF NOT EXISTS cdp_write_key (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    name VARCHAR(128) NOT NULL,
    key_prefix VARCHAR(16) NOT NULL,
    key_hash VARCHAR(120) NOT NULL,
    platform VARCHAR(32) NOT NULL DEFAULT 'WEB',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    rate_limit_qps INT NOT NULL DEFAULT 100,
    daily_quota BIGINT NULL,
    description VARCHAR(500) NULL,
    created_by VARCHAR(128) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_cdp_write_key_prefix (key_prefix),
    INDEX idx_cdp_write_key_tenant_status (tenant_id, status),
    INDEX idx_cdp_write_key_platform (tenant_id, platform)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CDP SDK write keys';
