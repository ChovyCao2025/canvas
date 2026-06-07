package org.chovy.canvas.domain.conversation;

import java.time.LocalDateTime;
import java.util.Map;

public record SocialDmConversationReplyPayload(
        Long canvasId,
        Long versionId,
        String executionId,
        String userId,
        String platform,
        String provider,
        String pageId,
        String threadId,
        String externalMessageId,
        String eventId,
        String text,
        String quickReplyPayload,
        String quickReplyTitle,
        String intent,
        Map<String, Object> attributes,
        LocalDateTime occurredAt) implements ProviderConversationReplyPayload {
}
