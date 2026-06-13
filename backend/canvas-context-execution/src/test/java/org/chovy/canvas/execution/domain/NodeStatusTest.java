package org.chovy.canvas.execution.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NodeStatusTest {

    @Test
    void preservesRuntimeStatusVocabulary() {
        assertThat(NodeStatus.values()).extracting(Enum::name)
                .containsExactly("PENDING", "RUNNING", "WAITING", "SUCCESS", "FAILED", "TIMEOUT", "SUPPRESSED", "SKIPPED");
    }
}
