package org.chovy.canvas.execution.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class NodeTypeGovernanceTest {

    @Test
    void executionRuntimeUsesLocalNodeTypeStringsInsteadOfCommonBusinessEnum() throws IOException {
        Path sourceRoot = Path.of("src/main/java");
        String source = Files.walk(sourceRoot)
                .filter(path -> path.toString().endsWith(".java"))
                .map(NodeTypeGovernanceTest::read)
                .reduce("", String::concat);

        assertThat(source).doesNotContain("org.chovy.canvas.common.enums.NodeType");
    }

    private static String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
