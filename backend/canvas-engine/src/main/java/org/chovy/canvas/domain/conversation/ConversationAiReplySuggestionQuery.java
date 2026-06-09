package org.chovy.canvas.domain.conversation;

/**
 * ConversationAiReplySuggestionQuery 承载 domain.conversation 场景中的不可变数据快照。
 * @param status status 字段。
 * @param limit limit 字段。
 */
public record ConversationAiReplySuggestionQuery(
        String status,
        int limit) {
}
