package org.chovy.canvas.engine.audience;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class AudienceBatchComputeReactiveBoundaryTest {

    @Test
    void audienceBatchComputeServiceDoesNotBlockReactiveTaggerCalls() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/org/chovy/canvas/engine/audience/AudienceBatchComputeService.java"));

        assertThat(source).doesNotContain(".block(");
    }
}
