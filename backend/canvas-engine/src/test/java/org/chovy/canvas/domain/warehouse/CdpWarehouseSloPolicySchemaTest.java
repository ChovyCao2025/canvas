package org.chovy.canvas.domain.warehouse;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.chovy.canvas.testsupport.MigrationTestSupport.readMigration;
import static org.assertj.core.api.Assertions.assertThat;

class CdpWarehouseSloPolicySchemaTest {

    @Test
    void migrationCreatesWarehouseSloPolicyTableAndDefaultSeed() throws Exception {
        String sql = readMigration("cdp_warehouse_slo_policy");

        assertThat(sql)
                .contains("CREATE TABLE IF NOT EXISTS cdp_warehouse_slo_policy")
                .contains("offline_warn_run_gap_minutes INT NOT NULL")
                .contains("offline_fail_watermark_lag_minutes INT NOT NULL")
                .contains("audience_warn_run_gap_minutes INT NOT NULL")
                .contains("UNIQUE KEY uk_cdp_warehouse_slo_policy_key (tenant_id, policy_key)")
                .contains("'WAREHOUSE_READINESS_DEFAULT'");
    }
}
