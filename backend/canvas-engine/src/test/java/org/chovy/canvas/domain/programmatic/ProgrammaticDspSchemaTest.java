package org.chovy.canvas.domain.programmatic;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class ProgrammaticDspSchemaTest {

    @Test
    void migrationCreatesSeatCampaignLineItemSupplyPathAndSnapshotTables() throws IOException {
        String sql = new String(getClass()
                .getResourceAsStream("/db/migration/V324__programmatic_dsp_foundation.sql")
                .readAllBytes(), StandardCharsets.UTF_8);

        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS `programmatic_dsp_seat`");
        assertThat(sql).contains("UNIQUE KEY `uk_programmatic_dsp_seat_identity` (`tenant_id`, `provider`, `seat_key`)");
        assertThat(sql).contains("`supply_chain_enforcement` VARCHAR(32) NOT NULL DEFAULT 'MONITOR'");
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS `programmatic_dsp_campaign`");
        assertThat(sql).contains("UNIQUE KEY `uk_programmatic_dsp_campaign_key` (`tenant_id`, `campaign_key`)");
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS `programmatic_dsp_line_item`");
        assertThat(sql).contains("UNIQUE KEY `uk_programmatic_dsp_line_item_key` (`tenant_id`, `campaign_id`, `line_item_key`)");
        assertThat(sql).contains("`max_bid_cpm` DECIMAL(18,4) NOT NULL DEFAULT 0.0000");
        assertThat(sql).contains("`targeting_json` JSON NULL");
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS `programmatic_dsp_supply_path`");
        assertThat(sql).contains("UNIQUE KEY `uk_programmatic_dsp_supply_path_identity` (`tenant_id`, `line_item_id`, `exchange_key`, `deal_id`, `seller_id`)");
        assertThat(sql).contains("`schain_complete` TINYINT NOT NULL DEFAULT 0");
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS `programmatic_dsp_performance_snapshot`");
        assertThat(sql).contains("UNIQUE KEY `uk_programmatic_dsp_snapshot_daily` (`tenant_id`, `seat_id`, `campaign_id`, `line_item_id`, `snapshot_date`)");
        assertThat(sql).contains("`bid_count` BIGINT NOT NULL DEFAULT 0");
        assertThat(sql).contains("`win_count` BIGINT NOT NULL DEFAULT 0");
        assertThat(sql).contains("`spend_amount` DECIMAL(18,4) NOT NULL DEFAULT 0.0000");
        assertThat(sql).contains("KEY `idx_programmatic_dsp_snapshot_scope` (`tenant_id`, `snapshot_date`, `seat_id`, `campaign_id`)");
    }
}
