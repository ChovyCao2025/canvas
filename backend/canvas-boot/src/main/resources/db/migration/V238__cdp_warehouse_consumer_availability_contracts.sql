CREATE TABLE IF NOT EXISTS cdp_warehouse_asset_availability (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    asset_type VARCHAR(32) NOT NULL,
    asset_key VARCHAR(256) NOT NULL,
    availability_mode VARCHAR(32) NOT NULL DEFAULT 'HYBRID',
    window_start DATETIME DEFAULT NULL,
    window_end DATETIME DEFAULT NULL,
    available_until DATETIME NOT NULL,
    status VARCHAR(32) NOT NULL,
    evidence_source VARCHAR(128) NOT NULL DEFAULT 'MANUAL',
    evidence_ref VARCHAR(256) DEFAULT NULL,
    reason VARCHAR(1000) DEFAULT NULL,
    observed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_cdp_warehouse_asset_availability_key
        (tenant_id, asset_type, asset_key, availability_mode, evidence_source),
    INDEX idx_cdp_warehouse_asset_availability_asset
        (tenant_id, asset_type, asset_key, availability_mode, observed_at),
    INDEX idx_cdp_warehouse_asset_availability_status
        (tenant_id, status, observed_at),
    INDEX idx_cdp_warehouse_asset_availability_window
        (tenant_id, availability_mode, available_until)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CDP warehouse table dataset metric availability observations';

CREATE TABLE IF NOT EXISTS cdp_warehouse_consumer_availability_contract (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    contract_key VARCHAR(128) NOT NULL,
    consumer_type VARCHAR(32) NOT NULL,
    consumer_ref VARCHAR(256) NOT NULL,
    dataset_key VARCHAR(128) DEFAULT NULL,
    metric_key VARCHAR(128) DEFAULT NULL,
    required_mode VARCHAR(32) NOT NULL DEFAULT 'HYBRID',
    required_assets_json JSON NOT NULL,
    gate_policy VARCHAR(32) NOT NULL DEFAULT 'BLOCK_ON_WARN',
    warn_tolerance_minutes INT NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    owner_name VARCHAR(128) DEFAULT NULL,
    description VARCHAR(1000) DEFAULT NULL,
    last_evaluated_at DATETIME DEFAULT NULL,
    last_status VARCHAR(32) DEFAULT NULL,
    last_message VARCHAR(1000) DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_cdp_warehouse_consumer_availability_contract
        (tenant_id, contract_key),
    INDEX idx_cdp_warehouse_consumer_availability_consumer
        (tenant_id, consumer_type, consumer_ref),
    INDEX idx_cdp_warehouse_consumer_availability_dataset
        (tenant_id, dataset_key, metric_key),
    INDEX idx_cdp_warehouse_consumer_availability_status
        (tenant_id, status, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CDP warehouse downstream consumer availability contracts';
