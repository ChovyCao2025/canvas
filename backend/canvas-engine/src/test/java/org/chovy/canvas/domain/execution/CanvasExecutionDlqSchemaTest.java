package org.chovy.canvas.domain.execution;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 画布执行 Dlq Schema 测试类。
 *
 * <p>覆盖该后端组件在典型输入、边界条件和异常场景下的行为，确保重构或性能优化不会改变既有契约。
 * <p>测试代码只构造必要的依赖与数据，断言重点放在可观察结果、状态变更和关键副作用上。
 */
class CanvasExecutionDlqSchemaTest {

    @Test
    void dlqMigrationIncludesTriggerReplayContextColumns() throws Exception {
        ClassPathResource migration = new ClassPathResource("db/migration/V52__dlq_trigger_context.sql");

        String sql = migration.getContentAsString(StandardCharsets.UTF_8);

        assertThat(sql)
                .contains("`trigger_type`")
                .contains("`trigger_node_type`")
                .contains("`match_key`");
    }

    @Test
    void retentionMigrationRegistersExecutionTablesAndCleanupLedger() throws Exception {
        ClassPathResource migration = new ClassPathResource("db/migration/V239__execution_retention_policy.sql");

        String sql = migration.getContentAsString(StandardCharsets.UTF_8);

        assertThat(sql)
                .contains("CREATE TABLE IF NOT EXISTS execution_retention_policy")
                .contains("CREATE TABLE IF NOT EXISTS execution_retention_run")
                .contains("CREATE TABLE IF NOT EXISTS execution_retention_archive_manifest")
                .contains("uk_execution_retention_policy_table")
                .contains("idx_execution_retention_run_table_cutoff")
                .contains("canvas_execution_trace', 30, 'ARCHIVE_THEN_DELETE'")
                .contains("canvas_execution_dlq', 90, 'DELETE_AFTER_RESOLUTION'")
                .contains("canvas_execution_stats', 730, 'KEEP_AGGREGATE'");
    }
}
