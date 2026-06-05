package org.chovy.canvas.domain.warehouse;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.chovy.canvas.testsupport.MigrationTestSupport.readMigration;
import static org.assertj.core.api.Assertions.assertThat;

class CdpWarehouseCatalogSchemaTest {

    @Test
    void migrationCreatesCatalogAndLineageTablesWithBuiltInSeeds() throws Exception {
        String sql = readMigration("cdp_warehouse_catalog_lineage");

        assertThat(sql)
                .contains("CREATE TABLE IF NOT EXISTS cdp_warehouse_dataset_catalog")
                .contains("CREATE TABLE IF NOT EXISTS cdp_warehouse_lineage_edge")
                .contains("UNIQUE KEY uk_cdp_warehouse_catalog_key")
                .contains("INDEX idx_cdp_warehouse_catalog_layer_status")
                .contains("UNIQUE KEY uk_cdp_warehouse_lineage_edge")
                .contains("INDEX idx_cdp_warehouse_lineage_upstream")
                .contains("INDEX idx_cdp_warehouse_lineage_downstream")
                .contains("'cdp_ods_event_log'")
                .contains("'cdp_dwd_user_event_fact'")
                .contains("'cdp_dws_user_event_metric_daily'")
                .contains("'canvas_daily_stats'")
                .contains("'CdpWarehouseAggregationService#dwdSql'")
                .contains("'CdpWarehouseAggregationService#dwsSql'");
    }
}
