package org.chovy.canvas.domain.warehouse;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.chovy.canvas.testsupport.MigrationTestSupport.readMigration;
import static org.assertj.core.api.Assertions.assertThat;

class CdpWarehouseRealtimeRetrySchemaTest {

    @Test
    void migrationCreatesRealtimeRetryLedger() throws Exception {
        String sql = readMigration("cdp_warehouse_realtime_retry");

        assertThat(sql)
                .contains("CREATE TABLE IF NOT EXISTS cdp_warehouse_realtime_retry")
                .contains("UNIQUE KEY uk_cdp_warehouse_retry_event")
                .contains("INDEX idx_cdp_warehouse_retry_due")
                .contains("INDEX idx_cdp_warehouse_retry_status");
    }
}
