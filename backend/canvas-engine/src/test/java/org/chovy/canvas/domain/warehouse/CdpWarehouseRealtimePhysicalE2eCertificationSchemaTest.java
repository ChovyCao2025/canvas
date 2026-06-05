package org.chovy.canvas.domain.warehouse;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class CdpWarehouseRealtimePhysicalE2eCertificationSchemaTest {

    @Test
    void migrationAddsRealtimeEvidenceFieldsToCertificationRuns() throws Exception {
        String sql = Files.readString(Path.of(
                "src/main/resources/db/migration/V247__cdp_warehouse_e2e_realtime_evidence.sql"));

        assertThat(sql).contains("ALTER TABLE cdp_warehouse_e2e_certification_run");
        assertThat(sql).contains("require_realtime TINYINT NOT NULL DEFAULT 0");
        assertThat(sql).contains("realtime_pipeline_status_json MEDIUMTEXT NULL");
        assertThat(sql).contains("realtime_job_status_json MEDIUMTEXT NULL");
    }
}
