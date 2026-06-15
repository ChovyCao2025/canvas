CREATE TABLE IF NOT EXISTS webhook_subscription (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    name VARCHAR(128) NOT NULL,
    callback_url VARCHAR(1000) NOT NULL,
    secret_prefix VARCHAR(16) NOT NULL,
    secret_hash VARCHAR(120) NOT NULL,
    secret_ciphertext VARCHAR(1000) NOT NULL,
    event_types JSON NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    max_attempts INT NOT NULL DEFAULT 3,
    created_by VARCHAR(128) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_webhook_subscription_tenant_status (tenant_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Outbound webhook subscriptions';

CREATE TABLE IF NOT EXISTS webhook_delivery_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    subscription_id BIGINT NOT NULL,
    delivery_id VARCHAR(80) NOT NULL,
    event_type VARCHAR(128) NOT NULL,
    payload JSON NOT NULL,
    attempt INT NOT NULL DEFAULT 1,
    http_status INT NULL,
    response_body VARCHAR(1000) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    next_retry_at DATETIME NULL,
    error_message VARCHAR(1000) NULL,
    terminal_reason VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_webhook_delivery_attempt (tenant_id, delivery_id, attempt),
    INDEX idx_webhook_delivery_sub_status (tenant_id, subscription_id, status),
    INDEX idx_webhook_retry (status, next_retry_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Outbound webhook delivery attempts';
