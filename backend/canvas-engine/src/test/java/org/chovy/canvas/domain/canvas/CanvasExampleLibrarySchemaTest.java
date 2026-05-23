package org.chovy.canvas.domain.canvas;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

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
