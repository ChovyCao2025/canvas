package org.chovy.canvas.domain.analytics;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.chovy.canvas.testsupport.MigrationTestSupport.readMigration;
import static org.assertj.core.api.Assertions.assertThat;

class CdpAudienceMaterializationRollbackSchemaTest {

    @Test
    void migrationCreatesAudienceBitmapRollbackAuditTable() throws Exception {
        String sql = readMigration("cdp_audience_materialization_rollback");

        assertThat(sql)
                .contains("CREATE TABLE IF NOT EXISTS audience_bitmap_rollback")
                .contains("target_version BIGINT NOT NULL")
                .contains("target_bitmap_key VARCHAR(256) NOT NULL")
                .contains("rolled_back_versions BIGINT NOT NULL DEFAULT 0")
                .contains("operator_name VARCHAR(128) NOT NULL")
                .contains("INDEX idx_audience_bitmap_rollback_audience")
                .contains("INDEX idx_audience_bitmap_rollback_operator");
    }
}
