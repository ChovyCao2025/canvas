package org.chovy.canvas.domain.conversation;

/**
 * ConversationSopTaskCompletionCommand 承载 domain.conversation 场景中的不可变数据快照。
 * @param note note 字段。
 */
public record ConversationSopTaskCompletionCommand(String note) {
}
