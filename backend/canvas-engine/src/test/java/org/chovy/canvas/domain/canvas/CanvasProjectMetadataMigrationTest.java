package org.chovy.canvas.domain.canvas;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class CanvasProjectMetadataMigrationTest {

    @Test
    void migrationAddsFlatProjectAndFolderColumns() throws Exception {
        String sql = Files.readString(Path.of(
                "src/main/resources/db/migration/V259__canvas_project_folder_metadata.sql"));

        assertThat(sql)
                .contains("ALTER TABLE canvas")
                .contains("project_key")
                .contains("project_name")
                .contains("folder_key")
                .contains("folder_name")
                .contains("idx_canvas_project_folder");
    }
}
