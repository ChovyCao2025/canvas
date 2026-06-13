package org.chovy.canvas.conversation.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record ConversationRoutingRule(
        Long id,
        Long tenantId,
        String ruleKey,
        String channel,
        String minPriority,
        List<String> requiredSkills,
        String targetTeam,
        int slaMinutes,
        boolean enabled,
        int sortOrder,
        Map<String, Object> metadata,
        String createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public ConversationRoutingRule {
        requiredSkills = DomainMaps.copyList(requiredSkills);
        metadata = DomainMaps.copy(metadata);
    }

    public ConversationRoutingRule withId(Long id) {
        return new ConversationRoutingRule(id, tenantId, ruleKey, channel, minPriority, requiredSkills,
                targetTeam, slaMinutes, enabled, sortOrder, metadata, createdBy, createdAt, updatedAt);
    }

    boolean matches(ConversationWorkItem item) {
        return enabled
                && (channel == null || channel.equals(item.channel()))
                && priorityRank(item.priority()) >= priorityRank(minPriority);
    }

    private static int priorityRank(String priority) {
        return switch (priority == null ? "NORMAL" : priority) {
            case "LOW" -> 1;
            case "HIGH" -> 3;
            case "URGENT" -> 4;
            default -> 2;
        };
    }
}
