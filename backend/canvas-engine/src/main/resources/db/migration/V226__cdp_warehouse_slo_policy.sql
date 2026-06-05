CREATE TABLE IF NOT EXISTS cdp_warehouse_slo_policy (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    policy_key VARCHAR(128) NOT NULL,
    display_name VARCHAR(128) NOT NULL,
    offline_warn_run_gap_minutes INT NOT NULL DEFAULT 120,
    offline_fail_run_gap_minutes INT NOT NULL DEFAULT 360,
    offline_warn_watermark_lag_minutes INT NOT NULL DEFAULT 30,
    offline_fail_watermark_lag_minutes INT NOT NULL DEFAULT 120,
    audience_warn_run_gap_minutes INT NOT NULL DEFAULT 1440,
    audience_fail_run_gap_minutes INT NOT NULL DEFAULT 4320,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    owner_name VARCHAR(128) DEFAULT NULL,
    description VARCHAR(1000) DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_cdp_warehouse_slo_policy_key (tenant_id, policy_key),
    INDEX idx_cdp_warehouse_slo_policy_status (tenant_id, status, policy_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CDP warehouse readiness SLO policy thresholds';

INSERT INTO cdp_warehouse_slo_policy
(tenant_id, policy_key, display_name,
 offline_warn_run_gap_minutes, offline_fail_run_gap_minutes,
 offline_warn_watermark_lag_minutes, offline_fail_watermark_lag_minutes,
 audience_warn_run_gap_minutes, audience_fail_run_gap_minutes,
 status, owner_name, description)
VALUES
(0, 'WAREHOUSE_READINESS_DEFAULT', 'Warehouse Readiness Default',
 120, 360,
 30, 120,
 1440, 4320,
 'ACTIVE', 'data-platform',
 'Default readiness thresholds for offline sync, watermarks, and audience materialization recency.')
ON DUPLICATE KEY UPDATE
    display_name = VALUES(display_name),
    offline_warn_run_gap_minutes = VALUES(offline_warn_run_gap_minutes),
    offline_fail_run_gap_minutes = VALUES(offline_fail_run_gap_minutes),
    offline_warn_watermark_lag_minutes = VALUES(offline_warn_watermark_lag_minutes),
    offline_fail_watermark_lag_minutes = VALUES(offline_fail_watermark_lag_minutes),
    audience_warn_run_gap_minutes = VALUES(audience_warn_run_gap_minutes),
    audience_fail_run_gap_minutes = VALUES(audience_fail_run_gap_minutes),
    status = VALUES(status),
    owner_name = VALUES(owner_name),
    description = VALUES(description),
    updated_at = CURRENT_TIMESTAMP;
