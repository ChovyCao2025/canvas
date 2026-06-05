package org.chovy.canvas.domain.bi.subscription;

import java.time.LocalDateTime;
import java.util.Map;

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
