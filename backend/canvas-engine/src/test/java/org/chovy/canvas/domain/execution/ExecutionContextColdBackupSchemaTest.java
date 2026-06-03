package org.chovy.canvas.domain.execution;

import org.chovy.canvas.dal.dataobject.CanvasExecutionDO;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class ExecutionContextColdBackupSchemaTest {

    @Test
    void migrationAddsExecutionContextSnapshotForColdRecovery() throws Exception {
        ClassPathResource migration = new ClassPathResource(
                "db/migration/V92__execution_context_cold_backup.sql");

        String sql = migration.getContentAsString(StandardCharsets.UTF_8);

        assertThat(sql)
                .contains("ALTER TABLE `canvas_execution`")
                .contains("ADD COLUMN `context_snapshot_json` MEDIUMTEXT NULL")
                .contains("ADD INDEX `idx_execution_paused_context` (`canvas_id`, `user_id`, `status`, `updated_at`)");
    }

    @Test
    void executionEntityExposesContextSnapshotJson() {
        CanvasExecutionDO execution = new CanvasExecutionDO();
        execution.setContextSnapshotJson("{\"executionId\":\"exec-paused\"}");

        assertThat(execution.getContextSnapshotJson()).contains("exec-paused");
    }
}
