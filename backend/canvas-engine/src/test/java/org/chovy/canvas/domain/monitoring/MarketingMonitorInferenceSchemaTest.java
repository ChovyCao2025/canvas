package org.chovy.canvas.domain.monitoring;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class MarketingMonitorInferenceSchemaTest {

    @Test
    void migrationCreatesGovernedMonitoringInferenceLedger() throws IOException {
        InputStream migration = getClass().getResourceAsStream(
                "/db/migration/V328__monitoring_llm_sentiment_inference.sql");
        assertThat(migration).isNotNull();
        String sql = new String(migration.readAllBytes(),
                StandardCharsets.UTF_8);

        assertThat(sql).contains("marketing_monitor_inference");
        assertThat(sql).contains("item_id BIGINT NOT NULL");
        assertThat(sql).contains("input_hash VARCHAR(64) NOT NULL");
        assertThat(sql).contains("prompt_hash VARCHAR(64) NOT NULL");
        assertThat(sql).contains("sentiment_label VARCHAR(32) NOT NULL");
        assertThat(sql).contains("entities_json JSON NULL");
        assertThat(sql).contains("topics_json JSON NULL");
        assertThat(sql).contains("risk_flags_json JSON NULL");
        assertThat(sql).contains("evidence_json JSON NULL");
        assertThat(sql).contains("fallback_used TINYINT(1) NOT NULL DEFAULT 0");
        assertThat(sql).contains("idx_monitor_inference_item");
        assertThat(sql).contains("idx_monitor_inference_status");
        assertThat(sql).contains("idx_monitor_inference_model");
        assertThat(sql).contains("idx_monitor_inference_hash");
    }
}
