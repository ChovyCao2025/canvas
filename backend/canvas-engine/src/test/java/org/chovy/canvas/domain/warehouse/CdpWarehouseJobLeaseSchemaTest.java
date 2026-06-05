package org.chovy.canvas.domain.warehouse;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.chovy.canvas.testsupport.MigrationTestSupport.readMigration;
import static org.assertj.core.api.Assertions.assertThat;

class CdpWarehouseJobLeaseSchemaTest {

    @Test
    void migrationCreatesWarehouseSchedulerLeaseTable() throws Exception {
        String sql = readMigration("cdp_warehouse_scheduler_lease");

        assertThat(sql)
                .contains("CREATE TABLE IF NOT EXISTS cdp_warehouse_job_lease")
                .contains("UNIQUE KEY uk_cdp_warehouse_job_lease")
                .contains("INDEX idx_cdp_warehouse_job_lease_until")
                .contains("tenant_id")
                .contains("lease_key")
                .contains("lease_until");
    }
}
