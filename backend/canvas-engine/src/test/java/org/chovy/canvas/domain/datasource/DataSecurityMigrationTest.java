package org.chovy.canvas.domain.datasource;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class DataSecurityMigrationTest {

    @Test
    void migrationFilesAfterRepairDoNotSeedRootJdbcCredentials() throws Exception {
        Path migrationRoot = migrationDir();
        try (var files = Files.list(migrationRoot)) {
            var offending = files
                    .filter(path -> path.getFileName().toString().matches("V\\d+__.*\\.sql"))
                    .filter(path -> version(path) >= 91)
                    .filter(path -> containsRootCredential(path))
                    .map(path -> path.getFileName().toString())
                    .toList();

            assertThat(offending).isEmpty();
        }
    }

    @Test
    void repairMigrationDisablesHistoricalDemoRootCredentials() throws Exception {
        Path migration = migrationDir().resolve("V91__data_security_and_tenant_isolation.sql");
        String sql = Files.readString(migration);

        assertThat(sql).contains("data_source_config");
        assertThat(sql).contains("canvas_demo_app");
        assertThat(sql).contains("canvas_demo_password_change_me");
        assertThat(containsRootCredential(migration)).isFalse();
    }

    private static boolean containsRootCredential(Path path) {
        try {
            String sql = Files.readString(path).toLowerCase(Locale.ROOT);
            return sql.contains("\"username\":\"root\"")
                    || sql.contains("'root',\n    'root'")
                    || sql.contains("\"password\":\"root\"");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to inspect migration " + path, e);
        }
    }

    private static int version(Path path) {
        String name = path.getFileName().toString();
        int start = name.startsWith("V") ? 1 : 0;
        int end = name.indexOf("__");
        return Integer.parseInt(name.substring(start, end));
    }

    private static Path migrationDir() {
        Path modulePath = Path.of("src/main/resources/db/migration");
        if (Files.exists(modulePath)) {
            return modulePath;
        }
        return Path.of("canvas-engine/src/main/resources/db/migration");
    }
}
