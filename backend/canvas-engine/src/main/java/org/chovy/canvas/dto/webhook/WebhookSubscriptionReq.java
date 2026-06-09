package org.chovy.canvas.dto.webhook;

import java.util.List;

/**
 * WebhookSubscriptionReq 承载 dto.webhook 场景中的不可变数据快照。
 * @param name name 字段。
 * @param callbackUrl callbackUrl 字段。
 * @param eventTypes eventTypes 字段。
 * @param maxAttempts maxAttempts 字段。
 */
public record WebhookSubscriptionReq(
        String name,
        String callbackUrl,
        List<String> eventTypes,
        Integer maxAttempts
) {
}
