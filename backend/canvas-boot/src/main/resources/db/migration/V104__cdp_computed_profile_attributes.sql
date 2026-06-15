CREATE TABLE IF NOT EXISTS cdp_computed_profile_attribute (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    attr_code VARCHAR(128) NOT NULL,
    display_name VARCHAR(128) NOT NULL,
    value_type VARCHAR(32) NOT NULL,
    compute_type VARCHAR(32) NOT NULL,
    expression_json JSON NOT NULL,
    refresh_mode VARCHAR(32) NOT NULL DEFAULT 'MANUAL',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_by VARCHAR(128) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_cdp_computed_profile_attr (tenant_id, attr_code),
    INDEX idx_cdp_computed_profile_status (tenant_id, status, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CDP computed profile attributes';

CREATE TABLE IF NOT EXISTS cdp_computed_profile_run (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    attr_id BIGINT NOT NULL,
    source_event_id VARCHAR(128) NULL,
    status VARCHAR(20) NOT NULL,
    scanned_count BIGINT NOT NULL DEFAULT 0,
    matched_count BIGINT NOT NULL DEFAULT 0,
    changed_count BIGINT NOT NULL DEFAULT 0,
    unchanged_count BIGINT NOT NULL DEFAULT 0,
    error_message VARCHAR(1000) NULL,
    started_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at DATETIME NULL,
    UNIQUE KEY uk_cdp_profile_run_event (tenant_id, attr_id, source_event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CDP computed profile run history';

CREATE TABLE IF NOT EXISTS cdp_profile_attribute_change_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    attr_code VARCHAR(128) NOT NULL,
    user_id VARCHAR(128) NOT NULL,
    old_value VARCHAR(1000) NULL,
    new_value VARCHAR(1000) NULL,
    source_run_id BIGINT NOT NULL,
    changed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_cdp_profile_attr_change_user (tenant_id, user_id, changed_at),
    INDEX idx_cdp_profile_attr_change_attr (tenant_id, attr_code, changed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CDP computed profile attribute change log';
