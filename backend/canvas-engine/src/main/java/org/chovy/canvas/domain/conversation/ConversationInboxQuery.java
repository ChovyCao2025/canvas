package org.chovy.canvas.domain.conversation;

public record ConversationInboxQuery(
        String status,
        String assignedTo,
        String channel,
        int limit) {
}
