package org.chovy.canvas.domain.conversation;

public record ConversationIngressResp(
        Long sessionId,
        Long messageId,
        String status,
        boolean duplicate,
        int resumedWaitCount) {
}
