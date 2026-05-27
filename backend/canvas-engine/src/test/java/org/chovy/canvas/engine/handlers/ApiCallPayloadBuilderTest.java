package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.engine.context.ExecutionContext;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Api Call Payload Builder 测试类。
 *
 * <p>覆盖该后端组件在典型输入、边界条件和异常场景下的行为，确保重构或性能优化不会改变既有契约。
 * <p>测试代码只构造必要的依赖与数据，断言重点放在可观察结果、状态变更和关键副作用上。
 */
class ApiCallPayloadBuilderTest {

    @Test
    void wraps_params_without_context_payload() {
        ApiCallPayloadBuilder builder = new ApiCallPayloadBuilder(() -> 1625037472000L);
        ExecutionContext ctx = new ExecutionContext();
        ctx.setUserId("1917810");
        ctx.setExecutionId("exec-1");

        List<Map<String, Object>> payload = builder.build(
                Map.of("define_item1", "优惠券ID"),
                ctx,
                "api-node",
                false
        );

        assertThat(payload).containsExactly(Map.of(
                "params", Map.of("define_item1", "优惠券ID")
        ));
    }

    @Test
    void adds_environment_blocks_when_context_payload_is_enabled() {
        ApiCallPayloadBuilder builder = new ApiCallPayloadBuilder(() -> 1625037472000L);
        ExecutionContext ctx = new ExecutionContext();
        ctx.setUserId("1917810");
        ctx.setExecutionId("exec-1");

        List<Map<String, Object>> payload = builder.build(
                Map.of("define_item1", "优惠券ID"),
                ctx,
                "api-node",
                true
        );

        assertThat(payload).hasSize(1);
        Map<String, Object> item = payload.getFirst();
        assertThat(item.get("user_profile")).isEqualTo(Map.of(
                "target_type", "OPEN_ID",
                "target_id", "1917810",
                "customer_id", "1917810"
        ));
        assertThat(item.get("params")).isEqualTo(Map.of("define_item1", "优惠券ID"));
        assertThat(item.get("callback_params")).isEqualTo(Map.of(
                "webhook_id", "",
                "send_time", "1625037472000",
                "nodeId", "api-node",
                "instanceId", "exec-1",
                "batchId", "exec-1",
                "actionId", "exec-1:api-node",
                "customerId", "1917810"
        ));
        assertThat(item.get("process_info")).isEqualTo(Map.of(
                "processInstanceId", "exec-1",
                "processInstanceStartTime", "1625037472000",
                "processNodeInstanceId", "exec-1:api-node",
                "processNodeInstanceStartTime", "1625037472000",
                "groupName", ""
        ));
    }
}
