package org.chovy.canvas.execution.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class RuntimeMigrationEvidenceTest {

    @Test
    void executionProductionCodeDoesNotImportLegacyEngineOrDalPackages() throws IOException {
        Path sourceRoot = Path.of("src/main/java");
        String source = Files.walk(sourceRoot)
                .filter(path -> path.toString().endsWith(".java"))
                .map(RuntimeMigrationEvidenceTest::read)
                .reduce("", String::concat);

        assertThat(source).doesNotContain("org.chovy.canvas.engine");
        assertThat(source).doesNotContain("org.chovy.canvas.dal");
    }

    private static String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
