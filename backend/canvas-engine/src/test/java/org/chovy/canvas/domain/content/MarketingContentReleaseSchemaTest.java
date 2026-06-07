package org.chovy.canvas.domain.content;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class MarketingContentReleaseSchemaTest {

    @Test
    void migrationCreatesReleaseLoopTables() throws Exception {
        String sql = Files.readString(migrationDir().resolve("V327__marketing_content_release_loop.sql"));

        assertThat(sql)
                .contains("CREATE TABLE IF NOT EXISTS marketing_content_release")
                .contains("CREATE TABLE IF NOT EXISTS marketing_content_release_item")
                .contains("CREATE TABLE IF NOT EXISTS marketing_content_audit_event")
                .contains("UNIQUE KEY uk_marketing_content_release_version (tenant_id, release_key, source_version)")
                .contains("INDEX idx_marketing_content_release_latest (tenant_id, source_type, source_key, status, published_at)")
                .contains("INDEX idx_marketing_content_audit_target (tenant_id, target_type, target_key, created_at)");
    }

    @Test
    void hardeningMigrationEnforcesSingleActiveReleasePerKey() throws Exception {
        String sql = Files.readString(migrationDir().resolve("V342__marketing_content_release_active_uniqueness.sql"));

        assertThat(sql)
                .contains("UPDATE marketing_content_release newer")
                .contains("SET older.status = 'SUPERSEDED'")
                .contains("older.status = 'ACTIVE'")
                .contains("newer.status = 'ACTIVE'")
                .contains("newer.source_version > older.source_version")
                .contains("ADD COLUMN active_release_key VARCHAR(128)")
                .contains("GENERATED ALWAYS AS (CASE WHEN status = 'ACTIVE' THEN release_key ELSE NULL END) STORED")
                .contains("ADD UNIQUE KEY uk_marketing_content_release_active (tenant_id, active_release_key)");
    }

    private static Path migrationDir() {
        Path modulePath = Path.of("src/main/resources/db/migration");
        if (Files.exists(modulePath)) {
            return modulePath;
        }
        return Path.of("canvas-engine/src/main/resources/db/migration");
    }
}
