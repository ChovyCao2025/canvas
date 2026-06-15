CREATE TABLE IF NOT EXISTS cdp_warehouse_stream_schema (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    pipeline_key VARCHAR(128) NOT NULL,
    schema_role VARCHAR(16) NOT NULL,
    schema_version VARCHAR(64) NOT NULL,
    schema_hash VARCHAR(128) NOT NULL,
    schema_json JSON NOT NULL,
    compatibility_policy VARCHAR(32) NOT NULL DEFAULT 'BACKWARD',
    compatibility_status VARCHAR(32) NOT NULL DEFAULT 'COMPATIBLE',
    compatibility_reason VARCHAR(1000) DEFAULT NULL,
    active TINYINT NOT NULL DEFAULT 1,
    registered_by VARCHAR(128) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_cdp_warehouse_stream_schema_version (tenant_id, pipeline_key, schema_role, schema_version),
    INDEX idx_cdp_warehouse_stream_schema_latest (tenant_id, pipeline_key, schema_role, active, created_at),
    INDEX idx_cdp_warehouse_stream_schema_status (tenant_id, compatibility_status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CDP warehouse realtime stream schema version registry';

ALTER TABLE cdp_warehouse_stream_checkpoint
    ADD COLUMN source_schema_version VARCHAR(64) DEFAULT NULL,
    ADD COLUMN sink_schema_version VARCHAR(64) DEFAULT NULL,
    ADD COLUMN schema_status VARCHAR(32) DEFAULT NULL;
