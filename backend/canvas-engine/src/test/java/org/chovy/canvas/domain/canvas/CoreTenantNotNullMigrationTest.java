package org.chovy.canvas.domain.canvas;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class CoreTenantNotNullMigrationTest {

    @Test
    void v273BackfillsAndEnforcesCoreTenantColumns() throws Exception {
        String sql = Files.readString(migrationDir().resolve("V273__enforce_core_tenant_not_null.sql"));

        assertThat(sql).contains("UPDATE canvas");
        assertThat(sql).contains("UPDATE canvas_version");
        assertThat(sql).contains("UPDATE canvas_execution");
        assertThat(sql).contains("UPDATE canvas_execution_trace");
        assertThat(sql).contains("ALTER TABLE sys_user MODIFY COLUMN tenant_id BIGINT NOT NULL");
        assertThat(sql).contains("ALTER TABLE canvas MODIFY COLUMN tenant_id BIGINT NOT NULL");
        assertThat(sql).contains("ALTER TABLE canvas_version MODIFY COLUMN tenant_id BIGINT NOT NULL");
        assertThat(sql).contains("ALTER TABLE canvas_execution MODIFY COLUMN tenant_id BIGINT NOT NULL");
        assertThat(sql).contains("ALTER TABLE canvas_execution_trace MODIFY COLUMN tenant_id BIGINT NOT NULL");
    }

    @Test
    void v93ScopesDatasourceAndExecutionRequestTablesByTenant() throws Exception {
        String sql = Files.readString(migrationDir().resolve("V93__tenant_scope_datasources_and_execution_requests.sql"));

        assertThat(sql).contains("ALTER TABLE data_source_config");
        assertThat(sql).contains("ADD COLUMN tenant_id BIGINT NULL AFTER id");
        assertThat(sql).contains("ALTER TABLE data_source_config")
                .contains("MODIFY COLUMN tenant_id BIGINT NOT NULL")
                .contains("idx_data_source_tenant_type_enabled");
        assertThat(sql).contains("ALTER TABLE canvas_execution_request");
        assertThat(sql).contains("LEFT JOIN canvas c ON c.id = r.canvas_id");
        assertThat(sql).contains("ALTER TABLE canvas_execution_request")
                .contains("MODIFY COLUMN tenant_id BIGINT NOT NULL")
                .contains("idx_execution_request_tenant_status_updated")
                .contains("idx_execution_request_tenant_canvas_status_updated");
    }

    private static Path migrationDir() {
        Path modulePath = Path.of("src/main/resources/db/migration");
        if (Files.exists(modulePath)) {
            return modulePath;
        }
        return Path.of("canvas-engine/src/main/resources/db/migration");
    }
}
