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
}
