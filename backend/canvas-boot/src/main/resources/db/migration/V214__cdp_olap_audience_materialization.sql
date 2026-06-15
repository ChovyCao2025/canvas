CREATE TABLE IF NOT EXISTS cdp_user_index (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    user_id VARCHAR(128) NOT NULL,
    user_index BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_cdp_user_index_user (tenant_id, user_id),
    UNIQUE KEY uk_cdp_user_index_value (tenant_id, user_index),
    INDEX idx_cdp_user_index_created (tenant_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Stable tenant user index for materialized audience bitmaps';

CREATE TABLE IF NOT EXISTS audience_bitmap_version (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    audience_id BIGINT NOT NULL,
    version BIGINT NOT NULL,
    bitmap_key VARCHAR(256) NOT NULL,
    estimated_size BIGINT NOT NULL DEFAULT 0,
    bitmap_size_kb BIGINT NOT NULL DEFAULT 0,
    source VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'WRITING',
    created_by VARCHAR(128) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ready_at DATETIME NULL,
    UNIQUE KEY uk_audience_bitmap_version (tenant_id, audience_id, version),
    INDEX idx_audience_bitmap_latest (tenant_id, audience_id, status, version),
    INDEX idx_audience_bitmap_created (tenant_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Versioned Redis bitmap metadata for materialized audiences';

CREATE TABLE IF NOT EXISTS audience_materialization_run (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    audience_id BIGINT NOT NULL,
    version BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    rule_json JSON NULL,
    matched_users BIGINT NOT NULL DEFAULT 0,
    bitmap_key VARCHAR(256) NULL,
    error_message VARCHAR(1000) NULL,
    started_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at DATETIME NULL,
    created_by VARCHAR(128) NULL,
    INDEX idx_audience_materialization_audience (tenant_id, audience_id, started_at),
    INDEX idx_audience_materialization_status (tenant_id, status, started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Audience OLAP materialization run ledger';

CREATE TABLE IF NOT EXISTS audience_quality_check (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    audience_id BIGINT NOT NULL,
    mysql_count BIGINT NOT NULL DEFAULT 0,
    doris_count BIGINT NOT NULL DEFAULT 0,
    bitmap_count BIGINT NOT NULL DEFAULT 0,
    freshness_lag_minutes BIGINT NOT NULL DEFAULT 0,
    bitmap_drift_ratio DECIMAL(18, 6) NOT NULL DEFAULT 0,
    verdict VARCHAR(16) NOT NULL,
    detail_json JSON NULL,
    checked_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_audience_quality_audience (tenant_id, audience_id, checked_at),
    INDEX idx_audience_quality_verdict (tenant_id, verdict, checked_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Audience materialization quality ledger';
