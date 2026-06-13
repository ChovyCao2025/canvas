package org.chovy.canvas.conversation.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ConversationRoutingPolicyTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 6, 6, 11, 0);

    @Test
    void choosesLowestLoadAvailableAgentWithAllRequiredSkills() {
        ConversationRoutingPolicy policy = new ConversationRoutingPolicy();

        ConversationRoutingDecision decision = policy.route(
                workItem("HIGH"),
                List.of(rule()),
                List.of(
                        agent("bob", "AVAILABLE", 3, 2, List.of("sales", "vip")),
                        agent("alice", "AVAILABLE", 3, 1, List.of("sales", "vip")),
                        agent("carl", "AVAILABLE", 3, 0, List.of("sales"))),
                new ConversationRouteRequest(List.of(), null, null, "route now"),
                NOW);

        assertThat(decision.routed()).isTrue();
        assertThat(decision.agent().orElseThrow().agentKey()).isEqualTo("alice");
        assertThat(decision.requiredSkills()).containsExactly("sales", "vip");
        assertThat(decision.slaDueAt()).isEqualTo(NOW.plusMinutes(30));
        assertThat(decision.reason()).contains("matched rule vip-sales");
    }

    @Test
    void returnsMissWhenNoAgentHasCapacity() {
        ConversationRoutingPolicy policy = new ConversationRoutingPolicy();

        ConversationRoutingDecision decision = policy.route(
                workItem("HIGH"),
                List.of(rule()),
                List.of(agent("alice", "AVAILABLE", 1, 1, List.of("sales", "vip"))),
                new ConversationRouteRequest(List.of("sales", "vip"), "sales", 30, "route now"),
                NOW);

        assertThat(decision.routed()).isFalse();
        assertThat(decision.routingStatus()).isEqualTo("UNROUTED");
        assertThat(decision.reason()).contains("no available agent");
    }

    private static ConversationWorkItem workItem(String priority) {
        return new ConversationWorkItem(
                50L, 7L, 10L, 20L, "user-1", "WEB_CHAT", "WIDGET",
                "WEB_CHAT conversation with user-1", "OPEN", priority, null, null,
                "CONVERSATION", null, null, NOW.minusMinutes(5), null,
                List.of(), Map.of(), "UNROUTED", List.of(), null, null, null,
                NOW.minusMinutes(10), NOW.minusMinutes(5));
    }

    private static ConversationRoutingRule rule() {
        return new ConversationRoutingRule(
                70L, 7L, "vip-sales", "WEB_CHAT", "HIGH",
                List.of("sales", "vip"), "sales", 30, true, 10, Map.of(),
                "manager", NOW.minusDays(1), NOW.minusDays(1));
    }

    private static ConversationRoutingAgent agent(String key,
                                                  String status,
                                                  int maxCapacity,
                                                  int currentLoad,
                                                  List<String> skills) {
        return new ConversationRoutingAgent(
                (long) key.hashCode(), 7L, key, key, "sales", status, maxCapacity, currentLoad,
                skills, Map.of(), "manager", NOW.minusDays(1), NOW.minusDays(1));
    }
}
