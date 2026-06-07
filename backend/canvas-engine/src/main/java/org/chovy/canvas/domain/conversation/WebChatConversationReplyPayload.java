package org.chovy.canvas.domain.conversation;

import java.time.LocalDateTime;
import java.util.Map;

public record WebChatConversationReplyPayload(
        Long canvasId,
        Long versionId,
        String executionId,
        String userId,
        String webChatSessionId,
        String provider,
        String externalMessageId,
        String eventId,
        String text,
        String actionId,
        String actionLabel,
        String intent,
        Map<String, Object> attributes,
        LocalDateTime occurredAt) implements ProviderConversationReplyPayload {
}
