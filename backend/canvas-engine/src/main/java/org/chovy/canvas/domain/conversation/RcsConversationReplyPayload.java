package org.chovy.canvas.domain.conversation;

import java.time.LocalDateTime;
import java.util.Map;

public record RcsConversationReplyPayload(
        Long canvasId,
        Long versionId,
        String executionId,
        String userId,
        String provider,
        String agentId,
        String conversationId,
        String externalMessageId,
        String eventId,
        String text,
        String suggestionReplyId,
        String suggestionText,
        String intent,
        Map<String, Object> attributes,
        LocalDateTime occurredAt) implements ProviderConversationReplyPayload {
}
