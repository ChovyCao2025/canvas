package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.context.NodeStatus;
import org.chovy.canvas.engine.handler.NodeResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * IfConditionHandler 单元测试。
 * 覆盖设计文档第 5.4、5.5节：条件评估、CONTAINS IN 语义、BigDecimal 数值比较。
 */
class IfConditionHandlerTest {

    private final IfConditionHandler handler = new IfConditionHandler();
    private ExecutionContext ctx;

    @BeforeEach
    void setUp() {
        ctx = new ExecutionContext();
        ctx.getFlatContext().put("userId", "user_001");
        ctx.getFlatContext().put("orderStatus", "PAID");
        ctx.getFlatContext().put("amount", "500");
        ctx.getFlatContext().put("marketIdentity", "newUser");
        ctx.getFlatContext().put("tripPhase", "待出行");
    }

    @Nested
    @DisplayName("EQ/NEQ 操作符")
    class EqNeq {
        @Test
        void eq_match() {
            Map<String, Object> rule = rule("orderStatus", "EQ", "PAID");
            assertThat(IfConditionHandler.evaluate(rule, ctx)).isTrue();
        }

        @Test
        void eq_mismatch() {
            Map<String, Object> rule = rule("orderStatus", "EQ", "CANCELLED");
            assertThat(IfConditionHandler.evaluate(rule, ctx)).isFalse();
        }

        @Test
        void neq_match() {
            Map<String, Object> rule = rule("orderStatus", "NEQ", "CANCELLED");
            assertThat(IfConditionHandler.evaluate(rule, ctx)).isTrue();
        }
    }

    @Nested
    @DisplayName("CONTAINS 操作符 — IN 语义（逗号分隔）")
    class ContainsIn {
        @Test
        void contains_in_match() {
            // "待出行,到达目的地" → tripPhase = "待出行" 应命中
            Map<String, Object> rule = rule("tripPhase", "CONTAINS", "待出行,到达目的地");
            assertThat(IfConditionHandler.evaluate(rule, ctx)).isTrue();
        }

        @Test
        void contains_in_miss() {
            Map<String, Object> rule = rule("tripPhase", "CONTAINS", "预出行,到达目的地");
            assertThat(IfConditionHandler.evaluate(rule, ctx)).isFalse();
        }

        @Test
        void contains_substring_without_comma() {
            // 无逗号时退化为子串匹配
            ctx.getFlatContext().put("desc", "机票订单已支付");
            Map<String, Object> rule = rule("desc", "CONTAINS", "已支付");
            assertThat(IfConditionHandler.evaluate(rule, ctx)).isTrue();
        }
    }

    @Nested
    @DisplayName("GT/LT 数值比较（BigDecimal，防字典序错误）")
    class NumericCompare {
        @Test
        void gt_numeric_match() {
            // amount=500, expected=50 → 数值比较 500>50 ✅（字典序"500"<"50" ❌）
            Map<String, Object> rule = rule("amount", "GT", "50");
            assertThat(IfConditionHandler.evaluate(rule, ctx)).isTrue();
        }

        @Test
        void lt_numeric_10_vs_9() {
            // 经典字典序陷阱："10" < "9" 字典序为真，但数值 10 > 9
            ctx.getFlatContext().put("count", "10");
            Map<String, Object> rule = rule("count", "LT", "9");
            assertThat(IfConditionHandler.evaluate(rule, ctx)).isFalse(); // 10 不小于 9
        }

        @Test
        void gte_match() {
            Map<String, Object> rule = rule("amount", "GTE", "500");
            assertThat(IfConditionHandler.evaluate(rule, ctx)).isTrue();
        }

        @Test
        void lte_mismatch() {
            Map<String, Object> rule = rule("amount", "LTE", "499");
            assertThat(IfConditionHandler.evaluate(rule, ctx)).isFalse();
        }
    }

    @Nested
    @DisplayName("IF_CONDITION 节点整体执行")
    class NodeExecution {
        @Test
        void all_rules_pass_goes_success() {
            Map<String, Object> config = Map.of(
                    "rules", List.of(rule("marketIdentity", "EQ", "newUser")),
                    "successNodeId", "node_coupon",
                    "failNodeId", "node_reach"
            );
            NodeResult result = handler.executeAsync(config, ctx).block();
            assertThat(result.success()).isTrue();
            assertThat(result.successNodeId()).isEqualTo("node_coupon");
            assertThat(result.failNodeId()).isNull();
        }

        @Test
        void rule_fail_goes_fail_branch() {
            Map<String, Object> config = Map.of(
                    "rules", List.of(rule("marketIdentity", "EQ", "vipUser")),
                    "successNodeId", "node_coupon",
                    "failNodeId", "node_reach"
            );
            NodeResult result = handler.executeAsync(config, ctx).block();
            assertThat(result.success()).isTrue();
            assertThat(result.successNodeId()).isNull();
            assertThat(result.failNodeId()).isEqualTo("node_reach");
        }
    }

    // ── helper ───────────────────────────────────────────────────────

    private static Map<String, Object> rule(String field, String operator, String value) {
        return Map.of("field", field, "operator", operator, "value", value, "isCustom", true);
    }
}
