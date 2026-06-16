package org.chovy.canvas.conversation.domain;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * 根据工单、规则、坐席和人工覆盖参数计算路由决策。
 */
public class ConversationRoutingPolicy {

    /**
     * 对工单执行一次路由决策。
     *
     * @param item 待路由工单
     * @param rules 租户内可用路由规则
     * @param agents 可承接工单的坐席列表
     * @param request 人工路由覆盖参数
     * @param routedAt 路由决策发生时间
     * @return 路由决策结果
     */
    public ConversationRoutingDecision route(ConversationWorkItem item,
                                             List<ConversationRoutingRule> rules,
                                             List<ConversationRoutingAgent> agents,
                                             ConversationRouteRequest request,
                                             LocalDateTime routedAt) {
        if ("RESOLVED".equals(item.status())) {
            throw new IllegalStateException("resolved work item cannot be routed");
        }
        // 先按规则顺序选出第一条匹配规则，再让人工请求覆盖技能、团队和 SLA。
        ConversationRoutingRule rule = rules.stream()
                .filter(candidate -> candidate.matches(item))
                .sorted(Comparator.comparingInt(ConversationRoutingRule::sortOrder))
                .findFirst()
                .orElse(null);
        List<String> requiredSkills = request != null && !request.requiredSkills().isEmpty()
                ? ConversationText.normalizeKeys(request.requiredSkills())
                : rule == null ? List.of() : rule.requiredSkills();
        String targetTeam = request != null && request.targetTeam() != null
                ? ConversationText.optionalKey(request.targetTeam())
                : rule == null ? null : rule.targetTeam();
        int slaMinutes = request != null && request.slaMinutes() != null
                ? Math.max(1, request.slaMinutes())
                : rule == null ? 60 : Math.max(1, rule.slaMinutes());
        // 同等条件下选择负载最低、业务键最小的坐席，使路由结果可预测。
        ConversationRoutingAgent agent = agents.stream()
                .filter(candidate -> candidate.canHandle(targetTeam, requiredSkills))
                .min(Comparator.comparingInt(ConversationRoutingAgent::currentLoad)
                        .thenComparing(ConversationRoutingAgent::agentKey))
                .orElse(null);
        if (agent == null) {
            return new ConversationRoutingDecision(false, "UNROUTED", java.util.Optional.empty(), targetTeam,
                    requiredSkills, "no available agent for required skills", routedAt, null,
                    rule == null ? null : rule.ruleKey());
        }
        LocalDateTime slaDueAt = routedAt.plusMinutes(slaMinutes);
        String reason = "matched rule " + (rule == null ? "default" : rule.ruleKey()) + " to agent " + agent.agentKey();
        return new ConversationRoutingDecision(true, "ROUTED", java.util.Optional.of(agent), agent.teamKey(),
                requiredSkills, reason, routedAt, slaDueAt, rule == null ? null : rule.ruleKey());
    }
}
