package org.chovy.canvas.domain.bi.subscription;

import java.util.Map;

/**
 * BiSubscriptionCommand 承载 domain.bi.subscription 场景中的不可变数据快照。
 * @param subscriptionKey subscriptionKey 字段。
 * @param name name 字段。
 * @param resourceType resourceType 字段。
 * @param resourceKey resourceKey 字段。
 * @param resourceId resourceId 字段。
 * @param schedule schedule 字段。
 * @param receivers receivers 字段。
 * @param delivery delivery 字段。
 * @param enabled enabled 字段。
 */
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
        schedule = schedule == null ? null : Map.copyOf(schedule);
        receivers = receivers == null ? null : Map.copyOf(receivers);
        delivery = delivery == null ? null : Map.copyOf(delivery);
    }
}
