package org.chovy.canvas.domain.warehouse;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.chovy.canvas.testsupport.MigrationTestSupport.readMigration;
import static org.assertj.core.api.Assertions.assertThat;

class CdpWarehouseRealtimeCheckpointSchemaTest {

    @Test
    void migrationCreatesRealtimeCheckpointTable() throws Exception {
        String sql = readMigration("cdp_warehouse_realtime_checkpoint");

        assertThat(sql)
                .contains("CREATE TABLE IF NOT EXISTS cdp_warehouse_realtime_checkpoint")
                .contains("UNIQUE KEY uk_cdp_warehouse_realtime_checkpoint")
                .contains("INDEX idx_cdp_warehouse_realtime_checkpoint_delivered")
                .contains("INDEX idx_cdp_warehouse_realtime_checkpoint_failure")
                .contains("last_event_log_id")
                .contains("last_delivered_at")
                .contains("last_failure_message");
    }
}
