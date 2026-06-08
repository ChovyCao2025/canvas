package org.chovy.canvas.domain.bi.subscription;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * BiSubscriptionView 承载 domain.bi.subscription 场景中的不可变数据快照。
 * @param id id 字段。
 * @param tenantId tenantId 字段。
 * @param workspaceId workspaceId 字段。
 * @param subscriptionKey subscriptionKey 字段。
 * @param name name 字段。
 * @param resourceType resourceType 字段。
 * @param resourceKey resourceKey 字段。
 * @param resourceId resourceId 字段。
 * @param schedule schedule 字段。
 * @param receivers receivers 字段。
 * @param delivery delivery 字段。
 * @param enabled enabled 字段。
 * @param createdBy createdBy 字段。
 * @param createdAt createdAt 字段。
 * @param updatedAt updatedAt 字段。
 */
public record BiSubscriptionView(
        Long id,
        Long tenantId,
        Long workspaceId,
        String subscriptionKey,
        String name,
        String resourceType,
        String resourceKey,
        Long resourceId,
        Map<String, Object> schedule,
        Map<String, Object> receivers,
        Map<String, Object> delivery,
        Boolean enabled,
        String createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public BiSubscriptionView {
        schedule = schedule == null ? Map.of() : Map.copyOf(schedule);
        receivers = receivers == null ? Map.of() : Map.copyOf(receivers);
        delivery = delivery == null ? Map.of() : Map.copyOf(delivery);
    }
}
