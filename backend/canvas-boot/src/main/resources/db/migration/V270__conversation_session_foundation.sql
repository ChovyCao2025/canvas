CREATE TABLE IF NOT EXISTS conversation_session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT 'Tenant id',
    canvas_id BIGINT NULL COMMENT 'Canvas id when the conversation is tied to a canvas',
    version_id BIGINT NULL COMMENT 'Canvas version id when available',
    execution_id VARCHAR(128) NULL COMMENT 'Canvas execution id when available',
    user_id VARCHAR(128) NOT NULL COMMENT 'Conversation user id',
    channel VARCHAR(32) NOT NULL COMMENT 'Conversation channel',
    provider VARCHAR(64) NOT NULL DEFAULT 'DEFAULT' COMMENT 'Channel provider',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/COMPLETED/EXPIRED/TRANSFERRED',
    turn_count INT NOT NULL DEFAULT 0 COMMENT 'Inbound turn count',
    context_json JSON NULL COMMENT 'Accumulated conversation context',
    last_message_at DATETIME NOT NULL COMMENT 'Last message time',
    expires_at DATETIME NULL COMMENT 'Conversation expiry time',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_conversation_session_active (tenant_id, user_id, channel, provider, status, last_message_at),
    INDEX idx_conversation_session_canvas (tenant_id, canvas_id, version_id),
    INDEX idx_conversation_session_execution (tenant_id, execution_id),
    INDEX idx_conversation_session_recent (tenant_id, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Conversation sessions';

CREATE TABLE IF NOT EXISTS conversation_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT 'Tenant id',
    session_id BIGINT NOT NULL COMMENT 'Conversation session id',
    direction VARCHAR(12) NOT NULL COMMENT 'INBOUND/OUTBOUND',
    message_type VARCHAR(20) NOT NULL DEFAULT 'UNKNOWN' COMMENT 'TEXT/IMAGE/INTERACTIVE/UNKNOWN',
    external_message_id VARCHAR(128) NULL COMMENT 'Provider message id',
    idempotency_key VARCHAR(256) NOT NULL COMMENT 'Tenant-scoped inbound idempotency key',
    content_json JSON NOT NULL COMMENT 'Raw normalized message content',
    text_content TEXT NULL COMMENT 'Plain text content when available',
    intent VARCHAR(64) NULL COMMENT 'Recognized or supplied reply intent',
    processed TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Whether downstream processing consumed this message',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_conversation_message_idempotency (tenant_id, idempotency_key),
    INDEX idx_conversation_message_session (tenant_id, session_id, created_at),
    INDEX idx_conversation_message_external (tenant_id, external_message_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Conversation messages';
