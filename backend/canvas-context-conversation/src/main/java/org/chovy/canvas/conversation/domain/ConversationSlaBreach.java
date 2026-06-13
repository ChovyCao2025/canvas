package org.chovy.canvas.conversation.domain;

import java.time.LocalDateTime;
import java.util.Map;

public record ConversationSlaBreach(
        Long id,
        Long tenantId,
        Long workItemId,
        String breachType,
        String severity,
        String status,
        String escalationTarget,
        String reason,
        LocalDateTime dueAt,
        LocalDateTime breachedAt,
        String resolvedBy,
        LocalDateTime resolvedAt,
        Map<String, Object> metadata,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public ConversationSlaBreach {
        metadata = DomainMaps.copy(metadata);
    }

    public ConversationSlaBreach withId(Long id) {
        return new ConversationSlaBreach(id, tenantId, workItemId, breachType, severity, status,
                escalationTarget, reason, dueAt, breachedAt, resolvedBy, resolvedAt, metadata, createdAt, updatedAt);
    }
}
