package org.chovy.canvas.migration;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class FlywayMigrationPolicyTest {

    private static final Pattern MIGRATION_FILE = Pattern.compile("^V([0-9]+)__[A-Za-z0-9][A-Za-z0-9_]*\\.sql$");

    @Test
    void bootRuntimeMigrationsUseUniqueNumericVersions() throws Exception {
        Path migrationDir = migrationDir();

        List<String> invalidNames = new ArrayList<>();
        Map<Integer, List<String>> filesByVersion = new HashMap<>();

        try (Stream<Path> paths = Files.list(migrationDir)) {
            paths.filter(path -> path.getFileName().toString().startsWith("V"))
                    .filter(path -> path.getFileName().toString().endsWith(".sql"))
                    .forEach(path -> {
                        String fileName = path.getFileName().toString();
                        Matcher matcher = MIGRATION_FILE.matcher(fileName);
                        if (!matcher.matches()) {
                            invalidNames.add(fileName);
                            return;
                        }
                        int version = Integer.parseInt(matcher.group(1));
                        filesByVersion.computeIfAbsent(version, ignored -> new ArrayList<>()).add(fileName);
                    });
        }

        List<String> duplicateVersions = filesByVersion.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .map(entry -> "V" + entry.getKey() + " -> " + entry.getValue())
                .sorted()
                .toList();

        assertThat(invalidNames).isEmpty();
        assertThat(duplicateVersions).isEmpty();
        assertThat(filesByVersion).isNotEmpty();
    }

    @Test
    void trackedConflictRepairMigrationsRemainExplicitAndExecutableFromBoot() throws Exception {
        Path migrationDir = migrationDir();

        assertThat(migrationDir.resolve("V91__sanitize_demo_datasource_credentials.sql"))
                .as("V91 is already owned by data security and tenant isolation")
                .doesNotExist();
        assertThat(migrationDir.resolve("V92__enforce_core_tenant_not_null.sql"))
                .as("V92 is already owned by execution context cold backup")
                .doesNotExist();
        assertThat(migrationDir.resolve("V354__sanitize_demo_datasource_credentials.sql"))
                .as("current tracked sanitize demo datasource credentials migration")
                .exists();
        assertThat(migrationDir.resolve("V355__enforce_core_tenant_not_null.sql"))
                .as("V355 is already tracked by a different migration; core tenant NOT NULL repair must use V356")
                .doesNotExist();
        assertThat(migrationDir.resolve("V356__enforce_core_tenant_not_null.sql"))
                .as("current tracked core tenant NOT NULL repair migration")
                .exists();

        String v93 = Files.readString(migrationDir.resolve("V93__tenant_scope_datasources_and_execution_requests.sql"));
        assertThat(v93)
                .as("V91 data_security already owns data_source_config tenant scope in merged history, so V93 must be idempotent")
                .contains("INFORMATION_SCHEMA.COLUMNS")
                .contains("COLUMN_NAME = 'tenant_id'")
                .contains("INFORMATION_SCHEMA.STATISTICS")
                .contains("INDEX_NAME = 'idx_data_source_tenant_type_enabled'")
                .contains("ALTER TABLE canvas_execution_request")
                .contains("ADD COLUMN tenant_id BIGINT NULL AFTER id");
    }

    private Path migrationDir() {
        Path modulePath = Path.of("src/main/resources/db/migration");
        if (Files.exists(modulePath)) {
            return modulePath;
        }
        return Path.of("canvas-boot/src/main/resources/db/migration");
    }
}
