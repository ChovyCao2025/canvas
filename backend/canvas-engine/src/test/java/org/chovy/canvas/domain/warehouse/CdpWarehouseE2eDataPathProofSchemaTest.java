package org.chovy.canvas.domain.warehouse;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.chovy.canvas.testsupport.MigrationTestSupport.readMigration;

class CdpWarehouseE2eDataPathProofSchemaTest {

    @Test
    void migrationAddsDataPathProofFieldsToCertificationRunHistory() throws Exception {
        String sql = readMigration("cdp_warehouse_e2e_data_path_proof");

        assertThat(sql)
                .contains("ALTER TABLE cdp_warehouse_e2e_certification_run")
                .contains("ADD COLUMN require_data_path_proof TINYINT NOT NULL DEFAULT 0")
                .contains("ADD COLUMN data_path_proof_json MEDIUMTEXT NULL");
    }
}
