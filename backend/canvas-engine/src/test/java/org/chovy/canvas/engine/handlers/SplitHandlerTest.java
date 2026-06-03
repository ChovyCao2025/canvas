package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SplitHandlerTest {

    private final SplitHandler handler = new SplitHandler();

    @Test
    void stableAllocationRoutesSameUserToSameBranch() {
        ExecutionContext ctx = context("user-123");
        Map<String, Object> config = Map.of(
                "splitKey", "pricing-exp",
                "branches", List.of(
                        Map.of("branchId", "a", "nextNodeId", "node-a", "weight", 50),
                        Map.of("branchId", "b", "nextNodeId", "node-b", "weight", 50)
                )
        );

        NodeResult first = handler.executeAsync(config, ctx).block();
        NodeResult second = handler.executeAsync(config, ctx).block();

        assertThat(first.routes()).isEqualTo(second.routes());
        assertThat(first.output()).containsKey("splitBranch");
    }

    @Test
    void emptyBranchesTerminatesWithoutRoute() {
        NodeResult result = handler.executeAsync(Map.of("branches", List.of()), context("user-x")).block();

        assertThat(result.success()).isTrue();
        assertThat(result.routes()).isEmpty();
        assertThat(result.nextNodeId()).isNull();
    }

    private static ExecutionContext context(String userId) {
        ExecutionContext ctx = new ExecutionContext();
        ctx.setCanvasId(10L);
        ctx.setUserId(userId);
        return ctx;
    }
}
