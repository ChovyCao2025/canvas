CREATE TABLE IF NOT EXISTS cdp_warehouse_dataset_catalog (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    dataset_key VARCHAR(128) NOT NULL,
    layer VARCHAR(32) NOT NULL,
    physical_name VARCHAR(256) NOT NULL,
    display_name VARCHAR(128) NOT NULL,
    subject_area VARCHAR(128) DEFAULT NULL,
    source_system VARCHAR(128) DEFAULT NULL,
    owner_name VARCHAR(128) DEFAULT NULL,
    description VARCHAR(1000) DEFAULT NULL,
    freshness_sla_minutes INT DEFAULT NULL,
    pii_level VARCHAR(32) NOT NULL DEFAULT 'NORMAL',
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    schema_json JSON DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_cdp_warehouse_catalog_key (tenant_id, dataset_key),
    INDEX idx_cdp_warehouse_catalog_layer_status (tenant_id, layer, status),
    INDEX idx_cdp_warehouse_catalog_subject (tenant_id, subject_area),
    INDEX idx_cdp_warehouse_catalog_physical (tenant_id, physical_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CDP warehouse dataset catalog';

CREATE TABLE IF NOT EXISTS cdp_warehouse_lineage_edge (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    upstream_dataset_key VARCHAR(128) NOT NULL,
    downstream_dataset_key VARCHAR(128) NOT NULL,
    transform_type VARCHAR(64) NOT NULL,
    transform_ref VARCHAR(256) NOT NULL DEFAULT '',
    dependency_type VARCHAR(64) NOT NULL DEFAULT 'HARD',
    description VARCHAR(1000) DEFAULT NULL,
    active TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_cdp_warehouse_lineage_edge (tenant_id, upstream_dataset_key, downstream_dataset_key, transform_ref),
    INDEX idx_cdp_warehouse_lineage_upstream (tenant_id, upstream_dataset_key, active),
    INDEX idx_cdp_warehouse_lineage_downstream (tenant_id, downstream_dataset_key, active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CDP warehouse dataset lineage edges';

INSERT INTO cdp_warehouse_dataset_catalog
(tenant_id, dataset_key, layer, physical_name, display_name, subject_area, source_system, owner_name,
 description, freshness_sla_minutes, pii_level, status, schema_json)
VALUES
(0, 'cdp_ods_event_log', 'ODS', 'canvas_ods.cdp_event_log', 'CDP ODS Event Log',
 'CDP_EVENT', 'canvas-engine', 'data-platform',
 'Accepted CDP events mirrored from cdp_event_log into Doris ODS.', 5, 'PII_RELATED', 'ACTIVE',
 JSON_OBJECT('fields', JSON_ARRAY('tenant_id', 'event_code', 'user_id', 'anonymous_id', 'event_time', 'received_at'))),
(0, 'cdp_dwd_user_event_fact', 'DWD', 'canvas_dwd.cdp_user_event_fact', 'CDP DWD User Event Fact',
 'CDP_EVENT', 'canvas-engine', 'data-platform',
 'User-level event facts derived from ODS event logs.', 15, 'PII_RELATED', 'ACTIVE',
 JSON_OBJECT('fields', JSON_ARRAY('tenant_id', 'user_id', 'event_code', 'event_time', 'channel', 'canvas_id', 'event_date'))),
(0, 'cdp_dws_user_event_metric_daily', 'DWS', 'canvas_dws.user_event_metric_daily', 'CDP DWS User Event Metric Daily',
 'CDP_EVENT', 'canvas-engine', 'data-platform',
 'Daily per-user event metrics aggregated from DWD facts.', 30, 'PII_RELATED', 'ACTIVE',
 JSON_OBJECT('fields', JSON_ARRAY('stat_date', 'tenant_id', 'user_id', 'event_code', 'count_value', 'latest_event_time'))),
(0, 'canvas_daily_stats', 'BI', 'canvas_dws.canvas_daily_stats', 'BI Canvas Daily Stats',
 'CANVAS_BI', 'canvas-engine', 'marketing-ops',
 'Daily canvas execution stats exposed by the BI query compiler.', 60, 'NORMAL', 'ACTIVE',
 JSON_OBJECT('fields', JSON_ARRAY('stat_date', 'canvas_id', 'canvas_name', 'trigger_type', 'total_executions', 'success_count')))
ON DUPLICATE KEY UPDATE
    layer = VALUES(layer),
    physical_name = VALUES(physical_name),
    display_name = VALUES(display_name),
    subject_area = VALUES(subject_area),
    source_system = VALUES(source_system),
    owner_name = VALUES(owner_name),
    description = VALUES(description),
    freshness_sla_minutes = VALUES(freshness_sla_minutes),
    pii_level = VALUES(pii_level),
    status = VALUES(status),
    schema_json = VALUES(schema_json),
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO cdp_warehouse_lineage_edge
(tenant_id, upstream_dataset_key, downstream_dataset_key, transform_type, transform_ref, dependency_type, description, active)
VALUES
(0, 'cdp_ods_event_log', 'cdp_dwd_user_event_fact', 'SQL_INSERT_SELECT', 'CdpWarehouseAggregationService#dwdSql',
 'HARD', 'DWD user event facts are derived from CDP ODS events.', 1),
(0, 'cdp_dwd_user_event_fact', 'cdp_dws_user_event_metric_daily', 'SQL_INSERT_SELECT', 'CdpWarehouseAggregationService#dwsSql',
 'HARD', 'DWS daily user event metrics are aggregated from DWD facts.', 1)
ON DUPLICATE KEY UPDATE
    transform_type = VALUES(transform_type),
    dependency_type = VALUES(dependency_type),
    description = VALUES(description),
    active = VALUES(active),
    updated_at = CURRENT_TIMESTAMP;
