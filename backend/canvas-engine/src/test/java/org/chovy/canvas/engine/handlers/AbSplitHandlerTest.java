package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeResult;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * AbSplitHandler 单元测试。
 * 覆盖设计文档：Hash 确定性分流、等比分桶、相同 userId+experimentKey 永远同组。
 */
class AbSplitHandlerTest {

    private final AbSplitHandler handler = new AbSplitHandler();

    @Test
    @DisplayName("相同 userId+experimentKey 永远落入同一分组（确定性）")
    void same_input_same_group() {
        ExecutionContext ctx = ctx("user_123");
        List<Map<String, Object>> groups = List.of(
                Map.of("groupKey", "A", "nextNodeId", "node_hotel"),
                Map.of("groupKey", "B", "nextNodeId", "node_ticket")
        );
        Map<String, Object> config = Map.of("experimentKey", "exp_001", "groups", groups);

        NodeResult r1 = handler.execute(config, ctx);
        NodeResult r2 = handler.execute(config, ctx);
        NodeResult r3 = handler.execute(config, ctx);

        assertThat(r1.output().get("abGroup"))
                .isEqualTo(r2.output().get("abGroup"))
                .isEqualTo(r3.output().get("abGroup"));
    }

    @Test
    @DisplayName("等比分桶：2组各覆盖 50% bucket")
    void equal_distribution_two_groups() {
        List<Map<String, Object>> groups = List.of(
                Map.of("groupKey", "A", "nextNodeId", "node_a"),
                Map.of("groupKey", "B", "nextNodeId", "node_b")
        );
        Map<String, Object> config = Map.of("experimentKey", "exp_002", "groups", groups);

        // 用多个不同 userId 测试，期望两组都有命中
        long countA = 0, countB = 0;
        for (int i = 0; i < 100; i++) {
            NodeResult r = handler.execute(config, ctx("user_" + i));
            String g = (String) r.output().get("abGroup");
            if ("A".equals(g)) countA++;
            else countB++;
        }
        // 等比分桶：每组约 50，允许一定偏差
        assertThat(countA).isBetween(35L, 65L);
        assertThat(countB).isBetween(35L, 65L);
    }

    @Test
    @DisplayName("空 groups 时返回 terminal")
    void empty_groups_returns_terminal() {
        Map<String, Object> config = Map.of("experimentKey", "exp_003", "groups", List.of());
        NodeResult r = handler.execute(config, ctx("user_x"));
        assertThat(r.success()).isTrue();
        assertThat(r.nextNodeId()).isNull();
    }

    private ExecutionContext ctx(String userId) {
        ExecutionContext ctx = new ExecutionContext();
        ctx.setUserId(userId);
        return ctx;
    }
}
