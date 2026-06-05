CREATE TABLE IF NOT EXISTS cdp_computed_tag_definition (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    tag_code VARCHAR(128) NOT NULL,
    display_name VARCHAR(128) NOT NULL,
    value_type VARCHAR(32) NOT NULL DEFAULT 'STRING',
    compute_type VARCHAR(32) NOT NULL,
    expression_json JSON NOT NULL,
    refresh_mode VARCHAR(32) NOT NULL DEFAULT 'MANUAL',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_by VARCHAR(128) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_cdp_computed_tag_definition (tenant_id, tag_code),
    INDEX idx_cdp_computed_tag_status (tenant_id, status, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CDP computed tag definitions';

CREATE TABLE IF NOT EXISTS cdp_computed_tag_dependency (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    tag_code VARCHAR(128) NOT NULL,
    depends_on_tag_code VARCHAR(128) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_cdp_computed_tag_dependency (tenant_id, tag_code, depends_on_tag_code),
    INDEX idx_cdp_computed_tag_dependency_reverse (tenant_id, depends_on_tag_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CDP computed tag dependency edges';

CREATE TABLE IF NOT EXISTS cdp_computed_tag_run (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    tag_code VARCHAR(128) NOT NULL,
    status VARCHAR(20) NOT NULL,
    cycle_path VARCHAR(1000) NULL,
    scanned_count BIGINT NOT NULL DEFAULT 0,
    matched_count BIGINT NOT NULL DEFAULT 0,
    updated_count BIGINT NOT NULL DEFAULT 0,
    skipped_count BIGINT NOT NULL DEFAULT 0,
    failed_count BIGINT NOT NULL DEFAULT 0,
    error_message VARCHAR(1000) NULL,
    started_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at DATETIME NULL,
    INDEX idx_cdp_computed_tag_run_tag (tenant_id, tag_code, started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CDP computed tag run history';
