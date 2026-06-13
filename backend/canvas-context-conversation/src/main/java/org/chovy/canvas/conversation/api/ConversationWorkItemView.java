package org.chovy.canvas.conversation.api;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record ConversationWorkItemView(
        Long id,
        Long tenantId,
        Long sessionId,
        String userId,
        String channel,
        String provider,
        String subject,
        String status,
        String priority,
        String assignedTo,
        String assignedTeam,
        String source,
        LocalDateTime slaDueAt,
        LocalDateTime nextFollowUpAt,
        LocalDateTime lastCustomerMessageAt,
        LocalDateTime lastOperatorActivityAt,
        List<String> tags,
        Map<String, Object> attributes,
        String routingStatus,
        List<String> requiredSkills,
        String routingReason,
        LocalDateTime routedAt,
        String slaPolicyKey) {
}
