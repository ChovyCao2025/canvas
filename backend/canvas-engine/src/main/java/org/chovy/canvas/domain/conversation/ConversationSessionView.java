package org.chovy.canvas.domain.conversation;

import java.time.LocalDateTime;
import java.util.Map;

public record ConversationSessionView(
        Long id,
        Long tenantId,
        Long canvasId,
        Long versionId,
        String executionId,
        String userId,
        String channel,
        String provider,
        String status,
        int turnCount,
        Map<String, Object> context,
        LocalDateTime lastMessageAt,
        LocalDateTime expiresAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
