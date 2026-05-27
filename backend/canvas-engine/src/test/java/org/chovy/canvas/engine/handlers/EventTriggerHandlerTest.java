package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeResult;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Event Trigger 测试类。
 *
 * <p>覆盖该后端组件在典型输入、边界条件和异常场景下的行为，确保重构或性能优化不会改变既有契约。
 * <p>测试代码只构造必要的依赖与数据，断言重点放在可观察结果、状态变更和关键副作用上。
 */
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
