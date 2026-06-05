package org.chovy.canvas.domain.warehouse;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.chovy.canvas.testsupport.MigrationTestSupport.readMigration;

class CdpWarehouseE2eCertificationRunSchemaTest {

    @Test
    void migrationCreatesCertificationRunHistoryTable() throws Exception {
        String sql = readMigration("cdp_warehouse_e2e_certification_history");

        assertThat(sql)
                .contains("CREATE TABLE IF NOT EXISTS cdp_warehouse_e2e_certification_run")
                .contains("tenant_id BIGINT NOT NULL")
                .contains("status VARCHAR(16) NOT NULL")
                .contains("contract_keys_json TEXT NULL")
                .contains("evidence_json TEXT NULL")
                .contains("production_readiness_json MEDIUMTEXT NULL")
                .contains("live_table_inspection_json MEDIUMTEXT NULL")
                .contains("INDEX idx_cdp_warehouse_e2e_cert_tenant_status")
                .contains("INDEX idx_cdp_warehouse_e2e_cert_created");
    }
}
