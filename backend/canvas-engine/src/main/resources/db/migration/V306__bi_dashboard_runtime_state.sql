CREATE TABLE IF NOT EXISTS bi_dashboard_runtime_state (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT 'Tenant id',
    workspace_id BIGINT NOT NULL COMMENT 'BI workspace id',
    dashboard_key VARCHAR(128) NOT NULL COMMENT 'Dashboard key',
    username VARCHAR(128) NOT NULL COMMENT 'Runtime state owner',
    parameter_json JSON NOT NULL COMMENT 'Remembered runtime parameters',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_bi_dashboard_runtime_state_user (tenant_id, workspace_id, dashboard_key, username),
    INDEX idx_bi_dashboard_runtime_state_dashboard (tenant_id, workspace_id, dashboard_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='BI dashboard runtime state';
