package org.chovy.canvas.testsupport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;

public final class MigrationTestSupport {

    private static final Path MIGRATION_DIR = Path.of("src/main/resources/db/migration");

    private MigrationTestSupport() {
    }

    public static String readMigration(String description) throws IOException {
        List<Path> matches;
        try (var paths = Files.list(MIGRATION_DIR)) {
            matches = paths
                    .filter(path -> path.getFileName().toString().startsWith("V"))
                    .filter(path -> path.getFileName().toString()
                            .endsWith("__" + description + ".sql"))
                    .sorted()
                    .toList();
        }
        if (matches.isEmpty()) {
            throw new NoSuchFileException(MIGRATION_DIR.resolve("V*__" + description + ".sql").toString());
        }
        if (matches.size() > 1) {
            throw new IllegalStateException("multiple migration files match " + description + ": " + matches);
        }
        return Files.readString(matches.get(0));
    }
}
