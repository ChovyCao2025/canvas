package org.chovy.canvas.domain.canvas;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Canvas Example Library Schema 测试类。
 *
 * <p>覆盖该后端组件在典型输入、边界条件和异常场景下的行为，确保重构或性能优化不会改变既有契约。
 * <p>测试代码只构造必要的依赖与数据，断言重点放在可观察结果、状态变更和关键副作用上。
 */
class CanvasExampleLibrarySchemaTest {

    @Test
    void migrationAddsTemplateAndCanvasExampleColumns() throws Exception {
        ClassPathResource migration =
                new ClassPathResource("db/migration/V54__canvas_example_library_schema.sql");

        String sql = migration.getContentAsString(StandardCharsets.UTF_8);

        assertThat(sql)
                .contains("ALTER TABLE canvas_template")
                .contains("template_key")
                .contains("company_type")
                .contains("marketing_scenario")
                .contains("covered_node_types")
                .contains("uk_canvas_template_key")
                .contains("ALTER TABLE canvas")
                .contains("is_example")
                .contains("source_template_key")
                .contains("idx_example_template")
                .contains("created_by = 'system'")
                .doesNotContain("created_by IS NULL");
    }
}
