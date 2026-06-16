package org.chovy.canvas.bi.api;

import java.time.LocalDateTime;
import java.util.Map;
/**
 * BiSubscriptionView 视图。
 */
public record BiSubscriptionView(
        /**
         * 唯一标识。
         */
        Long id,
        /**
         * 租户标识。
         */
        Long tenantId,
        /**
         * 工作空间标识。
         */
        Long workspaceId,
        /**
         * subscriptionKey 对应的业务键。
         */
        String subscriptionKey,
        /**
         * 展示名称。
         */
        String name,
        /**
         * 资源类型。
         */
        String resourceType,
        /**
         * 资源键。
         */
        String resourceKey,
        /**
         * 资源标识。
         */
        Long resourceId,
        /**
         * schedule 字段值。
         */
        Map<String, Object> schedule,
        /**
         * receivers 对应的数据集合。
         */
        Map<String, Object> receivers,
        /**
         * delivery 字段值。
         */
        Map<String, Object> delivery,
        /**
         * enabled 字段值。
         */
        Boolean enabled,
        /**
         * 创建人。
         */
        String createdBy,
        /**
         * 创建时间。
         */
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public BiSubscriptionView {
        schedule = schedule == null ? Map.of() : Map.copyOf(schedule);
        receivers = receivers == null ? Map.of() : Map.copyOf(receivers);
        delivery = delivery == null ? Map.of() : Map.copyOf(delivery);
    }
}
