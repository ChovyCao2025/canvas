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
    void migrationsUseUniqueNumericVersions() throws Exception {
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

    private Path migrationDir() {
        Path modulePath = Path.of("src/main/resources/db/migration");
        if (Files.exists(modulePath)) {
            return modulePath;
        }
        return Path.of("canvas-engine/src/main/resources/db/migration");
    }
}
