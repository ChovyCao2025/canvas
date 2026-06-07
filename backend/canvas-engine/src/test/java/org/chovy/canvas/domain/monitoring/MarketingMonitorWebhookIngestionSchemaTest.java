package org.chovy.canvas.domain.monitoring;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class MarketingMonitorWebhookIngestionSchemaTest {

    @Test
    void migrationAddsSourceScopedWebhookCredentialColumns() throws IOException {
        InputStream stream = getClass().getResourceAsStream(
                "/db/migration/V314__monitoring_webhook_ingestion.sql");
        assertThat(stream).as("V314 monitoring webhook ingestion migration").isNotNull();
        String sql = new String(stream.readAllBytes(),
                StandardCharsets.UTF_8);

        assertThat(sql).contains("ALTER TABLE marketing_monitor_source");
        assertThat(sql).contains("webhook_enabled TINYINT NOT NULL DEFAULT 0");
        assertThat(sql).contains("webhook_secret_prefix VARCHAR(16) NULL");
        assertThat(sql).contains("webhook_secret_hash VARCHAR(120) NULL");
        assertThat(sql).contains("webhook_secret_ciphertext VARCHAR(1000) NULL");
        assertThat(sql).contains("webhook_signature_tolerance_seconds INT NOT NULL DEFAULT 300");
        assertThat(sql).contains("idx_marketing_monitor_source_webhook");
    }
}
