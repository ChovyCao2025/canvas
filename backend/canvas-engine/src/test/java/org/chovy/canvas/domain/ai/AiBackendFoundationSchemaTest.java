package org.chovy.canvas.domain.ai;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class AiBackendFoundationSchemaTest {

    @Test
    void migrationCreatesProviderModelTemplateAndAuditTables() throws Exception {
        String sql = Files.readString(Path.of(
                "src/main/resources/db/migration/V162__ai_backend_foundation.sql"));

        assertThat(sql)
                .contains("CREATE TABLE IF NOT EXISTS ai_provider")
                .contains("CREATE TABLE IF NOT EXISTS ai_model_registry")
                .contains("CREATE TABLE IF NOT EXISTS ai_prompt_template")
                .contains("CREATE TABLE IF NOT EXISTS ai_usage_audit")
                .contains("uk_ai_provider_tenant_key")
                .contains("idx_ai_model_tenant_provider")
                .contains("idx_ai_template_tenant_category")
                .contains("idx_ai_audit_tenant_template");
    }

    @Test
    void migrationSeedsMockProviderAndBuiltInTemplates() throws Exception {
        String sql = Files.readString(Path.of(
                "src/main/resources/db/migration/V162__ai_backend_foundation.sql"));

        assertThat(sql)
                .contains("'mock-ai'")
                .contains("'mock-marketing-v1'")
                .contains("'Text Generation'")
                .contains("'Smart Scoring'");
    }
}
