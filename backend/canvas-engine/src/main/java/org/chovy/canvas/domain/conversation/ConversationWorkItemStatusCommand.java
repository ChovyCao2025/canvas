package org.chovy.canvas.domain.conversation;

import java.time.LocalDateTime;

public record ConversationWorkItemStatusCommand(
        String status,
        String priority,
        LocalDateTime nextFollowUpAt,
        String note) {
}
