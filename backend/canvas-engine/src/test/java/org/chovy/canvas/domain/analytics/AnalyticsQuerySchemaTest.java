package org.chovy.canvas.domain.analytics;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class AnalyticsQuerySchemaTest {

    @Test
    void migrationCreatesAnalyticsEventAndTraceTables() throws Exception {
        String sql = Files.readString(Path.of(
                "src/main/resources/db/migration/V132__analytics_event_trace_schema_and_sink.sql"));

        assertThat(sql)
                .contains("CREATE TABLE IF NOT EXISTS analytics_event")
                .contains("tenant_id BIGINT NOT NULL DEFAULT 0")
                .contains("event_code VARCHAR(128) NOT NULL")
                .contains("attributes_json JSON NULL")
                .contains("INDEX idx_analytics_event_tenant_time")
                .contains("CREATE TABLE IF NOT EXISTS analytics_event_trace")
                .contains("INDEX idx_analytics_trace_execution");
    }

    @Test
    void migrationCreatesRetentionPolicyTables() throws Exception {
        String sql = Files.readString(Path.of(
                "src/main/resources/db/migration/V133__analytics_retention_policy.sql"));

        assertThat(sql)
                .contains("CREATE TABLE IF NOT EXISTS analytics_retention_policy")
                .contains("UNIQUE KEY uk_analytics_retention_policy")
                .contains("CREATE TABLE IF NOT EXISTS analytics_retention_run")
                .contains("INDEX idx_analytics_retention_run");
    }

    @Test
    void migrationCreatesQueryDefinitionTables() throws Exception {
        String sql = Files.readString(Path.of(
                "src/main/resources/db/migration/V134__analytics_query_definitions.sql"));

        assertThat(sql)
                .contains("CREATE TABLE IF NOT EXISTS analytics_funnel_definition")
                .contains("CREATE TABLE IF NOT EXISTS analytics_alert_rule")
                .contains("CREATE TABLE IF NOT EXISTS analytics_export_job")
                .contains("row_limit INT NOT NULL");
    }
}
