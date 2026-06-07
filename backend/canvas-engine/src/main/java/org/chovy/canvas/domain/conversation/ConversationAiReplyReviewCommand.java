package org.chovy.canvas.domain.conversation;

public record ConversationAiReplyReviewCommand(
        String decision,
        String note) {
}
