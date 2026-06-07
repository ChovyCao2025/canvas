package org.chovy.canvas.domain.creator;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class CreatorCollaborationSchemaTest {

    @Test
    void migrationCreatesCreatorCampaignCollaborationAndDeliverableTables() throws IOException {
        String sql = new String(getClass()
                .getResourceAsStream("/db/migration/V321__creator_collaboration_foundation.sql")
                .readAllBytes(), StandardCharsets.UTF_8);

        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS `creator_profile`");
        assertThat(sql).contains("UNIQUE KEY `uk_creator_profile_identity` (`tenant_id`, `provider`, `handle_key`)");
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS `creator_campaign`");
        assertThat(sql).contains("UNIQUE KEY `uk_creator_campaign_key` (`tenant_id`, `campaign_key`)");
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS `creator_collaboration`");
        assertThat(sql).contains("UNIQUE KEY `uk_creator_collaboration_creator_campaign` (`tenant_id`, `campaign_id`, `creator_id`)");
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS `creator_deliverable`");
        assertThat(sql).contains("`tracking_link` VARCHAR(512) NULL");
        assertThat(sql).contains("`discount_code` VARCHAR(128) NULL");
        assertThat(sql).contains("`commission_rate` DECIMAL(9,6) NOT NULL DEFAULT 0.000000");
        assertThat(sql).contains("`impression_count` BIGINT NOT NULL DEFAULT 0");
        assertThat(sql).contains("`conversion_count` BIGINT NOT NULL DEFAULT 0");
        assertThat(sql).contains("`revenue_amount` DECIMAL(18,4) NOT NULL DEFAULT 0.0000");
        assertThat(sql).contains("KEY `idx_creator_deliverable_due` (`tenant_id`, `due_at`, `status`)");
    }
}
