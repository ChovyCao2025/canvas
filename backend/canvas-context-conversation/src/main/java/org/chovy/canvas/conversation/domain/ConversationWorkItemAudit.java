package org.chovy.canvas.conversation.domain;

import java.time.LocalDateTime;
import java.util.Map;

public record ConversationWorkItemAudit(
        Long id,
        Long tenantId,
        Long workItemId,
        String eventType,
        String actor,
        Map<String, Object> oldValue,
        Map<String, Object> newValue,
        String note,
        LocalDateTime createdAt) {

    public ConversationWorkItemAudit {
        oldValue = DomainMaps.copy(oldValue);
        newValue = DomainMaps.copy(newValue);
    }

    public ConversationWorkItemAudit withId(Long id) {
        return new ConversationWorkItemAudit(id, tenantId, workItemId, eventType, actor, oldValue, newValue, note, createdAt);
    }
}
