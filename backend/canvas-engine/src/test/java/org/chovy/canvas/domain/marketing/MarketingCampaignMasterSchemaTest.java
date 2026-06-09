package org.chovy.canvas.domain.marketing;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class MarketingCampaignMasterSchemaTest {

    @Test
    void migrationCreatesCampaignMasterAndResourceLinkLedgers() throws Exception {
        try (InputStream stream = Objects.requireNonNull(
                getClass().getClassLoader().getResourceAsStream(
                        "db/migration/V343__marketing_campaign_master_ledger.sql"))) {
            String sql = new String(stream.readAllBytes(), StandardCharsets.UTF_8);

            assertThat(sql)
                    .contains("CREATE TABLE IF NOT EXISTS `marketing_campaign_master`")
                    .contains("`tenant_id` BIGINT NOT NULL")
                    .contains("`campaign_key` VARCHAR(128) NOT NULL")
                    .contains("UNIQUE KEY `uk_marketing_campaign_master_key` (`tenant_id`, `campaign_key`)")
                    .contains("KEY `idx_marketing_campaign_master_status`")
                    .contains("CREATE TABLE IF NOT EXISTS `marketing_campaign_link`")
                    .contains("`campaign_id` BIGINT NOT NULL")
                    .contains("`resource_type` VARCHAR(64) NOT NULL")
                    .contains("`required_for_launch` TINYINT NOT NULL DEFAULT 0")
                    .contains("UNIQUE KEY `uk_marketing_campaign_link_resource`")
                    .contains("CONSTRAINT `fk_marketing_campaign_link_campaign`")
                    .contains("ON DELETE CASCADE");
        }
    }
}
