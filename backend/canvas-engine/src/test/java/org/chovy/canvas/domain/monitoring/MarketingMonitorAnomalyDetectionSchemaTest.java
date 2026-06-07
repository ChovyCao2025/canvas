package org.chovy.canvas.domain.monitoring;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class MarketingMonitorAnomalyDetectionSchemaTest {

    @Test
    void migrationCreatesAnomalyRuleAndEventTablesWithUniqueIdentities() throws IOException {
        String sql = new String(getClass()
                .getResourceAsStream("/db/migration/V325__monitoring_anomaly_detection.sql")
                .readAllBytes(), StandardCharsets.UTF_8);

        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS `marketing_monitor_anomaly_rule`");
        assertThat(sql).contains("UNIQUE KEY `uk_monitor_anomaly_rule_key` (`tenant_id`, `rule_key`)");
        assertThat(sql).contains("`metric_key` VARCHAR(64) NOT NULL");
        assertThat(sql).contains("`direction` VARCHAR(16) NOT NULL DEFAULT 'BOTH'");
        assertThat(sql).contains("`baseline_window_buckets` INT NOT NULL DEFAULT 14");
        assertThat(sql).contains("`threshold_multiplier` DECIMAL(10,4) NOT NULL DEFAULT 3.0000");
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS `marketing_monitor_anomaly_event`");
        assertThat(sql).contains("UNIQUE KEY `uk_monitor_anomaly_event_bucket` (`tenant_id`, `rule_id`, `metric_key`, `source_id`, `brand_key`, `competitor_key`, `bucket_start`)");
        assertThat(sql).contains("`actual_value` DECIMAL(18,6) NOT NULL DEFAULT 0.000000");
        assertThat(sql).contains("`baseline_median` DECIMAL(18,6) NOT NULL DEFAULT 0.000000");
        assertThat(sql).contains("`baseline_mad` DECIMAL(18,6) NOT NULL DEFAULT 0.000000");
        assertThat(sql).contains("`robust_z_score` DECIMAL(18,6) NOT NULL DEFAULT 0.000000");
        assertThat(sql).contains("KEY `idx_monitor_anomaly_event_status` (`tenant_id`, `status`, `severity`, `bucket_start`)");
    }
}
