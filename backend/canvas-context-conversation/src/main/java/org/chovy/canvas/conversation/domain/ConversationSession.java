package org.chovy.canvas.conversation.domain;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public record ConversationSession(
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

    public ConversationSession {
        context = DomainMaps.copy(context);
    }

    public ConversationSession withId(Long id) {
        return new ConversationSession(id, tenantId, canvasId, versionId, executionId, userId, channel, provider,
                status, turnCount, context, lastMessageAt, expiresAt, createdAt, updatedAt);
    }

    public ConversationSession recorded(ConversationMessage message, LocalDateTime occurredAt) {
        Map<String, Object> merged = new LinkedHashMap<>(context);
        if (message.intent() != null) {
            merged.put("intent", message.intent());
        }
        if (message.textContent() != null) {
            merged.put("lastText", message.textContent());
        }
        merged.put("lastMessageId", message.id());
        return new ConversationSession(id, tenantId, canvasId, versionId, executionId, userId, channel, provider,
                status, turnCount + 1, merged, occurredAt, expiresAt, createdAt, occurredAt);
    }
}
