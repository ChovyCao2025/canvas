package org.chovy.canvas.domain.warehouse;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.chovy.canvas.testsupport.MigrationTestSupport.readMigration;
import static org.assertj.core.api.Assertions.assertThat;

class CdpWarehouseRealtimeJobControlSchemaTest {

    @Test
    void migrationCreatesRealtimeJobInstanceAndActionAuditTables() throws Exception {
        String sql = readMigration("cdp_warehouse_realtime_job_control");

        assertThat(sql)
                .contains("CREATE TABLE IF NOT EXISTS cdp_warehouse_stream_job_instance")
                .contains("UNIQUE KEY uk_cdp_warehouse_stream_job_instance")
                .contains("runtime_status VARCHAR(32) NOT NULL DEFAULT 'UNKNOWN'")
                .contains("desired_status VARCHAR(32) NOT NULL DEFAULT 'RUNNING'")
                .contains("last_heartbeat_at DATETIME DEFAULT NULL")
                .contains("CREATE TABLE IF NOT EXISTS cdp_warehouse_stream_job_action")
                .contains("action VARCHAR(32) NOT NULL")
                .contains("status VARCHAR(32) NOT NULL DEFAULT 'PENDING'")
                .contains("acknowledged_at DATETIME DEFAULT NULL")
                .contains("completed_at DATETIME DEFAULT NULL");
    }
}
