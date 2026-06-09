package org.chovy.canvas.domain.conversation;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * ConversationSopTaskView 承载 domain.conversation 场景中的不可变数据快照。
 * @param id id 字段。
 * @param tenantId tenantId 字段。
 * @param workItemId workItemId 字段。
 * @param taskKey taskKey 字段。
 * @param title title 字段。
 * @param status status 字段。
 * @param assignee assignee 字段。
 * @param dueAt dueAt 字段。
 * @param completedBy completedBy 字段。
 * @param completedAt completedAt 字段。
 * @param metadata metadata 字段。
 * @param createdAt createdAt 字段。
 * @param updatedAt updatedAt 字段。
 */
public record ConversationSopTaskView(
        Long id,
        Long tenantId,
        Long workItemId,
        String taskKey,
        String title,
        String status,
        String assignee,
        LocalDateTime dueAt,
        String completedBy,
        LocalDateTime completedAt,
        Map<String, Object> metadata,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public ConversationSopTaskView {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
