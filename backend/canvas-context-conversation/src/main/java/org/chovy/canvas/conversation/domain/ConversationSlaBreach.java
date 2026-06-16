package org.chovy.canvas.conversation.domain;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 工单 SLA 违约领域对象。
 *
 * @param id 违约记录标识
 * @param tenantId 租户标识
 * @param workItemId 工单标识
 * @param breachType 违约类型
 * @param severity 严重程度
 * @param status 处理状态
 * @param escalationTarget 升级目标
 * @param reason 违约原因
 * @param dueAt SLA 到期时间
 * @param breachedAt 违约发生时间
 * @param resolvedBy 解决操作者
 * @param resolvedAt 解决时间
 * @param metadata 扩展元数据
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 */
public record ConversationSlaBreach(
        /**
         * 违约记录标识。
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
         * 违约类型。
         */
        String breachType,
        /**
         * 严重程度。
         */
        String severity,
        /**
         * 处理状态。
         */
        String status,
        /**
         * 升级目标。
         */
        String escalationTarget,
        /**
         * 违约原因。
         */
        String reason,
        /**
         * SLA 到期时间。
         */
        LocalDateTime dueAt,
        /**
         * 违约发生时间。
         */
        LocalDateTime breachedAt,
        /**
         * 解决操作者。
         */
        String resolvedBy,
        /**
         * 解决时间。
         */
        LocalDateTime resolvedAt,
        /**
         * 扩展元数据。
         */
        Map<String, Object> metadata,
        /**
         * 创建时间。
         */
        LocalDateTime createdAt,
        /**
         * 更新时间。
         */
        LocalDateTime updatedAt) {

    /**
     * 创建 SLA 违约记录并复制扩展元数据。
     */
    public ConversationSlaBreach {
        metadata = DomainMaps.copy(metadata);
    }

    /**
     * 返回替换持久化标识后的 SLA 违约副本。
     *
     * @param id 持久化生成的违约标识
     * @return 带新标识的 SLA 违约记录
     */
    public ConversationSlaBreach withId(Long id) {
        return new ConversationSlaBreach(id, tenantId, workItemId, breachType, severity, status,
                escalationTarget, reason, dueAt, breachedAt, resolvedBy, resolvedAt, metadata, createdAt, updatedAt);
    }
}
