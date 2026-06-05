package org.chovy.canvas.domain.warehouse;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.chovy.canvas.testsupport.MigrationTestSupport.readMigration;

class CdpWarehousePrivacyErasureSchemaTest {

    @Test
    void migrationCreatesPrivacyErasureRequestAndAssetProofTables() throws Exception {
        String sql = readMigration("cdp_warehouse_privacy_erasure_proof");

        assertThat(sql)
                .contains("CREATE TABLE IF NOT EXISTS cdp_warehouse_privacy_erasure_request")
                .contains("request_key VARCHAR(128) NOT NULL")
                .contains("subject_hash VARCHAR(128) NOT NULL")
                .contains("subject_ref_masked VARCHAR(128) NOT NULL")
                .contains("INDEX idx_cdp_warehouse_privacy_erasure_status_due")
                .contains("CREATE TABLE IF NOT EXISTS cdp_warehouse_privacy_erasure_asset_proof")
                .contains("asset_key VARCHAR(128) NOT NULL")
                .contains("status VARCHAR(32) NOT NULL DEFAULT 'PLANNED'")
                .contains("UNIQUE KEY uk_cdp_warehouse_privacy_erasure_asset")
                .contains("INDEX idx_cdp_warehouse_privacy_erasure_asset_status");
    }
}
