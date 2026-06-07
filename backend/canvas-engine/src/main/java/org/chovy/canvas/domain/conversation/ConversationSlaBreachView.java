package org.chovy.canvas.domain.conversation;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public record ConversationSlaBreachView(Long id,
                                        Long tenantId,
                                        Long workItemId,
                                        String breachType,
                                        String severity,
                                        String status,
                                        String escalationTarget,
                                        String reason,
                                        LocalDateTime dueAt,
                                        LocalDateTime breachedAt,
                                        String resolvedBy,
                                        LocalDateTime resolvedAt,
                                        Map<String, Object> metadata,
                                        LocalDateTime createdAt,
                                        LocalDateTime updatedAt) {

    public ConversationSlaBreachView {
        metadata = sanitize(metadata);
    }

    private static Map<String, Object> sanitize(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return Map.copyOf(result);
    }
}
