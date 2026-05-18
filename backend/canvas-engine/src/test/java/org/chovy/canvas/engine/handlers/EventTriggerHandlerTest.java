package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeResult;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EventTriggerHandlerTest {

    private final EventTriggerHandler handler = new EventTriggerHandler();

    @Test
    void routes_to_nextNodeId() {
        ExecutionContext ctx = new ExecutionContext();
        ctx.setUserId("u1");

        NodeResult result = handler.executeAsync(
                Map.of("eventCode", "ORDER_COMPLETE", "nextNodeId", "node_api"),
                ctx
        ).block();

        assertThat(result).isNotNull();
        assertThat(result.success()).isTrue();
        assertThat(result.nextNodeId()).isEqualTo("node_api");
    }

    @Test
    void returns_null_nextNodeId_when_not_configured() {
        ExecutionContext ctx = new ExecutionContext();
        ctx.setUserId("u1");

        NodeResult result = handler.executeAsync(
                Map.of("eventCode", "ORDER_COMPLETE"),
                ctx
        ).block();

        assertThat(result).isNotNull();
        assertThat(result.success()).isTrue();
        assertThat(result.nextNodeId()).isNull();
    }
}
