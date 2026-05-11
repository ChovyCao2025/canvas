package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeResult;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * SelectorHandler 单元测试。
 * 覆盖：分支按序匹配、全不命中走 else、全不命中无 else 自然结束。
 */
class SelectorHandlerTest {

    private final SelectorHandler handler = new SelectorHandler();

    @Test
    @DisplayName("命中第一个分支")
    void first_branch_match() {
        ExecutionContext ctx = new ExecutionContext();
        ctx.getFlatContext().put("tripPhase", "待出行");

        Map<String, Object> config = Map.of(
                "branches", List.of(
                        branch("待出行,到达目的地", "node_hotel"),
                        branch("预出行", "node_shuttle")
                ),
                "elseNodeId", "node_ticket"
        );

        NodeResult r = handler.execute(config, ctx);
        assertThat(r.nextNodeId()).isEqualTo("node_hotel");
    }

    @Test
    @DisplayName("第一个不命中，第二个命中")
    void second_branch_match() {
        ExecutionContext ctx = new ExecutionContext();
        ctx.getFlatContext().put("tripPhase", "预出行");

        Map<String, Object> config = Map.of(
                "branches", List.of(
                        branch("待出行,到达目的地", "node_hotel"),
                        branch("预出行", "node_shuttle")
                ),
                "elseNodeId", "node_ticket"
        );

        NodeResult r = handler.execute(config, ctx);
        assertThat(r.nextNodeId()).isEqualTo("node_shuttle");
    }

    @Test
    @DisplayName("全不命中走 elseNodeId")
    void all_miss_goes_else() {
        ExecutionContext ctx = new ExecutionContext();
        ctx.getFlatContext().put("tripPhase", "已取消");

        Map<String, Object> config = Map.of(
                "branches", List.of(branch("待出行", "node_hotel")),
                "elseNodeId", "node_ticket"
        );

        NodeResult r = handler.execute(config, ctx);
        assertThat(r.nextNodeId()).isEqualTo("node_ticket");
    }

    @Test
    @DisplayName("全不命中无 else → 流程自然结束（SUCCESS，无后续节点）")
    void all_miss_no_else_terminal() {
        ExecutionContext ctx = new ExecutionContext();
        ctx.getFlatContext().put("tripPhase", "已取消");

        Map<String, Object> config = Map.of(
                "branches", List.of(branch("待出行", "node_hotel"))
                // 无 elseNodeId
        );

        NodeResult r = handler.execute(config, ctx);
        assertThat(r.success()).isTrue();
        assertThat(r.nextNodeId()).isNull();
    }

    private Map<String, Object> branch(String value, String nextNodeId) {
        return Map.of(
                "strategyRelation", "AND",
                "conditions", List.of(Map.of(
                        "field", "tripPhase", "operator", "CONTAINS",
                        "value", value, "isCustom", true)),
                "nextNodeId", nextNodeId
        );
    }
}
