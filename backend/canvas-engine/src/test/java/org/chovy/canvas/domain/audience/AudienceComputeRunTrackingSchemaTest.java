package org.chovy.canvas.domain.audience;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import org.chovy.canvas.dal.dataobject.AudienceComputeRunDO;

class AudienceComputeRunTrackingSchemaTest {

    @Test
    void migrationAddsAudienceComputeRunLedger() throws Exception {
        ClassPathResource migration = new ClassPathResource("db/migration/V73__audience_compute_run_tracking.sql");

        String sql = migration.getContentAsString(StandardCharsets.UTF_8);

        assertThat(sql)
                .contains("CREATE TABLE IF NOT EXISTS `audience_compute_run`")
                .contains("`perf_run_id` VARCHAR(80) NULL")
                .contains("`perf_input_id` VARCHAR(160) NULL")
                .contains("KEY `idx_audience_compute_run_perf` (`perf_run_id`, `audience_id`, `updated_at`)")
                .contains("KEY `idx_audience_compute_run_input` (`perf_input_id`)");
    }

    @Test
    void entityExposesPerfRunFields() {
        AudienceComputeRunDO run = new AudienceComputeRunDO();
        run.setPerfRunId("perf_20260523_001");
        run.setPerfInputId("perf_20260523_001:audience:1");

        assertThat(run.getPerfRunId()).isEqualTo("perf_20260523_001");
        assertThat(run.getPerfInputId()).isEqualTo("perf_20260523_001:audience:1");
    }
}
