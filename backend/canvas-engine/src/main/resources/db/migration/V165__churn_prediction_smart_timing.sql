CREATE TABLE IF NOT EXISTS ai_prediction_run (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    model_key VARCHAR(80) NOT NULL,
    model_version VARCHAR(80) NOT NULL,
    run_date DATE NOT NULL,
    status VARCHAR(30) NOT NULL,
    processed_count INT NOT NULL DEFAULT 0,
    skipped_count INT NOT NULL DEFAULT 0,
    failed_count INT NOT NULL DEFAULT 0,
    started_at DATETIME NOT NULL,
    finished_at DATETIME NULL,
    error_message VARCHAR(500) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ai_prediction_run (tenant_id, model_key, model_version, run_date),
    KEY idx_ai_prediction_run_status (tenant_id, status, started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ai_user_prediction_snapshot (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    run_id BIGINT NOT NULL,
    user_id VARCHAR(128) NOT NULL,
    model_key VARCHAR(80) NOT NULL,
    model_version VARCHAR(80) NOT NULL,
    churn_probability DECIMAL(6,5) NULL,
    churn_risk_band VARCHAR(20) NULL,
    best_send_hour TINYINT NULL,
    confidence DECIMAL(6,5) NOT NULL DEFAULT 0.50000,
    feature_json JSON NOT NULL,
    contribution_json JSON NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ai_user_prediction_snapshot (tenant_id, run_id, user_id, model_key),
    KEY idx_ai_prediction_user_latest (tenant_id, user_id, created_at),
    KEY idx_ai_prediction_band (tenant_id, churn_risk_band, churn_probability)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
