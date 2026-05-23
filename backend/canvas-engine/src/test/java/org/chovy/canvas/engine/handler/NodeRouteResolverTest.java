package org.chovy.canvas.engine.handler;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

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
}
