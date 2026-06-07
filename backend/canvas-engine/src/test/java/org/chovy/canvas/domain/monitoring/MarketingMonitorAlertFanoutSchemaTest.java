package org.chovy.canvas.domain.monitoring;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class MarketingMonitorAlertFanoutSchemaTest {

    @Test
    void migrationCreatesAlertChannelAndDeliveryTables() throws IOException {
        InputStream stream = getClass().getResourceAsStream(
                "/db/migration/V315__monitoring_alert_fanout.sql");
        assertThat(stream).as("V315 monitoring alert fanout migration").isNotNull();
        String sql = new String(stream.readAllBytes(), StandardCharsets.UTF_8);

        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS marketing_monitor_alert_channel");
        assertThat(sql).contains("channel_key VARCHAR(128) NOT NULL");
        assertThat(sql).contains("channel_type VARCHAR(64) NOT NULL");
        assertThat(sql).contains("secret_prefix VARCHAR(16) NULL");
        assertThat(sql).contains("secret_hash VARCHAR(120) NULL");
        assertThat(sql).contains("secret_ciphertext VARCHAR(1000) NULL");
        assertThat(sql).contains("UNIQUE KEY uk_marketing_monitor_alert_channel");
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS marketing_monitor_alert_delivery");
        assertThat(sql).contains("alert_id BIGINT NOT NULL");
        assertThat(sql).contains("delivery_id VARCHAR(64) NOT NULL");
        assertThat(sql).contains("request_payload JSON NULL");
        assertThat(sql).contains("idx_marketing_monitor_alert_delivery_status");
    }
}
