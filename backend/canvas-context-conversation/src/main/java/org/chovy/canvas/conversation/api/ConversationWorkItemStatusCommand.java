package org.chovy.canvas.conversation.api;

import java.time.LocalDateTime;

public record ConversationWorkItemStatusCommand(
        String status,
        String priority,
        LocalDateTime nextFollowUpAt,
        String note) {
}
