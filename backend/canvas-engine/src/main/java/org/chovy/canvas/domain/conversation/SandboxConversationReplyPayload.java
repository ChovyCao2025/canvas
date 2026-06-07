package org.chovy.canvas.domain.conversation;

import java.util.Map;

public record SandboxConversationReplyPayload(
        Long canvasId,
        Long versionId,
        String executionId,
        String userId,
        String externalMessageId,
        String eventId,
        String text,
        String intent,
        Map<String, Object> attributes) {
}
