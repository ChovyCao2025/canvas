package org.chovy.canvas.domain.ai;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class AiLlmNodeProductionizationSchemaTest {

    @Test
    void migrationRegistersAiLlmNodeAndRemovesLegacyKey() throws Exception {
        String sql = Files.readString(Path.of(
                "src/main/resources/db/migration/V164__ai_llm_node_productionization.sql"));

        assertThat(sql)
                .contains("'AI_LLM'")
                .contains("org.chovy.canvas.engine.handlers.AiLlmHandler")
                .contains("DELETE FROM node_type_registry WHERE type_key = 'AI_NEXT_BEST_ACTION'")
                .contains("/meta/ai-providers")
                .contains("/meta/ai-templates")
                .contains("ai_output")
                .contains("ai_fallback_used");
    }

    @Test
    void migrationAddsRuntimeAuditColumnsSafely() throws Exception {
        String sql = Files.readString(Path.of(
                "src/main/resources/db/migration/V164__ai_llm_node_productionization.sql"));

        assertThat(sql)
                .contains("information_schema.columns")
                .contains("canvas_id")
                .contains("execution_id")
                .contains("node_id")
                .contains("prompt_tokens")
                .contains("completion_tokens")
                .contains("idx_ai_audit_runtime");
    }

    @Test
    void registryFollowUpExposesSafeLlmOverrides() throws Exception {
        String sql = Files.readString(Path.of(
                "src/main/resources/db/migration/V360__ai_llm_registry_safe_overrides.sql"));

        assertThat(sql)
                .contains("schemaOverride")
                .contains("maxTokens")
                .contains("outlet_schema")
                .contains("\"id\":\"success\"")
                .contains("UPDATE node_type_registry")
                .contains("WHERE type_key = 'AI_LLM'")
                .doesNotContain("failNodeId");
    }
}
