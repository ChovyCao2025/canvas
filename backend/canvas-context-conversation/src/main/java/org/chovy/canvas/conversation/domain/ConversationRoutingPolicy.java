package org.chovy.canvas.conversation.domain;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

public class ConversationRoutingPolicy {

    public ConversationRoutingDecision route(ConversationWorkItem item,
                                             List<ConversationRoutingRule> rules,
                                             List<ConversationRoutingAgent> agents,
                                             ConversationRouteRequest request,
                                             LocalDateTime routedAt) {
        if ("RESOLVED".equals(item.status())) {
            throw new IllegalStateException("resolved work item cannot be routed");
        }
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
