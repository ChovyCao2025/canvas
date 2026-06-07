package org.chovy.canvas.domain.conversation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record ConversationWorkItemView(
        Long id,
        Long tenantId,
        Long sessionId,
        Long contactProfileId,
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
        String slaPolicyKey,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public ConversationWorkItemView {
        tags = tags == null ? List.of() : List.copyOf(tags);
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        requiredSkills = requiredSkills == null ? List.of() : List.copyOf(requiredSkills);
    }

    public ConversationWorkItemView(Long id,
                                    Long tenantId,
                                    Long sessionId,
                                    Long contactProfileId,
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
                                    LocalDateTime createdAt,
                                    LocalDateTime updatedAt) {
        this(id, tenantId, sessionId, contactProfileId, userId, channel, provider, subject, status, priority,
                assignedTo, assignedTeam, source, slaDueAt, nextFollowUpAt, lastCustomerMessageAt,
                lastOperatorActivityAt, tags, attributes, "UNROUTED", List.of(), null, null, null,
                createdAt, updatedAt);
    }
}
