package org.chovy.canvas.engine.scheduler;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class DagEngineManagedSubscriptionTest {

    @Test
    void dagEngineDoesNotOwnRawReactorSubscriptions() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/org/chovy/canvas/engine/scheduler/DagEngine.java"));

        assertThat(source)
                .satisfiesAnyOf(
                        s -> assertThat(s).contains("BackgroundSubscriptionRegistry"),
                        s -> assertThat(s).contains("TrackedReactiveTaskRegistry"));
        assertThat(source).doesNotContain(".subscribe(");
    }
}
