ALTER TABLE marketing_monitor_provider_credential
    ADD COLUMN revoke_endpoint VARCHAR(512) NULL AFTER refresh_endpoint,
    ADD COLUMN revoked_at DATETIME NULL AFTER refresh_token_expires_at,
    ADD COLUMN last_revoke_status VARCHAR(64) NULL AFTER last_refresh_error,
    ADD COLUMN last_revoke_error VARCHAR(1000) NULL AFTER last_revoke_status,
    ADD INDEX idx_monitor_provider_credential_due_refresh (tenant_id, status, expires_at, last_refresh_status);

ALTER TABLE marketing_monitor_provider_oauth_authorization
    ADD COLUMN revoke_endpoint VARCHAR(512) NULL AFTER token_endpoint;
