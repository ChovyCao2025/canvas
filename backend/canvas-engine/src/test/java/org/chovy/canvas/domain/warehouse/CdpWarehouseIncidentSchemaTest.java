package org.chovy.canvas.domain.warehouse;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.chovy.canvas.testsupport.MigrationTestSupport.readMigration;
import static org.assertj.core.api.Assertions.assertThat;

class CdpWarehouseIncidentSchemaTest {

    @Test
    void migrationCreatesWarehouseIncidentTable() throws Exception {
        String sql = readMigration("cdp_warehouse_quality_incident");

        assertThat(sql)
                .contains("CREATE TABLE IF NOT EXISTS cdp_warehouse_incident")
                .contains("UNIQUE KEY uk_cdp_warehouse_incident_key")
                .contains("INDEX idx_cdp_warehouse_incident_status")
                .contains("INDEX idx_cdp_warehouse_incident_severity")
                .contains("acknowledged_at")
                .contains("resolved_at");
    }
}
