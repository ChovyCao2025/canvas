package org.chovy.canvas.domain.canvas;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class CanvasExampleTemplateMigrationTest {

    @Test
    void exampleTemplateMigrationIsNoopAfterCatalogGovernance() throws Exception {
        String sql = new ClassPathResource("db/migration/V55__canvas_example_templates.sql")
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(sql).contains("example templates are intentionally omitted");
        assertThat(sql).contains("SELECT 1");
        assertThat(sql).doesNotContain("INSERT INTO canvas_template");
    }
}
