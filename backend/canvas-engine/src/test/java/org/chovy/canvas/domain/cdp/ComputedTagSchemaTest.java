package org.chovy.canvas.domain.cdp;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class ComputedTagSchemaTest {

    @Test
    void migrationCreatesComputedTagDefinitionDependencyAndRunTables() throws Exception {
        try (InputStream stream = Objects.requireNonNull(
                getClass().getClassLoader().getResourceAsStream(
                        "db/migration/V105__cdp_computed_tags_lineage.sql"))) {
            String sql = new String(stream.readAllBytes(), StandardCharsets.UTF_8);

            assertThat(sql)
                    .contains("CREATE TABLE IF NOT EXISTS cdp_computed_tag_definition")
                    .contains("tenant_id")
                    .contains("tag_code")
                    .contains("compute_type")
                    .contains("expression_json")
                    .contains("refresh_mode")
                    .contains("UNIQUE KEY uk_cdp_computed_tag_definition")
                    .contains("CREATE TABLE IF NOT EXISTS cdp_computed_tag_dependency")
                    .contains("depends_on_tag_code")
                    .contains("UNIQUE KEY uk_cdp_computed_tag_dependency")
                    .contains("CREATE TABLE IF NOT EXISTS cdp_computed_tag_run")
                    .contains("cycle_path")
                    .contains("scanned_count")
                    .contains("matched_count")
                    .contains("updated_count")
                    .contains("skipped_count")
                    .contains("failed_count");
        }
    }
}
