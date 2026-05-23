package org.chovy.canvas.domain.execution;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class PerfRunTrackingSchemaTest {

    private static final Pattern VERSION_PATTERN = Pattern.compile("V(\\d+)__.*\\.sql");

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

    @Test
    void flywayMigrationVersionsAreUnique() throws Exception {
        Path migrationDir = Path.of("src/main/resources/db/migration");

        Map<String, Long> versionCounts;
        try (var paths = Files.list(migrationDir)) {
            versionCounts = paths
                    .map(path -> VERSION_PATTERN.matcher(path.getFileName().toString()))
                    .filter(Matcher::matches)
                    .collect(Collectors.groupingBy(matcher -> matcher.group(1), Collectors.counting()));
        }

        assertThat(versionCounts)
                .allSatisfy((version, count) -> assertThat(count)
                        .as("Flyway migration version V%s must be unique", version)
                        .isEqualTo(1L));
    }
}
