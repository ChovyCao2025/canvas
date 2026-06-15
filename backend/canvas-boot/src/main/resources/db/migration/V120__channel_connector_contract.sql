CREATE TABLE IF NOT EXISTS channel_connector (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  connector_key VARCHAR(128) NOT NULL,
  channel VARCHAR(32) NOT NULL,
  provider VARCHAR(64) NOT NULL,
  mode VARCHAR(20) NOT NULL DEFAULT 'DISABLED',
  capabilities_json JSON NULL,
  health_status VARCHAR(20) NOT NULL DEFAULT 'UNKNOWN',
  health_message VARCHAR(500) NULL,
  disabled_reason VARCHAR(500) NULL,
  last_checked_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_channel_connector (tenant_id, connector_key),
  INDEX idx_channel_connector_lookup (tenant_id, channel, provider, mode)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO channel_connector
  (tenant_id, connector_key, channel, provider, mode, capabilities_json, health_status, health_message)
VALUES
  (0, 'sms-sandbox', 'SMS', 'SANDBOX', 'SANDBOX',
   JSON_OBJECT('send', true, 'receipt', false), 'UP', 'sandbox connector ready'),
  (0, 'email-sandbox', 'EMAIL', 'SANDBOX', 'SANDBOX',
   JSON_OBJECT('send', true, 'receipt', false), 'UP', 'sandbox connector ready'),
  (0, 'push-sandbox', 'PUSH', 'SANDBOX', 'SANDBOX',
   JSON_OBJECT('send', true, 'receipt', false), 'UP', 'sandbox connector ready');
