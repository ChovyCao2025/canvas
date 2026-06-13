package org.chovy.canvas.conversation.domain;

import java.time.LocalDateTime;
import java.util.Map;

public record ConversationMessage(
        Long id,
        Long tenantId,
        Long sessionId,
        String direction,
        String messageType,
        String externalMessageId,
        String idempotencyKey,
        Map<String, Object> content,
        String textContent,
        String intent,
        boolean processed,
        LocalDateTime createdAt) {

    public ConversationMessage {
        content = DomainMaps.copy(content);
    }

    public ConversationMessage withId(Long id) {
        return new ConversationMessage(id, tenantId, sessionId, direction, messageType, externalMessageId,
                idempotencyKey, content, textContent, intent, processed, createdAt);
    }
}
