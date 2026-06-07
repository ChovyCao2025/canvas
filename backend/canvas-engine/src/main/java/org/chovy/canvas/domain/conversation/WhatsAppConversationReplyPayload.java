package org.chovy.canvas.domain.conversation;

import java.time.LocalDateTime;
import java.util.Map;

public record WhatsAppConversationReplyPayload(
        Long canvasId,
        Long versionId,
        String executionId,
        String userId,
        String provider,
        String externalMessageId,
        String eventId,
        String text,
        String interactiveReplyId,
        String interactiveReplyTitle,
        String intent,
        Map<String, Object> attributes,
        LocalDateTime occurredAt) implements ProviderConversationReplyPayload {
}
