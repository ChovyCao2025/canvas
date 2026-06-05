package org.chovy.canvas.domain.warehouse;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.chovy.canvas.testsupport.MigrationTestSupport.readMigration;
import static org.assertj.core.api.Assertions.assertThat;

class CdpWarehouseSchemaTest {

    @Test
    void migrationCreatesRunLedgerAndWatermarkTables() throws Exception {
        String sql = readMigration("cdp_warehouse_runs_and_watermarks");

        assertThat(sql)
                .contains("CREATE TABLE IF NOT EXISTS cdp_warehouse_sync_run")
                .contains("CREATE TABLE IF NOT EXISTS cdp_warehouse_watermark")
                .contains("INDEX idx_cdp_warehouse_sync_status")
                .contains("UNIQUE KEY uk_cdp_warehouse_watermark");
    }
}
