CREATE TABLE IF NOT EXISTS cdp_realtime_audience_event_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    audience_id BIGINT NOT NULL,
    source_event_id VARCHAR(128) NOT NULL,
    user_id VARCHAR(128) NOT NULL,
    operation VARCHAR(20) NOT NULL,
    event_time DATETIME(3) NULL,
    processed_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_cdp_realtime_audience_event (tenant_id, audience_id, source_event_id),
    INDEX idx_cdp_realtime_audience_event_user (tenant_id, user_id, processed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CDP realtime audience event log';

CREATE TABLE IF NOT EXISTS cdp_audience_snapshot (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    audience_id BIGINT NOT NULL,
    estimated_size BIGINT NOT NULL DEFAULT 0,
    bitmap_key VARCHAR(256) NOT NULL,
    snapshot_source VARCHAR(32) NOT NULL,
    created_by VARCHAR(128) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_cdp_audience_snapshot_audience (tenant_id, audience_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CDP audience snapshots';
