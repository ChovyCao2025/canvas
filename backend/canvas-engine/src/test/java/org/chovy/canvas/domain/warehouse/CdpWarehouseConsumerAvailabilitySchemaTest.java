package org.chovy.canvas.domain.warehouse;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.chovy.canvas.testsupport.MigrationTestSupport.readMigration;
import static org.assertj.core.api.Assertions.assertThat;

class CdpWarehouseConsumerAvailabilitySchemaTest {

    @Test
    void migrationCreatesAssetAvailabilityAndConsumerContractTables() throws Exception {
        String sql = readMigration("cdp_warehouse_consumer_availability_contracts");

        assertThat(sql)
                .contains("CREATE TABLE IF NOT EXISTS cdp_warehouse_asset_availability")
                .contains("CREATE TABLE IF NOT EXISTS cdp_warehouse_consumer_availability_contract")
                .contains("UNIQUE KEY uk_cdp_warehouse_asset_availability_key")
                .contains("INDEX idx_cdp_warehouse_asset_availability_window")
                .contains("required_assets_json JSON NOT NULL")
                .contains("gate_policy VARCHAR(32) NOT NULL DEFAULT 'BLOCK_ON_WARN'")
                .contains("UNIQUE KEY uk_cdp_warehouse_consumer_availability_contract")
                .contains("INDEX idx_cdp_warehouse_consumer_availability_consumer")
                .contains("INDEX idx_cdp_warehouse_consumer_availability_dataset");
    }
}
