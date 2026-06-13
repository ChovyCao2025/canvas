package org.chovy.canvas.conversation.api;

public record ConversationRecordResult(
        Long sessionId,
        Long messageId,
        String status,
        boolean duplicate,
        int resumedWaitCount) {
}
