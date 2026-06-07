package org.chovy.canvas.domain.conversation;

import java.time.LocalDateTime;
import java.util.Map;

public record ConversationSopTaskCommand(
        String taskKey,
        String title,
        String assignee,
        LocalDateTime dueAt,
        Map<String, Object> metadata) {

    public ConversationSopTaskCommand {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
