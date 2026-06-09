package org.chovy.canvas.domain.marketing;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class MarketingIntegrationContractSchemaTest {

    @Test
    void migrationCreatesIntegrationContractRegistry() throws Exception {
        try (InputStream stream = Objects.requireNonNull(
                getClass().getClassLoader().getResourceAsStream(
                        "db/migration/V345__marketing_integration_contract_registry.sql"))) {
            String sql = new String(stream.readAllBytes(), StandardCharsets.UTF_8);

            assertThat(sql)
                    .contains("CREATE TABLE IF NOT EXISTS `marketing_integration_contract`")
                    .contains("`tenant_id` BIGINT NOT NULL")
                    .contains("`contract_key` VARCHAR(128) NOT NULL")
                    .contains("`provider_family` VARCHAR(64) NOT NULL")
                    .contains("`source_capability_key` VARCHAR(128) NOT NULL")
                    .contains("`target_capability_key` VARCHAR(128) NOT NULL")
                    .contains("`environment` VARCHAR(32) NOT NULL DEFAULT 'PRODUCTION'")
                    .contains("`auth_mode` VARCHAR(32) NOT NULL DEFAULT 'OAUTH'")
                    .contains("`retry_policy_json` JSON NULL")
                    .contains("`schema_contract_json` JSON NULL")
                    .contains("UNIQUE KEY `uk_marketing_integration_contract_key` (`tenant_id`, `contract_key`)")
                    .contains("KEY `idx_marketing_integration_contract_status`")
                    .contains("KEY `idx_marketing_integration_contract_provider`")
                    .contains("KEY `idx_marketing_integration_contract_asset`");
        }
    }

    @Test
    void migrationCreatesIntegrationContractProbeRuns() throws Exception {
        try (InputStream stream = Objects.requireNonNull(
                getClass().getClassLoader().getResourceAsStream(
                        "db/migration/V346__marketing_integration_contract_probe_runs.sql"))) {
            String sql = new String(stream.readAllBytes(), StandardCharsets.UTF_8);

            assertThat(sql)
                    .contains("CREATE TABLE IF NOT EXISTS `marketing_integration_contract_probe_run`")
                    .contains("`tenant_id` BIGINT NOT NULL")
                    .contains("`contract_id` BIGINT NOT NULL")
                    .contains("`contract_key` VARCHAR(128) NOT NULL")
                    .contains("`probe_key` VARCHAR(128) NOT NULL")
                    .contains("`status` VARCHAR(32) NOT NULL DEFAULT 'PASS'")
                    .contains("`http_status_code` INT NULL")
                    .contains("`latency_ms` BIGINT NULL")
                    .contains("`problem_type_uri` VARCHAR(512) NULL")
                    .contains("`observed_at` DATETIME NOT NULL")
                    .contains("`evidence_json` JSON NULL")
                    .contains("KEY `idx_marketing_integration_probe_contract`")
                    .contains("KEY `idx_marketing_integration_probe_status`")
                    .contains("CONSTRAINT `fk_marketing_integration_probe_contract`");
        }
    }

    @Test
    void migrationCreatesIntegrationContractAuditEvents() throws Exception {
        try (InputStream stream = Objects.requireNonNull(
                getClass().getClassLoader().getResourceAsStream(
                        "db/migration/V347__marketing_integration_contract_audit_events.sql"))) {
            String sql = new String(stream.readAllBytes(), StandardCharsets.UTF_8);

            assertThat(sql)
                    .contains("CREATE TABLE IF NOT EXISTS `marketing_integration_contract_audit_event`")
                    .contains("`tenant_id` BIGINT NOT NULL")
                    .contains("`contract_id` BIGINT NOT NULL")
                    .contains("`contract_key` VARCHAR(128) NOT NULL")
                    .contains("`revision` INT NOT NULL")
                    .contains("`event_type` VARCHAR(32) NOT NULL")
                    .contains("`snapshot_json` JSON NOT NULL")
                    .contains("`changed_fields_json` JSON NULL")
                    .contains("UNIQUE KEY `uk_marketing_integration_contract_revision`")
                    .contains("KEY `idx_marketing_integration_audit_contract`")
                    .contains("CONSTRAINT `fk_marketing_integration_audit_contract`");
        }
    }

    @Test
    void migrationAddsMonitoringAlertOpenDedupeKey() throws Exception {
        try (InputStream stream = Objects.requireNonNull(
                getClass().getClassLoader().getResourceAsStream(
                        "db/migration/V348__marketing_monitor_alert_open_dedupe_key.sql"))) {
            String sql = new String(stream.readAllBytes(), StandardCharsets.UTF_8);

            assertThat(sql)
                    .contains("ALTER TABLE `marketing_monitor_alert`")
                    .contains("ADD COLUMN `dedupe_key` VARCHAR(256) NULL")
                    .contains("UNIQUE KEY `uk_marketing_monitor_alert_open_dedupe` (`tenant_id`, `dedupe_key`)");
        }
    }

    @Test
    void migrationCreatesIntegrationContractProbeObservationHistory() throws Exception {
        try (InputStream stream = Objects.requireNonNull(
                getClass().getClassLoader().getResourceAsStream(
                        "db/migration/V349__marketing_integration_contract_probe_observations.sql"))) {
            String sql = new String(stream.readAllBytes(), StandardCharsets.UTF_8);

            assertThat(sql)
                    .contains("CREATE TABLE IF NOT EXISTS `marketing_integration_contract_probe_observation`")
                    .contains("`tenant_id` BIGINT NOT NULL")
                    .contains("`contract_id` BIGINT NOT NULL")
                    .contains("`probe_run_id` BIGINT NULL")
                    .contains("`probe_key` VARCHAR(128) NOT NULL")
                    .contains("`status` VARCHAR(32) NOT NULL")
                    .contains("`observed_at` DATETIME NOT NULL")
                    .contains("`evidence_json` JSON NULL")
                    .contains("KEY `idx_marketing_integration_probe_observation_window`")
                    .contains("KEY `idx_marketing_integration_probe_observation_status`")
                    .contains("CONSTRAINT `fk_marketing_integration_probe_observation_contract`");
        }
    }
}
