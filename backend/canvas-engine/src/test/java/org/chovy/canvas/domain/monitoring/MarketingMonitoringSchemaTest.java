package org.chovy.canvas.domain.monitoring;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class MarketingMonitoringSchemaTest {

    @Test
    void migrationCreatesMonitoringSourcesItemsSentimentCompetitorsAndAlerts() throws IOException {
        String sql = new String(getClass().getResourceAsStream(
                "/db/migration/V311__sentiment_competitor_monitoring.sql").readAllBytes(),
                StandardCharsets.UTF_8);

        assertThat(sql).contains("marketing_monitor_source");
        assertThat(sql).contains("marketing_monitor_item");
        assertThat(sql).contains("marketing_sentiment_analysis");
        assertThat(sql).contains("marketing_competitor_mention");
        assertThat(sql).contains("marketing_monitor_alert");
        assertThat(sql).contains("uk_marketing_monitor_source");
        assertThat(sql).contains("uk_marketing_monitor_item_external");
        assertThat(sql).contains("idx_marketing_monitor_alert_status");
    }
}
