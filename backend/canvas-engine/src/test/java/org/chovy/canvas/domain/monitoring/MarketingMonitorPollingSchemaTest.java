package org.chovy.canvas.domain.monitoring;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class MarketingMonitorPollingSchemaTest {

    @Test
    void migrationAddsPollingStateRunLedgerAndTrendSnapshots() throws IOException {
        InputStream stream = getClass().getResourceAsStream(
                "/db/migration/V319__monitoring_polling_trends.sql");
        assertThat(stream).as("V319 monitoring polling and trends migration").isNotNull();
        String sql = new String(stream.readAllBytes(), StandardCharsets.UTF_8);

        assertThat(sql).contains("ALTER TABLE marketing_monitor_source");
        assertThat(sql).contains("poll_enabled TINYINT NOT NULL DEFAULT 0");
        assertThat(sql).contains("poll_interval_minutes INT NOT NULL DEFAULT 60");
        assertThat(sql).contains("poll_cursor VARCHAR(1000) NULL");
        assertThat(sql).contains("last_polled_at DATETIME NULL");
        assertThat(sql).contains("next_poll_at DATETIME NULL");
        assertThat(sql).contains("last_poll_status VARCHAR(32) NULL");
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS marketing_monitor_poll_run");
        assertThat(sql).contains("cursor_before VARCHAR(1000) NULL");
        assertThat(sql).contains("cursor_after VARCHAR(1000) NULL");
        assertThat(sql).contains("duplicate_count INT NOT NULL DEFAULT 0");
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS marketing_monitor_trend_snapshot");
        assertThat(sql).contains("avg_sentiment_score DECIMAL(8,5) NOT NULL DEFAULT 0.00000");
        assertThat(sql).contains("UNIQUE KEY uk_marketing_monitor_trend_snapshot");
        assertThat(sql).contains("idx_marketing_monitor_poll_run_source");
        assertThat(sql).contains("idx_marketing_monitor_trend_scope");
    }
}
