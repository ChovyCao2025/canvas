package org.chovy.canvas.domain.conversation;

/**
 * ConversationInboxQuery 承载 domain.conversation 场景中的不可变数据快照。
 * @param status status 字段。
 * @param assignedTo assignedTo 字段。
 * @param channel channel 字段。
 * @param limit limit 字段。
 */
public record ConversationInboxQuery(
        String status,
        String assignedTo,
        String channel,
        int limit) {
}
