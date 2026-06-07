package org.chovy.canvas.domain.content;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class MarketingContentHubSchemaTest {

    @Test
    void migrationCreatesTenantScopedContentHubTables() throws Exception {
        String sql = Files.readString(migrationDir().resolve("V301__marketing_content_hub.sql"));

        assertThat(sql)
                .contains("CREATE TABLE IF NOT EXISTS marketing_asset_folder")
                .contains("CREATE TABLE IF NOT EXISTS marketing_asset")
                .contains("CREATE TABLE IF NOT EXISTS marketing_content_template")
                .contains("CREATE TABLE IF NOT EXISTS marketing_content_template_version")
                .contains("CREATE TABLE IF NOT EXISTS marketing_content_entry")
                .contains("CREATE TABLE IF NOT EXISTS marketing_content_entry_version")
                .contains("UNIQUE KEY uk_marketing_asset_key (tenant_id, asset_key)")
                .contains("UNIQUE KEY uk_marketing_content_template_key (tenant_id, template_key)")
                .contains("UNIQUE KEY uk_marketing_content_entry_key (tenant_id, entry_key)")
                .contains("INDEX idx_marketing_asset_search (tenant_id, asset_type, status, updated_at)")
                .contains("INDEX idx_marketing_template_search (tenant_id, channel, status, updated_at)")
                .contains("INDEX idx_marketing_entry_search (tenant_id, content_type, status, updated_at)");
    }

    private Path migrationDir() {
        Path modulePath = Path.of("src/main/resources/db/migration");
        if (Files.exists(modulePath)) {
            return modulePath;
        }
        return Path.of("canvas-engine/src/main/resources/db/migration");
    }
}
