package org.chovy.canvas.domain.canvas;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class CanvasExampleTemplateSqlTest {

    @Test
    void later_migration_links_groovy_transform_example_score_to_api_call() throws Exception {
        String seedSql = Files.readString(Path.of("src/main/resources/db/migration/V55__canvas_example_templates.sql"));
        String fixSql = Files.readString(Path.of("src/main/resources/db/migration/V80__fix_groovy_example_api_mapping.sql"));

        assertThat(seedSql).contains("'component_groovy_transform'");
        assertThat(seedSql).contains("\"code\":\"return [score: 88]\"");
        assertThat(seedSql).doesNotContain("\"inputParams\":{\"userId\":\"$${score}\"");
        assertThat(fixSql).contains("template_key = 'component_groovy_transform'");
        assertThat(fixSql).contains("JSON_OBJECT('userId', '$${score}')");
    }

    @Test
    void later_migration_addsDirectCallEntryToSaasExpansionSignalExample() throws Exception {
        String fixSql = Files.readString(Path.of("src/main/resources/db/migration/V83__fix_saas_expansion_signal_direct_entry.sql"));

        assertThat(fixSql).contains("template_key = 'saas_expansion_signal'");
        assertThat(fixSql).contains("\"type\":\"DIRECT_CALL\"");
        assertThat(fixSql).contains("\"nextNodeId\":\"api_a\"");
        assertThat(fixSql).contains("c.source_template_key = 'saas_expansion_signal'");
    }
}
