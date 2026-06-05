package org.chovy.canvas.domain.warehouse;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.chovy.canvas.testsupport.MigrationTestSupport.readMigration;

class CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunSchemaTest {

    @Test
    void migrationCreatesAutomationRunHistoryTable() throws Exception {
        String sql = readMigration("privacy_audience_rebuild_automation_run");

        assertThat(sql)
                .contains("CREATE TABLE IF NOT EXISTS cdp_warehouse_privacy_audience_rebuild_automation_run")
                .contains("tenant_id BIGINT NOT NULL")
                .contains("trigger_source VARCHAR(32) NOT NULL")
                .contains("status VARCHAR(16) NOT NULL")
                .contains("result_json MEDIUMTEXT NULL")
                .contains("INDEX idx_cdp_warehouse_privacy_audience_rebuild_run_status")
                .contains("INDEX idx_cdp_warehouse_privacy_audience_rebuild_run_source")
                .contains("INDEX idx_cdp_warehouse_privacy_audience_rebuild_run_started");
    }
}
