package org.chovy.canvas.domain.monitoring;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class MarketingMonitorProviderOAuthAuthorizationSchemaTest {

    @Test
    void migrationCreatesOAuthAuthorizationAndEventLedgers() throws IOException {
        InputStream migration = getClass().getResourceAsStream(
                "/db/migration/V331__monitoring_provider_oauth_authorization.sql");
        assertThat(migration).isNotNull();
        String sql = new String(migration.readAllBytes(), StandardCharsets.UTF_8);

        assertThat(sql).contains("marketing_monitor_provider_oauth_authorization");
        assertThat(sql).contains("auth_state VARCHAR(128) NOT NULL");
        assertThat(sql).contains("credential_key VARCHAR(128) NOT NULL");
        assertThat(sql).contains("authorize_endpoint VARCHAR(512) NOT NULL");
        assertThat(sql).contains("token_endpoint VARCHAR(512) NOT NULL");
        assertThat(sql).contains("client_id_ciphertext VARCHAR(1000) NOT NULL");
        assertThat(sql).contains("client_secret_ciphertext VARCHAR(2000) NULL");
        assertThat(sql).contains("code_verifier_ciphertext VARCHAR(1000) NOT NULL");
        assertThat(sql).contains("code_challenge VARCHAR(256) NOT NULL");
        assertThat(sql).contains("status VARCHAR(64) NOT NULL");
        assertThat(sql).contains("expires_at DATETIME NOT NULL");
        assertThat(sql).contains("credential_id BIGINT NULL");
        assertThat(sql).contains("uk_monitor_oauth_authorization_state");
        assertThat(sql).contains("idx_monitor_oauth_authorization_status");
        assertThat(sql).contains("marketing_monitor_provider_oauth_authorization_event");
        assertThat(sql).contains("event_type VARCHAR(64) NOT NULL");
        assertThat(sql).contains("idx_monitor_oauth_authorization_event");
    }
}
