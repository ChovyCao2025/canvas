package org.chovy.canvas.domain.warehouse;

import org.junit.jupiter.api.Test;

import static org.chovy.canvas.testsupport.MigrationTestSupport.readMigration;
import static org.assertj.core.api.Assertions.assertThat;

class CdpWarehouseExternalRealtimeJobProbeSchemaTest {

    @Test
    void migrationCreatesExternalRealtimeJobProbeTargetTable() throws Exception {
        String sql = readMigration("cdp_warehouse_external_realtime_job_probe");

        assertThat(sql)
                .contains("CREATE TABLE IF NOT EXISTS cdp_warehouse_external_realtime_job_probe_target")
                .contains("pipeline_key VARCHAR(128) NOT NULL")
                .contains("job_key VARCHAR(128) NOT NULL")
                .contains("engine_type VARCHAR(64) NOT NULL")
                .contains("endpoint_url VARCHAR(512) NOT NULL")
                .contains("max_staleness_seconds INT NOT NULL DEFAULT 300")
                .contains("last_probe_status VARCHAR(32) DEFAULT NULL")
                .contains("UNIQUE KEY uk_cdp_warehouse_external_job_probe_target (tenant_id, pipeline_key, job_key)")
                .contains("INDEX idx_cdp_warehouse_external_job_probe_enabled");
    }
}
