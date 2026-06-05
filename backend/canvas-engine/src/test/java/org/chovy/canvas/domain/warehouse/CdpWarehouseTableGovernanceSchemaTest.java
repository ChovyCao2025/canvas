package org.chovy.canvas.domain.warehouse;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.chovy.canvas.testsupport.MigrationTestSupport.readMigration;
import static org.assertj.core.api.Assertions.assertThat;

class CdpWarehouseTableGovernanceSchemaTest {

    @Test
    void migrationCreatesTableContractsAndInspectionLedgerWithBuiltInSeeds() throws Exception {
        String sql = readMigration("cdp_warehouse_table_contract");

        assertThat(sql)
                .contains("CREATE TABLE IF NOT EXISTS cdp_warehouse_table_contract")
                .contains("CREATE TABLE IF NOT EXISTS cdp_warehouse_table_inspection")
                .contains("UNIQUE KEY uk_cdp_warehouse_table_contract")
                .contains("INDEX idx_cdp_warehouse_table_contract_physical")
                .contains("INDEX idx_cdp_warehouse_table_inspection_table")
                .contains("'canvas_ods.cdp_event_log'")
                .contains("'canvas_dwd.cdp_user_event_fact'")
                .contains("'canvas_dws.user_event_metric_daily'")
                .contains("'canvas_ods.canvas_execution_trace'")
                .contains("'canvas_dws.canvas_daily_stats'")
                .contains("'canvas_dws.node_daily_stats'");
    }

    @Test
    void dorisCdpDdlHasProductionPhysicalGovernance() throws Exception {
        String sql = Files.readString(Path.of(
                "src/main/resources/infrastructure/doris/cdp-audience-ddl.sql"));

        assertThat(sql)
                .contains("PARTITION BY RANGE(event_time)")
                .contains("PARTITION BY RANGE(event_date)")
                .contains("PARTITION BY RANGE(stat_date)")
                .contains("\"dynamic_partition.enable\" = \"true\"")
                .contains("\"dynamic_partition.start\" = \"-90\"")
                .contains("\"dynamic_partition.start\" = \"-180\"")
                .contains("\"dynamic_partition.start\" = \"-730\"")
                .contains("\"replication_num\" = \"3\"")
                .contains("DISTRIBUTED BY HASH(tenant_id, event_code) BUCKETS 16")
                .contains("DISTRIBUTED BY HASH(tenant_id, user_id) BUCKETS 32");
    }

    @Test
    void dorisTraceDdlHasProductionPhysicalGovernance() throws Exception {
        String sql = Files.readString(Path.of(
                "src/main/resources/infrastructure/doris/trace-ddl.sql"));

        assertThat(sql)
                .contains("PARTITION BY RANGE(created_at)")
                .contains("PARTITION BY RANGE(stat_date)")
                .contains("\"dynamic_partition.enable\" = \"true\"")
                .contains("\"dynamic_partition.start\" = \"-90\"")
                .contains("\"dynamic_partition.start\" = \"-730\"")
                .contains("\"replication_num\" = \"3\"")
                .contains("DISTRIBUTED BY HASH(execution_id) BUCKETS 8")
                .contains("DISTRIBUTED BY HASH(canvas_id) BUCKETS 8");
    }
}
