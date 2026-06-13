package org.chovy.canvas.conversation.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public record ConversationRoutingDecision(
        boolean routed,
        String routingStatus,
        Optional<ConversationRoutingAgent> agent,
        String assignedTeam,
        List<String> requiredSkills,
        String reason,
        LocalDateTime routedAt,
        LocalDateTime slaDueAt,
        String slaPolicyKey) {

    public ConversationRoutingDecision {
        agent = agent == null ? Optional.empty() : agent;
        requiredSkills = DomainMaps.copyList(requiredSkills);
    }
}
