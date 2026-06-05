package org.chovy.canvas.engine.concurrent;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BackgroundTaskExecutorGovernanceTest {

    @Test
    void productionCodeUsesManagedBackgroundTaskExecutorForVirtualThreads() throws Exception {
        Path sourceRoot = Path.of("src/main/java/org/chovy/canvas");
        List<Path> unmanagedStarts;
        try (var paths = Files.walk(sourceRoot)) {
            unmanagedStarts = paths
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> contains(path, "Thread.ofVirtual().start"))
                    .toList();
        }

        assertThat(unmanagedStarts).isEmpty();
    }

    @Test
    void virtualThreadPerTaskExecutorIsOwnedByBackgroundTaskExecutorOnly() throws Exception {
        Path sourceRoot = Path.of("src/main/java/org/chovy/canvas");
        List<Path> owners;
        try (var paths = Files.walk(sourceRoot)) {
            owners = paths
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> contains(path, "Executors.newVirtualThreadPerTaskExecutor()"))
                    .filter(path -> !path.endsWith(Path.of(
                            "engine/concurrent/BackgroundTaskExecutor.java")))
                    .toList();
        }

        assertThat(owners).isEmpty();
    }

    private static boolean contains(Path path, String needle) {
        try {
            return Files.readString(path).contains(needle);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
