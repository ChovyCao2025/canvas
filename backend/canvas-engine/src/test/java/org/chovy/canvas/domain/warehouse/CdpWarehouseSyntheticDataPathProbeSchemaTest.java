package org.chovy.canvas.domain.warehouse;

import org.junit.jupiter.api.Test;

import static org.chovy.canvas.testsupport.MigrationTestSupport.readMigration;
import static org.assertj.core.api.Assertions.assertThat;

class CdpWarehouseSyntheticDataPathProbeSchemaTest {

    @Test
    void migrationCreatesSyntheticDataPathProbeRunTable() throws Exception {
        String sql = readMigration("cdp_warehouse_synthetic_ods_data_path_probe");

        assertThat(sql)
                .contains("CREATE TABLE IF NOT EXISTS cdp_warehouse_synthetic_data_path_probe_run")
                .contains("probe_key VARCHAR(128) NOT NULL")
                .contains("message_id VARCHAR(128) NOT NULL")
                .contains("event_code VARCHAR(128) NOT NULL")
                .contains("strict_mode TINYINT(1) NOT NULL DEFAULT 1")
                .contains("sink_status VARCHAR(32) DEFAULT NULL")
                .contains("ods_status VARCHAR(32) DEFAULT NULL")
                .contains("ods_row_count BIGINT NOT NULL DEFAULT 0")
                .contains("UNIQUE KEY uk_cdp_warehouse_synthetic_probe_message (tenant_id, message_id)");
    }
}
