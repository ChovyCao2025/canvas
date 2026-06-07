package org.chovy.canvas.domain.conversation;

import java.time.LocalDateTime;
import java.util.Map;

public record ConversationSopTaskView(
        Long id,
        Long tenantId,
        Long workItemId,
        String taskKey,
        String title,
        String status,
        String assignee,
        LocalDateTime dueAt,
        String completedBy,
        LocalDateTime completedAt,
        Map<String, Object> metadata,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public ConversationSopTaskView {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
