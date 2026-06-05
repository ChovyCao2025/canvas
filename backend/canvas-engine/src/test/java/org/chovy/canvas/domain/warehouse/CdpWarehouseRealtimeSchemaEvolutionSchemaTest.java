package org.chovy.canvas.domain.warehouse;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.chovy.canvas.testsupport.MigrationTestSupport.readMigration;
import static org.assertj.core.api.Assertions.assertThat;

class CdpWarehouseRealtimeSchemaEvolutionSchemaTest {

    @Test
    void migrationCreatesRealtimeSchemaRegistryAndCheckpointSchemaColumns() throws Exception {
        String sql = readMigration("cdp_warehouse_realtime_schema_evolution");

        assertThat(sql)
                .contains("CREATE TABLE IF NOT EXISTS cdp_warehouse_stream_schema")
                .contains("schema_role VARCHAR(16) NOT NULL")
                .contains("schema_version VARCHAR(64) NOT NULL")
                .contains("schema_hash VARCHAR(128) NOT NULL")
                .contains("compatibility_status VARCHAR(32) NOT NULL DEFAULT 'COMPATIBLE'")
                .contains("UNIQUE KEY uk_cdp_warehouse_stream_schema_version")
                .contains("ALTER TABLE cdp_warehouse_stream_checkpoint")
                .contains("ADD COLUMN source_schema_version VARCHAR(64) DEFAULT NULL")
                .contains("ADD COLUMN sink_schema_version VARCHAR(64) DEFAULT NULL")
                .contains("ADD COLUMN schema_status VARCHAR(32) DEFAULT NULL");
    }
}
