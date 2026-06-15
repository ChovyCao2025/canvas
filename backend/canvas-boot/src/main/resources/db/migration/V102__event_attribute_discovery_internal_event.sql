ALTER TABLE event_definition
    ADD COLUMN auto_discover TINYINT NOT NULL DEFAULT 0 COMMENT 'Whether SDK ingestion can discover new attributes',
    ADD COLUMN discovery_mode VARCHAR(32) NOT NULL DEFAULT 'REJECT_UNKNOWN' COMMENT 'REJECT_UNKNOWN or PENDING_REVIEW';

CREATE TABLE IF NOT EXISTS event_attr_definition (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    event_code VARCHAR(128) NOT NULL,
    attr_name VARCHAR(128) NOT NULL,
    attr_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING_REVIEW',
    sample_value VARCHAR(1000) NULL,
    first_seen_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_seen_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    approved_by VARCHAR(128) NULL,
    approved_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_event_attr_definition (tenant_id, event_code, attr_name),
    INDEX idx_event_attr_status (tenant_id, status, last_seen_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Discovered event attribute definitions';
