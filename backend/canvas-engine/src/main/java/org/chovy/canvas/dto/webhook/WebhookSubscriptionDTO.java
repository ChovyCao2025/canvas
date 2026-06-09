package org.chovy.canvas.dto.webhook;

import java.time.LocalDateTime;
import java.util.List;

/**
 * WebhookSubscriptionDTO 承载 dto.webhook 场景中的不可变数据快照。
 * @param id id 字段。
 * @param name name 字段。
 * @param callbackUrl callbackUrl 字段。
 * @param secretPrefix secretPrefix 字段。
 * @param eventTypes eventTypes 字段。
 * @param status status 字段。
 * @param maxAttempts maxAttempts 字段。
 * @param createdAt createdAt 字段。
 * @param updatedAt updatedAt 字段。
 */
public record WebhookSubscriptionDTO(
        Long id,
        String name,
        String callbackUrl,
        String secretPrefix,
        List<String> eventTypes,
        String status,
        Integer maxAttempts,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
