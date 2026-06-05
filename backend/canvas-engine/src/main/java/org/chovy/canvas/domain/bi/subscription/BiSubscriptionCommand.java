package org.chovy.canvas.domain.bi.subscription;

import java.util.Map;

public record BiSubscriptionCommand(
        String subscriptionKey,
        String name,
        String resourceType,
        String resourceKey,
        Long resourceId,
        Map<String, Object> schedule,
        Map<String, Object> receivers,
        Map<String, Object> delivery,
        Boolean enabled
) {
    public BiSubscriptionCommand {
        schedule = schedule == null ? Map.of() : Map.copyOf(schedule);
        receivers = receivers == null ? Map.of() : Map.copyOf(receivers);
        delivery = delivery == null ? Map.of() : Map.copyOf(delivery);
    }
}
