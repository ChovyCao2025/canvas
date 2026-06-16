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

/**
 * 校验 Canvas Boot 打包的 Flyway 迁移文件命名、版本和历史冲突修复约束。
 */
class FlywayMigrationPolicyTest {

    /**
     * Boot 运行时允许的 Flyway SQL 迁移文件名格式。
     */
    private static final Pattern MIGRATION_FILE = Pattern.compile("^V([0-9]+)__[A-Za-z0-9][A-Za-z0-9_]*\\.sql$");

    /**
     * 验证 Boot 运行时迁移文件使用合法文件名并保持数字版本唯一。
     *
     * @throws Exception 读取迁移目录失败时抛出
     */
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
                        // 只用数字版本分组，确保不同描述名不会掩盖同一版本号冲突。
                        int version = Integer.parseInt(matcher.group(1));
                        filesByVersion.computeIfAbsent(version, ignored -> new ArrayList<>()).add(fileName);
                    });
        }

        // 将冲突版本格式化为稳定字符串，便于断言失败时直接定位文件。
        List<String> duplicateVersions = filesByVersion.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .map(entry -> "V" + entry.getKey() + " -> " + entry.getValue())
                .sorted()
                .toList();

        assertThat(invalidNames).isEmpty();
        assertThat(duplicateVersions).isEmpty();
        assertThat(filesByVersion).isNotEmpty();
    }

    /**
     * 验证已知冲突修复迁移保留在当前约定版本并可由 Boot 运行时加载。
     *
     * @throws Exception 读取迁移脚本失败时抛出
     */
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

    /**
     * 根据当前测试运行目录定位 Boot 模块中的 Flyway 迁移目录。
     *
     * @return Boot 运行时迁移目录路径
     */
    private Path migrationDir() {
        Path modulePath = Path.of("src/main/resources/db/migration");
        if (Files.exists(modulePath)) {
            return modulePath;
        }
        return Path.of("canvas-boot/src/main/resources/db/migration");
    }
}
