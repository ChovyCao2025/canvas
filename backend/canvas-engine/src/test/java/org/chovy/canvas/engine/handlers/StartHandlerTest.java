package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class StartHandlerTest {

    private final StartHandler handler = new StartHandler();

    @Test
    void routesToAllConfiguredBranchesWhenStartFansOut() {
        Map<String, Object> config = Map.of(
                "branches", List.of(
                        Map.of("label", "入口 A", "nextNodeId", "trigger_a"),
                        Map.of("label", "入口 B", "nextNodeId", "trigger_b")
                )
        );

        NodeResult result = handler.executeAsync(config, new ExecutionContext()).block();

        assertThat(result.nextNodeId()).isNull();
        assertThat(result.routes()).containsEntry("branch-0", "trigger_a")
                .containsEntry("branch-1", "trigger_b");
    }
}
