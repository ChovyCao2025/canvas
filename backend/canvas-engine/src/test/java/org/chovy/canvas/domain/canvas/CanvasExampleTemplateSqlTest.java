package org.chovy.canvas.domain.canvas;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class CanvasExampleTemplateSqlTest {

    @Test
    void omittedExampleTemplateMigrationsStayNoop() throws Exception {
        String seedSql = Files.readString(Path.of("src/main/resources/db/migration/V55__canvas_example_templates.sql"));
        String directCallRepairSql = Files.readString(Path.of("src/main/resources/db/migration/V83__fix_saas_expansion_signal_direct_entry.sql"));

        assertThat(seedSql).contains("SELECT 1");
        assertThat(directCallRepairSql).contains("no-op after governed node catalog consolidation");
        assertThat(directCallRepairSql).contains("SELECT 1");
    }
}
