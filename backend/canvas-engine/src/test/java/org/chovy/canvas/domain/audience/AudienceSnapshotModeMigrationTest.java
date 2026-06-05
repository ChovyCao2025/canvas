package org.chovy.canvas.domain.audience;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class AudienceSnapshotModeMigrationTest {

    @Test
    void migrationAddsAudienceModeSnapshotTableAndTaggerConfig() throws Exception {
        String sql = Files.readString(Path.of(
                "src/main/resources/db/migration/V252__audience_snapshot_mode_and_defaults.sql"));

        assertThat(sql)
                .contains("ALTER TABLE audience_definition")
                .contains("default_snapshot_mode")
                .contains("CREATE TABLE IF NOT EXISTS audience_snapshot")
                .contains("user_ids_json")
                .contains("audienceSnapshotMode")
                .contains("audienceSnapshotId")
                .contains("STATIC_LOCKED")
                .contains("DYNAMIC_REFRESH");
    }
}
