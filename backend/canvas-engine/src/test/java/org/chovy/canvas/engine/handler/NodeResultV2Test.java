package org.chovy.canvas.engine.handler;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class NodeResultV2Test {

    @Test
    void ok_uses_success_outcome_and_default_route() {
        NodeResult result = NodeResult.ok("next_node", Map.of("couponId", "c1"));

        assertThat(result.outcome()).isEqualTo(NodeOutcome.SUCCESS);
        assertThat(result.routes()).containsEntry("success", "next_node");
        assertThat(result.output()).containsEntry("couponId", "c1");
        assertThat(result.success()).isTrue();
    }

    @Test
    void suppressed_routes_to_suppressed_branch_without_engine_failure() {
        NodeResult result = NodeResult.suppressed("suppressed_node", "UNSUBSCRIBED", "用户已退订");

        assertThat(result.outcome()).isEqualTo(NodeOutcome.SUPPRESSED);
        assertThat(result.routes()).containsEntry("suppressed", "suppressed_node");
        assertThat(result.reasonCode()).isEqualTo("UNSUBSCRIBED");
        assertThat(result.success()).isTrue();
    }

    @Test
    void timeout_routes_to_timeout_branch_without_engine_failure() {
        NodeResult result = NodeResult.timeout("timeout_node", "WAIT_TIMEOUT", "等待目标事件超时");

        assertThat(result.outcome()).isEqualTo(NodeOutcome.TIMEOUT);
        assertThat(result.routes()).containsEntry("timeout", "timeout_node");
        assertThat(result.reasonMessage()).isEqualTo("等待目标事件超时");
        assertThat(result.success()).isTrue();
    }

    @Test
    void skipped_routes_to_skipped_branch() {
        NodeResult result = NodeResult.skipped("after_skip", "NODE_SKIPPED", "节点配置为跳过");

        assertThat(result.outcome()).isEqualTo(NodeOutcome.SKIPPED);
        assertThat(result.routes()).containsEntry("skipped", "after_skip");
        assertThat(result.success()).isTrue();
    }
}
