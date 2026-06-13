package org.chovy.canvas.conversation.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record ConversationWorkItem(
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

    public ConversationWorkItem {
        tags = DomainMaps.copyList(tags);
        attributes = DomainMaps.copy(attributes);
        requiredSkills = DomainMaps.copyList(requiredSkills);
    }

    public ConversationWorkItem withId(Long id) {
        return new ConversationWorkItem(id, tenantId, sessionId, contactProfileId, userId, channel, provider,
                subject, status, priority, assignedTo, assignedTeam, source, slaDueAt, nextFollowUpAt,
                lastCustomerMessageAt, lastOperatorActivityAt, tags, attributes, routingStatus, requiredSkills,
                routingReason, routedAt, slaPolicyKey, createdAt, updatedAt);
    }

    public ConversationWorkItem withAssignment(String assignedTo, String assignedTeam, LocalDateTime updatedAt) {
        return new ConversationWorkItem(id, tenantId, sessionId, contactProfileId, userId, channel, provider,
                subject, status, priority, assignedTo, assignedTeam, source, slaDueAt, nextFollowUpAt,
                lastCustomerMessageAt, updatedAt, tags, attributes, routingStatus, requiredSkills,
                routingReason, routedAt, slaPolicyKey, createdAt, updatedAt);
    }

    public ConversationWorkItem withStatus(String status, String priority, LocalDateTime nextFollowUpAt, LocalDateTime updatedAt) {
        return new ConversationWorkItem(id, tenantId, sessionId, contactProfileId, userId, channel, provider,
                subject, status, priority, assignedTo, assignedTeam, source, slaDueAt, nextFollowUpAt,
                lastCustomerMessageAt, updatedAt, tags, attributes, routingStatus, requiredSkills,
                routingReason, routedAt, slaPolicyKey, createdAt, updatedAt);
    }

    public ConversationWorkItem withRouting(String routingStatus,
                                            String assignedTo,
                                            String assignedTeam,
                                            List<String> requiredSkills,
                                            String routingReason,
                                            LocalDateTime routedAt,
                                            LocalDateTime slaDueAt) {
        return new ConversationWorkItem(id, tenantId, sessionId, contactProfileId, userId, channel, provider,
                subject, status, priority, assignedTo, assignedTeam, source, slaDueAt, nextFollowUpAt,
                lastCustomerMessageAt, lastOperatorActivityAt, tags, attributes, routingStatus, requiredSkills,
                routingReason, routedAt, slaPolicyKey, createdAt, routedAt == null ? updatedAt : routedAt);
    }

    public ConversationWorkItem withSlaPolicy(String slaPolicyKey) {
        return new ConversationWorkItem(id, tenantId, sessionId, contactProfileId, userId, channel, provider,
                subject, status, priority, assignedTo, assignedTeam, source, slaDueAt, nextFollowUpAt,
                lastCustomerMessageAt, lastOperatorActivityAt, tags, attributes, routingStatus, requiredSkills,
                routingReason, routedAt, slaPolicyKey, createdAt, updatedAt);
    }
}
