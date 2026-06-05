package org.chovy.canvas.domain.warehouse;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.chovy.canvas.testsupport.MigrationTestSupport.readMigration;
import static org.assertj.core.api.Assertions.assertThat;

class CdpWarehouseMetricChangeReviewSchemaTest {

    @Test
    void migrationCreatesMetricChangeReviewTableAndIndexes() throws Exception {
        String sql = readMigration("cdp_warehouse_metric_change_review");

        assertThat(sql)
                .contains("CREATE TABLE IF NOT EXISTS cdp_warehouse_metric_change_review")
                .contains("current_snapshot_json JSON NOT NULL")
                .contains("proposed_snapshot_json JSON NOT NULL")
                .contains("impact_summary_json JSON NOT NULL")
                .contains("status VARCHAR(32) NOT NULL DEFAULT 'PENDING_REVIEW'")
                .contains("INDEX idx_cdp_warehouse_metric_review_target")
                .contains("INDEX idx_cdp_warehouse_metric_review_status")
                .contains("INDEX idx_cdp_warehouse_metric_review_requester");
    }
}
