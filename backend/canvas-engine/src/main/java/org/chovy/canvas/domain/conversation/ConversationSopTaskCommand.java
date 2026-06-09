package org.chovy.canvas.domain.conversation;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * ConversationSopTaskCommand 承载 domain.conversation 场景中的不可变数据快照。
 * @param taskKey taskKey 字段。
 * @param title title 字段。
 * @param assignee assignee 字段。
 * @param dueAt dueAt 字段。
 * @param metadata metadata 字段。
 */
public record ConversationSopTaskCommand(
        String taskKey,
        String title,
        String assignee,
        LocalDateTime dueAt,
        Map<String, Object> metadata) {

    public ConversationSopTaskCommand {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
