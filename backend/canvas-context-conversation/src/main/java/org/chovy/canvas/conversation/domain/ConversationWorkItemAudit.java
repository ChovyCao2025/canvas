package org.chovy.canvas.conversation.domain;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 工单变更审计事件。
 *
 * @param id 审计记录标识
 * @param tenantId 租户标识
 * @param workItemId 工单标识
 * @param eventType 审计事件类型
 * @param actor 触发事件的操作者
 * @param oldValue 变更前值
 * @param newValue 变更后值
 * @param note 审计备注
 * @param createdAt 审计创建时间
 */
public record ConversationWorkItemAudit(
        /**
         * 审计记录标识。
         */
        Long id,
        /**
         * 租户标识。
         */
        Long tenantId,
        /**
         * 工单标识。
         */
        Long workItemId,
        /**
         * 审计事件类型。
         */
        String eventType,
        /**
         * 触发事件的操作者。
         */
        String actor,
        /**
         * 变更前值。
         */
        Map<String, Object> oldValue,
        /**
         * 变更后值。
         */
        Map<String, Object> newValue,
        /**
         * 审计备注。
         */
        String note,
        /**
         * 审计创建时间。
         */
        LocalDateTime createdAt) {

    /**
     * 创建审计事件并复制变更快照，避免后续修改污染审计内容。
     */
    public ConversationWorkItemAudit {
        oldValue = DomainMaps.copy(oldValue);
        newValue = DomainMaps.copy(newValue);
    }

    /**
     * 返回替换持久化标识后的审计事件副本。
     *
     * @param id 持久化生成的审计标识
     * @return 带新标识的审计事件
     */
    public ConversationWorkItemAudit withId(Long id) {
        return new ConversationWorkItemAudit(id, tenantId, workItemId, eventType, actor, oldValue, newValue, note, createdAt);
    }
}
