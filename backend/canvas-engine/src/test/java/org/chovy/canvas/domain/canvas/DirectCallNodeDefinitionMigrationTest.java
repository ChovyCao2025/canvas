package org.chovy.canvas.domain.canvas;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class DirectCallNodeDefinitionMigrationTest {

    @Test
    void migrationRestoresDirectCallAsVisibleApiEntryAndHidesApiTrigger() throws Exception {
        ClassPathResource migration =
                new ClassPathResource("db/migration/V82__direct_call_api_entry_definition.sql");

        String sql = migration.getContentAsString(StandardCharsets.UTF_8);

        assertThat(sql).contains("WHERE type_key = 'DIRECT_CALL'");
        assertThat(sql).contains("type_name = 'API入口'");
        assertThat(sql).contains("category = '入口节点'");
        assertThat(sql).contains("enabled = 1");
        assertThat(sql).contains("WHERE type_key = 'API_TRIGGER'");
        assertThat(sql).contains("enabled = 0");
    }

    @Test
    void migrationAddsFanOutBranchesToDirectCallDefinition() throws Exception {
        ClassPathResource migration =
                new ClassPathResource("db/migration/V84__direct_call_fanout_branches.sql");

        String sql = migration.getContentAsString(StandardCharsets.UTF_8);

        assertThat(sql).contains("WHERE type_key = 'DIRECT_CALL'");
        assertThat(sql).contains("\"key\":\"branches\"");
        assertThat(sql).contains("\"type\":\"broadcast-branch-list\"");
        assertThat(sql).contains("outlet_schema = '[]'");
        assertThat(sql).contains("\"nextNodeId\":\"api_a\"");
        assertThat(sql).contains("\"nextNodeId\":\"api_b\"");
    }

    @Test
    void migrationClearsLegacyDirectCallOutletSchemaFromSavedGraphs() throws Exception {
        ClassPathResource migration =
                new ClassPathResource("db/migration/V85__clear_direct_call_legacy_outlet_schema.sql");

        String sql = migration.getContentAsString(StandardCharsets.UTF_8);

        assertThat(sql).contains("JSON_REMOVE(graph_json, '$.nodes[1].outletSchema')");
        assertThat(sql).contains("JSON_UNQUOTE(JSON_EXTRACT(graph_json, '$.nodes[1].type')) = 'DIRECT_CALL'");
        assertThat(sql).contains("UPDATE canvas_template");
        assertThat(sql).contains("UPDATE canvas_version");
    }

    @Test
    void migrationRepairsSaasExpansionDraftsWithSingleDirectCallBranch() throws Exception {
        ClassPathResource migration =
                new ClassPathResource("db/migration/V86__repair_saas_expansion_direct_call_drafts.sql");

        String sql = migration.getContentAsString(StandardCharsets.UTF_8);

        assertThat(sql).contains("source_template_key = 'saas_expansion_signal'");
        assertThat(sql).contains("COALESCE(JSON_LENGTH(JSON_EXTRACT(cv.graph_json, '$.nodes[1].config.branches')), 0) < 2");
        assertThat(sql).contains("\"label\":\"渠道 A\",\"nextNodeId\":\"api_a\"");
        assertThat(sql).contains("\"label\":\"渠道 B\",\"nextNodeId\":\"api_b\"");
        assertThat(sql).contains("JSON_REMOVE(cv.graph_json, '$.nodes[1].outletSchema')");
    }

    @Test
    void migrationRemovesManualDirectCallConfiguration() throws Exception {
        ClassPathResource migration =
                new ClassPathResource("db/migration/V87__direct_call_auto_branch_config.sql");

        String sql = migration.getContentAsString(StandardCharsets.UTF_8);

        assertThat(sql).contains("config_schema = '[]'");
        assertThat(sql).contains("outlet_schema = '[]'");
        assertThat(sql).contains("WHERE type_key = 'DIRECT_CALL'");
        assertThat(sql).contains("下游分支由画布连线自动生成");
    }

    @Test
    void migrationNormalizesCanvas46DirectCallRoutes() throws Exception {
        ClassPathResource migration =
                new ClassPathResource("db/migration/V88__repair_canvas46_direct_call_routes.sql");

        String sql = migration.getContentAsString(StandardCharsets.UTF_8);

        assertThat(sql).contains("template_key = 'saas_expansion_signal'");
        assertThat(sql).contains("\"label\":\"渠道 A\",\"nextNodeId\":\"api_a\"");
        assertThat(sql).contains("\"label\":\"渠道 B\",\"nextNodeId\":\"api_b\"");
        assertThat(sql).contains("'$.nodes[1].config.nextNodeId'");
        assertThat(sql).contains("'$.nodes[1].outletSchema'");
    }
}
