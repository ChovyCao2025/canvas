package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DirectCallHandlerTest {

    private final DirectCallHandler handler = new DirectCallHandler();

    @Test
    void routesToAllConfiguredBranchesWhenDirectCallFansOut() {
        Map<String, Object> config = Map.of(
                "branches", List.of(
                        Map.of("label", "查询用户", "nextNodeId", "api_user"),
                        Map.of("label", "查询订单", "nextNodeId", "api_order")
                )
        );

        NodeResult result = handler.executeAsync(config, new ExecutionContext()).block();

        assertThat(result.nextNodeId()).isNull();
        assertThat(result.routes()).containsEntry("branch-0", "api_user")
                .containsEntry("branch-1", "api_order");
    }
}
