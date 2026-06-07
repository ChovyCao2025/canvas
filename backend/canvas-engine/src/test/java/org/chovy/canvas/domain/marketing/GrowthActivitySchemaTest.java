package org.chovy.canvas.domain.marketing;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class GrowthActivitySchemaTest {

    @Test
    void migrationCreatesGrowthActivityCenterCoreTables() throws Exception {
        try (InputStream stream = Objects.requireNonNull(
                getClass().getClassLoader().getResourceAsStream(
                        "db/migration/V351__growth_activity_center.sql"))) {
            String sql = new String(stream.readAllBytes(), StandardCharsets.UTF_8);

            assertThat(sql)
                    .contains("CREATE TABLE IF NOT EXISTS `growth_activity`")
                    .contains("`tenant_id` BIGINT NOT NULL")
                    .contains("`activity_key` VARCHAR(128) NOT NULL")
                    .contains("`activity_type` VARCHAR(64) NOT NULL")
                    .contains("`campaign_id` BIGINT DEFAULT NULL")
                    .contains("UNIQUE KEY `uk_growth_activity_key` (`tenant_id`, `activity_key`)")
                    .contains("KEY `idx_growth_activity_status` (`tenant_id`, `status`)")
                    .contains("CREATE TABLE IF NOT EXISTS `growth_activity_rule_set`")
                    .contains("CREATE TABLE IF NOT EXISTS `growth_reward_pool`")
                    .contains("CREATE TABLE IF NOT EXISTS `growth_budget_counter`")
                    .contains("CREATE TABLE IF NOT EXISTS `growth_activity_participant`")
                    .contains("CREATE TABLE IF NOT EXISTS `growth_reward_grant`")
                    .contains("UNIQUE KEY `uk_growth_reward_grant_idempotency` (`tenant_id`, `idempotency_key`)")
                    .contains("CREATE TABLE IF NOT EXISTS `growth_activity_event`")
                    .contains("`source_type` VARCHAR(64) NULL")
                    .contains("`source_id` BIGINT DEFAULT NULL")
                    .contains("`payload_json` JSON NULL")
                    .contains("`created_by` VARCHAR(128) NULL")
                    .contains("CREATE TABLE IF NOT EXISTS `growth_referral_code`")
                    .contains("UNIQUE KEY `uk_growth_referral_code` (`tenant_id`, `code`)")
                    .contains("CREATE TABLE IF NOT EXISTS `growth_referral_relation`")
                    .contains("`referral_code_id` BIGINT NOT NULL")
                    .contains("`risk_evidence_json` JSON NULL")
                    .contains("`inviter_reward_grant_id` BIGINT DEFAULT NULL")
                    .contains("`invitee_reward_grant_id` BIGINT DEFAULT NULL")
                    .contains("CREATE TABLE IF NOT EXISTS `growth_task_definition`")
                    .contains("`completion_policy` VARCHAR(32) NOT NULL DEFAULT 'EVENT'")
                    .contains("`reset_policy` VARCHAR(32) NOT NULL DEFAULT 'ONCE'")
                    .contains("`reward_pool_id` BIGINT DEFAULT NULL")
                    .contains("CREATE TABLE IF NOT EXISTS `growth_task_progress`")
                    .contains("`last_event_key` VARCHAR(191) NULL")
                    .contains("`evidence_json` JSON NULL")
                    .contains("`reward_grant_id` BIGINT DEFAULT NULL")
                    .contains("UNIQUE KEY `uk_growth_task_progress` (`tenant_id`, `activity_id`, `participant_id`, `task_id`)");
        }
    }
}
