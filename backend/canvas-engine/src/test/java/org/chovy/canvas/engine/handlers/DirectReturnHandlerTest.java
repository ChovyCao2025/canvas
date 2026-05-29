package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DirectReturnHandler 单元测试。
 *
 * <p>覆盖直调返回节点的配置契约，确保前端 schema 与示例模板生成的返回映射都能被执行端识别。
 */
class DirectReturnHandlerTest {

    private final DirectReturnHandler handler = new DirectReturnHandler();

    @Test
    void builds_terminal_output_from_data_key_and_template_context_reference() {
        ExecutionContext ctx = ctx();
        ctx.putNodeOutput("api", Map.of("user.level", "VIP", "score", 88));

        NodeResult result = handler.executeAsync(Map.of(
                "data", List.of(
                        Map.of("key", "userLevel", "value", "${user.level}"),
                        Map.of("key", "label", "value", "fixed")
                )
        ), ctx).block();

        assertThat(result.success()).isTrue();
        assertThat(result.nextNodeId()).isNull();
        assertThat(result.output()).containsEntry("userLevel", "VIP")
                .containsEntry("label", "fixed");
    }

    @Test
    void keeps_compatibility_with_legacy_bizData_name_and_context_value_type() {
        ExecutionContext ctx = ctx();
        ctx.setTriggerPayload(Map.of("orderId", "ORDER-1"));

        NodeResult result = handler.executeAsync(Map.of(
                "bizData", List.of(
                        Map.of("name", "order", "valueType", "CONTEXT", "value", "orderId")
                )
        ), ctx).block();

        assertThat(result.output()).containsEntry("order", "ORDER-1");
    }

    private static ExecutionContext ctx() {
        ExecutionContext ctx = new ExecutionContext();
        ctx.setExecutionId("exec-1");
        ctx.setCanvasId(10L);
        ctx.setUserId("user-1");
        return ctx;
    }
}
