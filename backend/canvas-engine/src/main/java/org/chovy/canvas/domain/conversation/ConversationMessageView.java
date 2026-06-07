package org.chovy.canvas.domain.conversation;

import java.time.LocalDateTime;
import java.util.Map;

public record ConversationMessageView(
        Long id,
        Long tenantId,
        Long sessionId,
        String direction,
        String messageType,
        String externalMessageId,
        String text,
        String intent,
        Map<String, Object> content,
        LocalDateTime createdAt) {
}
