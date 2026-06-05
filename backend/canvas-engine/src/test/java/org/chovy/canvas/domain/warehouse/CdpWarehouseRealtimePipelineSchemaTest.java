package org.chovy.canvas.domain.warehouse;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.chovy.canvas.testsupport.MigrationTestSupport.readMigration;
import static org.assertj.core.api.Assertions.assertThat;

class CdpWarehouseRealtimePipelineSchemaTest {

    @Test
    void migrationCreatesRealtimePipelineContractsAndCheckpointEvidence() throws Exception {
        String sql = readMigration("cdp_warehouse_realtime_pipeline_runtime");

        assertThat(sql)
                .contains("CREATE TABLE IF NOT EXISTS cdp_warehouse_stream_pipeline")
                .contains("CREATE TABLE IF NOT EXISTS cdp_warehouse_stream_checkpoint")
                .contains("UNIQUE KEY uk_cdp_warehouse_stream_pipeline")
                .contains("UNIQUE KEY uk_cdp_warehouse_stream_checkpoint")
                .contains("INDEX idx_cdp_warehouse_stream_pipeline_status")
                .contains("INDEX idx_cdp_warehouse_stream_checkpoint_time")
                .contains("'mysql_cdp_event_log_to_doris_ods'")
                .contains("'mysql_canvas_trace_to_doris_ods'")
                .contains("'doris_ods_cdp_event_to_dwd_fact'")
                .contains("'doris_dwd_user_fact_to_dws_metric_daily'")
                .contains("'EXACTLY_ONCE'")
                .contains("'FLINK_CDC'")
                .contains("'FLINK_SQL'");
    }
}
