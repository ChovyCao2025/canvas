package org.chovy.canvas.domain.content;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class MarketingAssetUploadIntentSchemaTest {

    @Test
    void migrationCreatesProviderUploadIntentTable() throws Exception {
        String sql = Files.readString(migrationDir().resolve("V333__marketing_asset_upload_intent.sql"));

        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS marketing_asset_upload_intent");
        assertThat(sql).contains("tenant_id BIGINT NOT NULL");
        assertThat(sql).contains("upload_token VARCHAR(128) NOT NULL");
        assertThat(sql).contains("upload_params_json JSON NOT NULL");
        assertThat(sql).contains("UNIQUE KEY uk_marketing_asset_upload_intent_key");
        assertThat(sql).contains("UNIQUE KEY uk_marketing_asset_upload_token");
        assertThat(sql).contains("INDEX idx_marketing_asset_upload_asset");
        assertThat(sql).contains("INDEX idx_marketing_asset_upload_provider_asset");
    }

    @Test
    void hardeningMigrationWidensUploadUrlForPresignedProviderUrls() throws Exception {
        String sql = Files.readString(migrationDir().resolve("V341__marketing_asset_upload_handoff_hardening.sql"));

        assertThat(sql).contains("MODIFY upload_url VARCHAR(4096) NOT NULL");
    }

    private static Path migrationDir() {
        Path modulePath = Path.of("src/main/resources/db/migration");
        if (Files.exists(modulePath)) {
            return modulePath;
        }
        return Path.of("canvas-engine/src/main/resources/db/migration");
    }
}
