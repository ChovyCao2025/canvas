package org.chovy.canvas.domain.execution;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class PerfRunTrackingSchemaTest {

    @Test
    void migrationAddsPerfRunIdToAllPerformanceLedgers() throws Exception {
        ClassPathResource migration = new ClassPathResource("db/migration/V50__perf_run_tracking.sql");

        String sql = migration.getContentAsString(StandardCharsets.UTF_8);

        assertThat(sql)
                .contains("ALTER TABLE `event_log`")
                .contains("ADD COLUMN `perf_run_id` VARCHAR(80) NULL")
                .contains("ADD INDEX `idx_event_log_perf_run` (`perf_run_id`, `created_at`)")
                .contains("ALTER TABLE `canvas_execution`")
                .contains("ADD INDEX `idx_execution_perf_run` (`perf_run_id`, `created_at`)")
                .contains("ALTER TABLE `canvas_execution_request`")
                .contains("ADD INDEX `idx_execution_request_perf_run` (`perf_run_id`, `status`, `updated_at`)")
                .contains("ALTER TABLE `canvas_execution_dlq`")
                .contains("ADD INDEX `idx_execution_dlq_perf_run` (`perf_run_id`, `failed_at`)");
    }
}
