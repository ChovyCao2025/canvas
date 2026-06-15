CREATE TABLE IF NOT EXISTS audience_bitmap_rollback (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    audience_id BIGINT NOT NULL,
    target_version BIGINT NOT NULL,
    target_bitmap_key VARCHAR(256) NOT NULL,
    rolled_back_versions BIGINT NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL,
    reason VARCHAR(512) NOT NULL,
    operator_name VARCHAR(128) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_audience_bitmap_rollback_audience (tenant_id, audience_id, created_at),
    INDEX idx_audience_bitmap_rollback_operator (tenant_id, operator_name, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Audience bitmap rollback audit ledger';
