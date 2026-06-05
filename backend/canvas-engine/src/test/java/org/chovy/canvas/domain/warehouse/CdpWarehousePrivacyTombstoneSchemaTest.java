package org.chovy.canvas.domain.warehouse;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.chovy.canvas.testsupport.MigrationTestSupport.readMigration;

class CdpWarehousePrivacyTombstoneSchemaTest {

    @Test
    void migrationCreatesPrivacySubjectTombstoneTableAndAuditIndexes() throws Exception {
        String sql = readMigration("cdp_warehouse_privacy_tombstone_guard");

        assertThat(sql)
                .contains("CREATE TABLE IF NOT EXISTS cdp_warehouse_privacy_subject_tombstone")
                .contains("tenant_id BIGINT NOT NULL DEFAULT 0")
                .contains("subject_type VARCHAR(64) NOT NULL DEFAULT 'USER_ID'")
                .contains("subject_hash VARCHAR(128) NOT NULL")
                .contains("subject_ref_masked VARCHAR(128) NOT NULL")
                .contains("blocked_event_count BIGINT NOT NULL DEFAULT 0")
                .contains("last_blocked_at DATETIME DEFAULT NULL")
                .contains("UNIQUE KEY uk_cdp_warehouse_privacy_tombstone_subject (tenant_id, subject_type, subject_hash)")
                .contains("INDEX idx_cdp_warehouse_privacy_tombstone_status")
                .contains("INDEX idx_cdp_warehouse_privacy_tombstone_blocked");
    }
}
