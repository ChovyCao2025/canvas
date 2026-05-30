package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeResult;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class CommitActionHandlerTest {

    @Test
    void delegatesCouponActionToCouponExecutor() {
        CapturingHandler coupon = new CapturingHandler();
        CommitActionHandler handler = new CommitActionHandler(coupon, new CapturingHandler());
        ExecutionContext ctx = context();

        NodeResult result = handler.executeAsync(Map.of(
                MapFieldKeys.ACTION_TYPE, NodeType.COUPON,
                MapFieldKeys.NODE_ID_INTERNAL, "coupon-a",
                MapFieldKeys.NEXT_NODE_ID, "next"
        ), ctx).block();

        assertThat(result.success()).isTrue();
        assertThat(result.nextNodeId()).isEqualTo("next");
        assertThat(coupon.lastConfig().get(MapFieldKeys.NODE_ID_INTERNAL)).isEqualTo("coupon-a");
        assertThat(handler.isBenefitNode()).isTrue();
    }

    @Test
    void delegatesPointsActionToPointsExecutor() {
        CapturingHandler points = new CapturingHandler();
        CommitActionHandler handler = new CommitActionHandler(new CapturingHandler(), points);
        ExecutionContext ctx = context();

        NodeResult result = handler.executeAsync(Map.of(
                MapFieldKeys.ACTION_TYPE, NodeType.POINTS_OPERATION,
                MapFieldKeys.NODE_ID_INTERNAL, "points-a",
                MapFieldKeys.NEXT_NODE_ID, "next"
        ), ctx).block();

        assertThat(result.success()).isTrue();
        assertThat(points.lastConfig().get(MapFieldKeys.NODE_ID_INTERNAL)).isEqualTo("points-a");
    }

    @Test
    void failsUnknownActionType() {
        CommitActionHandler handler = new CommitActionHandler(new CapturingHandler(), new CapturingHandler());

        NodeResult result = handler.executeAsync(Map.of(MapFieldKeys.ACTION_TYPE, "UNKNOWN"), context()).block();

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).contains("未知提交动作类型");
    }

    private ExecutionContext context() {
        ExecutionContext ctx = new ExecutionContext();
        ctx.setExecutionId("exec-commit-handler-test");
        ctx.setUserId("user-1");
        return ctx;
    }

    static class CapturingHandler implements NodeHandler {
        private final AtomicReference<Map<String, Object>> lastConfig = new AtomicReference<>();

        @Override
        public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
            lastConfig.set(config);
            return Mono.just(NodeResult.ok((String) config.get(MapFieldKeys.NEXT_NODE_ID), Map.of()));
        }

        Map<String, Object> lastConfig() {
            return lastConfig.get();
        }
    }
}
