package org.chovy.canvas.bi.api;

import java.util.Map;
/**
 * BiSubscriptionCommand 命令。
 */
public record BiSubscriptionCommand(
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
        Boolean enabled) {

    public BiSubscriptionCommand {
        schedule = schedule == null ? Map.of() : Map.copyOf(schedule);
        receivers = receivers == null ? Map.of() : Map.copyOf(receivers);
        delivery = delivery == null ? Map.of() : Map.copyOf(delivery);
    }
}
