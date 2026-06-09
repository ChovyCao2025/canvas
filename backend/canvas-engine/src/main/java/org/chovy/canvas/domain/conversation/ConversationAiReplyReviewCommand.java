package org.chovy.canvas.domain.conversation;

/**
 * ConversationAiReplyReviewCommand 承载 domain.conversation 场景中的不可变数据快照。
 * @param decision decision 字段。
 * @param note note 字段。
 */
public record ConversationAiReplyReviewCommand(
        String decision,
        String note) {
}
