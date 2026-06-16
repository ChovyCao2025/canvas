package org.chovy.canvas.conversation.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证会话工单路由策略的单元测试。
 */
class ConversationRoutingPolicyTest {

    /**
     * 固定路由决策时间。
     */
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 6, 6, 11, 0);

    /**
     * 验证策略选择满足技能要求且负载最低的坐席。
     */
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

    /**
     * 验证没有坐席容量时返回未路由决策。
     */
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

    /**
     * 构造指定优先级的测试工单。
     *
     * @param priority 工单优先级
     * @return 测试工单
     */
    private static ConversationWorkItem workItem(String priority) {
        return new ConversationWorkItem(
                50L, 7L, 10L, 20L, "user-1", "WEB_CHAT", "WIDGET",
                "WEB_CHAT conversation with user-1", "OPEN", priority, null, null,
                "CONVERSATION", null, null, NOW.minusMinutes(5), null,
                List.of(), Map.of(), "UNROUTED", List.of(), null, null, null,
                NOW.minusMinutes(10), NOW.minusMinutes(5));
    }

    /**
     * 构造匹配 WEB_CHAT 高优先级工单的测试规则。
     *
     * @return 测试路由规则
     */
    private static ConversationRoutingRule rule() {
        return new ConversationRoutingRule(
                70L, 7L, "vip-sales", "WEB_CHAT", "HIGH",
                List.of("sales", "vip"), "sales", 30, true, 10, Map.of(),
                "manager", NOW.minusDays(1), NOW.minusDays(1));
    }

    /**
     * 构造测试路由坐席。
     *
     * @param key 坐席业务键
     * @param status 坐席状态
     * @param maxCapacity 最大容量
     * @param currentLoad 当前负载
     * @param skills 技能列表
     * @return 测试坐席
     */
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
