package org.chovy.canvas.domain.conversation;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * ConversationWorkItemAuditView 承载 domain.conversation 场景中的不可变数据快照。
 * @param id id 字段。
 * @param tenantId tenantId 字段。
 * @param workItemId workItemId 字段。
 * @param eventType eventType 字段。
 * @param actor actor 字段。
 * @param oldValue oldValue 字段。
 * @param newValue newValue 字段。
 * @param note note 字段。
 * @param createdAt createdAt 字段。
 */
public record ConversationWorkItemAuditView(
        Long id,
        Long tenantId,
        Long workItemId,
        String eventType,
        String actor,
        Map<String, Object> oldValue,
        Map<String, Object> newValue,
        String note,
        LocalDateTime createdAt) {

    public ConversationWorkItemAuditView {
        oldValue = oldValue == null ? Map.of() : Map.copyOf(oldValue);
        newValue = newValue == null ? Map.of() : Map.copyOf(newValue);
    }
}
