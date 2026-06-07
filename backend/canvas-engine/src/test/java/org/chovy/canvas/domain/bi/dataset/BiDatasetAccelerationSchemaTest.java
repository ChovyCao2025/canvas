package org.chovy.canvas.domain.bi.dataset;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class BiDatasetAccelerationSchemaTest {

    @Test
    void migrationCreatesCanonicalPolicyAndExtractRunTables() throws Exception {
        String sql = Files.readString(Path.of(
                "src/main/resources/db/migration/V310__bi_dataset_extract_acceleration.sql"));

        assertThat(sql)
                .contains("CREATE TABLE IF NOT EXISTS bi_dataset_acceleration_policy")
                .contains("refresh_interval_minutes BIGINT NOT NULL DEFAULT 60")
                .contains("ttl_seconds BIGINT NOT NULL DEFAULT 300")
                .contains("max_rows BIGINT NOT NULL DEFAULT 100000")
                .contains("last_status VARCHAR(32) NOT NULL DEFAULT 'IDLE'")
                .contains("UNIQUE KEY uk_bi_dataset_acceleration_policy_dataset")
                .contains("CREATE TABLE IF NOT EXISTS bi_dataset_extract_refresh_run")
                .contains("started_at DATETIME NOT NULL")
                .contains("error_message VARCHAR(1000)")
                .contains("updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
                .doesNotContain("error_summary");
    }
}
