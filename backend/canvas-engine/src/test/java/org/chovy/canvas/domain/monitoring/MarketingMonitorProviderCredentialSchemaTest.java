package org.chovy.canvas.domain.monitoring;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class MarketingMonitorProviderCredentialSchemaTest {

    @Test
    void migrationCreatesProviderCredentialAndLifecycleEventLedgers() throws IOException {
        InputStream migration = getClass().getResourceAsStream(
                "/db/migration/V330__monitoring_provider_credential_lifecycle.sql");
        assertThat(migration).isNotNull();
        String sql = new String(migration.readAllBytes(), StandardCharsets.UTF_8);

        assertThat(sql).contains("marketing_monitor_provider_credential");
        assertThat(sql).contains("credential_key VARCHAR(128) NOT NULL");
        assertThat(sql).contains("access_token_ciphertext VARCHAR(2000) NULL");
        assertThat(sql).contains("refresh_token_ciphertext VARCHAR(2000) NULL");
        assertThat(sql).contains("api_key_ciphertext VARCHAR(2000) NULL");
        assertThat(sql).contains("client_id_ciphertext VARCHAR(1000) NULL");
        assertThat(sql).contains("client_secret_ciphertext VARCHAR(2000) NULL");
        assertThat(sql).contains("refresh_endpoint VARCHAR(512) NULL");
        assertThat(sql).contains("expires_at DATETIME NULL");
        assertThat(sql).contains("last_refreshed_at DATETIME NULL");
        assertThat(sql).contains("uk_monitor_provider_credential");
        assertThat(sql).contains("idx_monitor_provider_credential_status");
        assertThat(sql).contains("marketing_monitor_provider_credential_event");
        assertThat(sql).contains("event_type VARCHAR(64) NOT NULL");
        assertThat(sql).contains("idx_monitor_provider_credential_event");
    }

    @Test
    void migrationAddsProviderCredentialRefreshAndRevocationOperations() throws IOException {
        InputStream migration = getClass().getResourceAsStream(
                "/db/migration/V337__monitoring_provider_oauth_refresh_revocation.sql");
        assertThat(migration).isNotNull();
        String sql = new String(migration.readAllBytes(), StandardCharsets.UTF_8);

        assertThat(sql).contains("ALTER TABLE marketing_monitor_provider_credential");
        assertThat(sql).contains("ADD COLUMN revoke_endpoint VARCHAR(512) NULL");
        assertThat(sql).contains("ADD COLUMN revoked_at DATETIME NULL");
        assertThat(sql).contains("ADD COLUMN last_revoke_status VARCHAR(64) NULL");
        assertThat(sql).contains("ADD COLUMN last_revoke_error VARCHAR(1000) NULL");
        assertThat(sql).contains("idx_monitor_provider_credential_due_refresh");
    }
}
