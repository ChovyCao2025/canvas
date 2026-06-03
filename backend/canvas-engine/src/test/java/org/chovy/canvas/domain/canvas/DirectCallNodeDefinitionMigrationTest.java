package org.chovy.canvas.domain.canvas;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DirectCallNodeDefinitionMigrationTest {

    @Test
    void legacyDirectCallRepairMigrationsAreNoopAfterCatalogGovernance() throws Exception {
        for (String file : List.of(
                "V82__direct_call_api_entry_definition.sql",
                "V83__fix_saas_expansion_signal_direct_entry.sql",
                "V84__direct_call_fanout_branches.sql",
                "V85__clear_direct_call_legacy_outlet_schema.sql",
                "V86__repair_saas_expansion_direct_call_drafts.sql",
                "V87__direct_call_auto_branch_config.sql",
                "V88__repair_canvas46_direct_call_routes.sql",
                "V89__repair_canvas42_selector_demo.sql"
        )) {
            ClassPathResource migration = new ClassPathResource("db/migration/" + file);
            String sql = migration.getContentAsString(StandardCharsets.UTF_8);

            assertThat(sql).contains("no-op after governed node catalog consolidation");
            assertThat(sql).contains("SELECT 1");
        }
    }
}
