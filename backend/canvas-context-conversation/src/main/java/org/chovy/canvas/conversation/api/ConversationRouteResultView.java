package org.chovy.canvas.conversation.api;

import java.time.LocalDateTime;
import java.util.List;

public record ConversationRouteResultView(
        Long tenantId,
        Long workItemId,
        boolean routed,
        String routingStatus,
        String assignedTo,
        String assignedTeam,
        List<String> requiredSkills,
        String routingReason,
        LocalDateTime routedAt,
        LocalDateTime slaDueAt,
        String slaPolicyKey) {
}
