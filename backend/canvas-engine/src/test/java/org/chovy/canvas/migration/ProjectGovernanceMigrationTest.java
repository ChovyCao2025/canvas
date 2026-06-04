package org.chovy.canvas.migration;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectGovernanceMigrationTest {

    @Test
    void migrationDefinesProjectMemberAndAssignmentTables() throws Exception {
        String sql = Files.readString(Path.of(
                "src/main/resources/db/migration/V189__project_governance.sql"));

        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS canvas_project");
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS canvas_project_member");
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS canvas_project_folder");
        assertThat(sql).contains("ADD COLUMN project_id BIGINT NULL");
        assertThat(sql).contains("uk_canvas_project_tenant_key");
        assertThat(sql).contains("uk_canvas_project_member_project_user");
    }
}
