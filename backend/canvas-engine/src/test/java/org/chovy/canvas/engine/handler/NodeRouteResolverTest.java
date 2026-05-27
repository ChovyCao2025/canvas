package org.chovy.canvas.engine.handler;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Node Route Resolver 测试类。
 *
 * <p>覆盖该后端组件在典型输入、边界条件和异常场景下的行为，确保重构或性能优化不会改变既有契约。
 * <p>测试代码只构造必要的依赖与数据，断言重点放在可观察结果、状态变更和关键副作用上。
 */
class NodeRouteResolverTest {

    @Test
    void resolves_v2_route_before_legacy_route() {
        NodeResult result = NodeResult.timeout("timeout_node", "WAIT_TIMEOUT", "timeout");

        assertThat(NodeRouteResolver.resolveTargets(result)).containsExactly("timeout_node");
    }

    @Test
    void resolves_legacy_default_next_route() {
        NodeResult result = NodeResult.ok("next_node", Map.of());

        assertThat(NodeRouteResolver.resolveTargets(result)).containsExactly("next_node");
    }

    @Test
    void resolves_legacy_branch_map_values() {
        NodeResult result = NodeResult.multiNext(Map.of("A", "node_a", "B", "node_b"), null);

        assertThat(NodeRouteResolver.resolveTargets(result)).containsExactlyInAnyOrder("node_a", "node_b");
    }

    @Test
    void ignores_blank_routes() {
        NodeResult result = NodeResult.multiNext(Map.of("A", "", "B", "node_b"), null);

        assertThat(NodeRouteResolver.resolveTargets(result)).containsExactly("node_b");
    }

    @Test
    void resolves_reserved_else_after_named_branch_else() {
        Map<String, String> branches = new LinkedHashMap<>();
        branches.put("else", "branch_else");
        NodeResult result = NodeResult.multiNext(branches, "fallback_else");

        assertThat(NodeRouteResolver.resolveTargets(result)).containsExactly("branch_else", "fallback_else");
    }

    @Test
    void resolver_moves_reserved_else_route_to_the_end() {
        Map<String, String> routes = new LinkedHashMap<>();
        routes.put("__else", "fallback_else");
        routes.put("A", "node_a");
        routes.put("B", "node_b");

        NodeResult result = new NodeResult(
                null, null, null, null, null, Map.of(), true, null, false,
                NodeOutcome.SUCCESS, routes, null, null, null
        );

        assertThat(NodeRouteResolver.resolveTargets(result)).containsExactly("node_a", "node_b", "fallback_else");
    }

    @Test
    void resolves_priority_branches_by_route_identity_not_target_value() {
        Map<String, String> routes = new LinkedHashMap<>();
        routes.put("__else", "shared_node");
        routes.put("A", "shared_node");
        routes.put("B", "node_b");

        NodeResult result = new NodeResult(
                null, null, null, null, null, Map.of(), true, null, false,
                NodeOutcome.SUCCESS, routes, null, null, null
        );

        assertThat(NodeRouteResolver.resolvePriorityBranchTargets(result)).containsExactly("shared_node", "node_b");
        assertThat(NodeRouteResolver.resolveFallbackTarget(result)).isEqualTo("shared_node");
    }

    @Test
    void ignores_blank_priority_fallback() {
        NodeResult result = NodeResult.multiNext(Map.of("A", "node_a"), "");

        assertThat(NodeRouteResolver.resolvePriorityBranchTargets(result)).containsExactly("node_a");
        assertThat(NodeRouteResolver.resolveFallbackTarget(result)).isNull();
    }
}
