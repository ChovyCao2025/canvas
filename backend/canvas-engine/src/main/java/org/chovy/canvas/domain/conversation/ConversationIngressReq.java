package org.chovy.canvas.domain.conversation;

import java.time.LocalDateTime;
import java.util.Map;

public record ConversationIngressReq(
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
