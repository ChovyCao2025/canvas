package org.chovy.canvas.conversation.api;

import java.time.LocalDateTime;
import java.util.Map;

public record ConversationInboundCommand(
        Long tenantId,
        Long canvasId,
        Long versionId,
        String executionId,
        String userId,
        String channel,
        String provider,
        String externalMessageId,
        String eventId,
        String messageType,
        String text,
        String intent,
        Map<String, Object> attributes,
        LocalDateTime occurredAt) {
}
