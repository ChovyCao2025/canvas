package org.chovy.canvas.domain.search;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class SearchMarketingSchemaTest {

    @Test
    void migrationCreatesSourceKeywordSnapshotAndOpportunityTables() throws IOException {
        String sql = new String(getClass()
                .getResourceAsStream("/db/migration/V322__search_marketing_foundation.sql")
                .readAllBytes(), StandardCharsets.UTF_8);

        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS `search_marketing_source`");
        assertThat(sql).contains("`channel` VARCHAR(16) NOT NULL");
        assertThat(sql).contains("UNIQUE KEY `uk_search_marketing_source_identity` (`tenant_id`, `provider`, `source_key`)");
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS `search_marketing_keyword`");
        assertThat(sql).contains("UNIQUE KEY `uk_search_marketing_keyword_identity` (`tenant_id`, `channel`, `keyword_key`, `match_type`, `landing_page_url_hash`)");
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS `search_marketing_snapshot`");
        assertThat(sql).contains("UNIQUE KEY `uk_search_marketing_snapshot_daily` (`tenant_id`, `source_id`, `keyword_id`, `snapshot_date`, `device`, `country`, `query_group_key`)");
        assertThat(sql).contains("`cost_amount` DECIMAL(18,4) NOT NULL DEFAULT 0.0000");
        assertThat(sql).contains("`average_position` DECIMAL(10,4) NULL");
        assertThat(sql).contains("KEY `idx_search_marketing_snapshot_scope` (`tenant_id`, `channel`, `snapshot_date`)");
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS `search_marketing_opportunity`");
        assertThat(sql).contains("UNIQUE KEY `uk_search_marketing_opportunity_identity` (`tenant_id`, `source_id`, `keyword_id`, `opportunity_type`, `snapshot_date`)");
        assertThat(sql).contains("KEY `idx_search_marketing_opportunity_status` (`tenant_id`, `status`, `severity`, `created_at`)");
        assertThat(sql).contains("`evidence_json` JSON NULL");
    }
}
