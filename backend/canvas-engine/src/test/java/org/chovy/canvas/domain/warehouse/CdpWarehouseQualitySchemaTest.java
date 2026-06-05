package org.chovy.canvas.domain.warehouse;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.chovy.canvas.testsupport.MigrationTestSupport.readMigration;
import static org.assertj.core.api.Assertions.assertThat;

class CdpWarehouseQualitySchemaTest {

    @Test
    void migrationCreatesWarehouseQualityCheckLedger() throws Exception {
        String sql = readMigration("cdp_warehouse_quality_checks");

        assertThat(sql)
                .contains("CREATE TABLE IF NOT EXISTS cdp_warehouse_quality_check")
                .contains("INDEX idx_cdp_warehouse_quality_status")
                .contains("INDEX idx_cdp_warehouse_quality_type")
                .contains("INDEX idx_cdp_warehouse_quality_window");
    }
}
