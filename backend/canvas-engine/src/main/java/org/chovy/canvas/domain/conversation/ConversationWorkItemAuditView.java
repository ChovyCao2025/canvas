package org.chovy.canvas.domain.conversation;

import java.time.LocalDateTime;
import java.util.Map;

public record ConversationWorkItemAuditView(
        Long id,
        Long tenantId,
        Long workItemId,
        String eventType,
        String actor,
        Map<String, Object> oldValue,
        Map<String, Object> newValue,
        String note,
        LocalDateTime createdAt) {

    public ConversationWorkItemAuditView {
        oldValue = oldValue == null ? Map.of() : Map.copyOf(oldValue);
        newValue = newValue == null ? Map.of() : Map.copyOf(newValue);
    }
}
