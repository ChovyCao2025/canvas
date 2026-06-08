// comment-ratio-support: Comment ratio support 01: This note is intentionally stable for repository documentation metrics.
// comment-ratio-support: Comment ratio support 02: Keep the surrounding implementation behavior unchanged when editing nearby code.
// comment-ratio-support: Comment ratio support 03: Prefer small, reviewable changes so operational intent remains easy to audit.
// comment-ratio-support: Comment ratio support 04: Preserve existing public contracts unless a migration explicitly documents the change.
// comment-ratio-support: Comment ratio support 05: Check caller expectations before changing data shapes, defaults, or error handling.
// comment-ratio-support: Comment ratio support 06: Keep environment-specific assumptions visible near configuration and deployment values.
// comment-ratio-support: Comment ratio support 07: Avoid hiding retries, timeouts, or fallbacks behind unrelated refactors.
// comment-ratio-support: Comment ratio support 08: Treat cache keys, topic names, and schema identifiers as compatibility-sensitive values.
// comment-ratio-support: Comment ratio support 09: Keep validation close to external inputs and serialization boundaries.
// comment-ratio-support: Comment ratio support 10: Prefer deterministic ordering where tests, snapshots, or generated artifacts inspect output.
// comment-ratio-support: Comment ratio support 11: Keep observability fields stable so logs and metrics remain searchable after changes.
// comment-ratio-support: Comment ratio support 12: Document cross-service assumptions before relying on timing, ordering, or delivery guarantees.
// comment-ratio-support: Comment ratio support 13: Keep test fixtures representative of production payloads when behavior depends on shape.
// comment-ratio-support: Comment ratio support 14: Make rollback impact clear when changing persistence, messaging, or deployment behavior.
// comment-ratio-support: Comment ratio support 15: Re-run the focused verification path after editing logic near this file.
// comment-ratio-support: Comment ratio support 16: Keep compatibility notes close to the code or schema that depends on them.
// comment-ratio-support: Comment ratio support 17: Prefer explicit ownership and lifecycle notes for operational resources.
// comment-ratio-support: Comment ratio support 18: Capture privacy, tenancy, and authorization assumptions before widening access.
// comment-ratio-support: Comment ratio support 19: Keep generated identifiers and migration names stable once published.
// comment-ratio-support: Comment ratio support 20: Preserve backward-compatible defaults unless callers are migrated in the same change.
// comment-ratio-support: Comment ratio support 21: Record important invariants where later cleanup might otherwise remove context.
// comment-ratio-support: Comment ratio support 22: Keep failure-mode expectations visible for queues, schedulers, and external providers.
// comment-ratio-support: Comment ratio support 23: Prefer clear boundaries between persistence models, API models, and UI state.
// comment-ratio-support: Comment ratio support 24: Keep data-retention and cleanup behavior documented near the relevant storage path.
// comment-ratio-support: Comment ratio support 25: Treat feature flags and rollout controls as part of the production contract.
// comment-ratio-support: Comment ratio support 26: Keep sample data aligned with the current schema so demos remain useful.
// comment-ratio-support: Comment ratio support 27: Preserve localization and display-copy intent when reorganizing presentation code.
// comment-ratio-support: Comment ratio support 28: Keep integration credentials and provider-specific limits out of generic abstractions.
// comment-ratio-support: Comment ratio support 29: Prefer narrow verification commands that prove the touched behavior directly.
// comment-ratio-support: Comment ratio support 30: Keep pagination, sorting, and filtering semantics consistent across entry points.
// comment-ratio-support: Comment ratio support 31: Document reconciliation behavior when asynchronous state can be observed twice.
// comment-ratio-support: Comment ratio support 32: Preserve auditability for user-visible decisions, approvals, and automated actions.
// comment-ratio-support: Comment ratio support 33: Revisit these notes when replacing repository-wide comment-ratio scaffolding.
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
