CREATE TABLE IF NOT EXISTS cdp_warehouse_enterprise_olap_evidence (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    evidence_key VARCHAR(128) NOT NULL,
    source VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    measured_at DATETIME NOT NULL,
    expires_at DATETIME DEFAULT NULL,
    evidence_json JSON DEFAULT NULL,
    created_by VARCHAR(128) NOT NULL DEFAULT 'system',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_enterprise_olap_evidence_key (tenant_id, evidence_key, measured_at),
    INDEX idx_enterprise_olap_evidence_status (tenant_id, status, measured_at),
    INDEX idx_enterprise_olap_evidence_expiry (tenant_id, expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CDP warehouse enterprise OLAP operational evidence ledger';
