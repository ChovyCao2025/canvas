package org.chovy.canvas.domain.conversation;

import java.time.LocalDateTime;

/**
 * ConversationWorkItemStatusCommand 承载 domain.conversation 场景中的不可变数据快照。
 * @param status status 字段。
 * @param priority priority 字段。
 * @param nextFollowUpAt nextFollowUpAt 字段。
 * @param note note 字段。
 */
public record ConversationWorkItemStatusCommand(
        String status,
        String priority,
        LocalDateTime nextFollowUpAt,
        String note) {
}
