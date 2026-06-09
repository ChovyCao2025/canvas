package org.chovy.canvas.dto.webhook;

import java.time.LocalDateTime;

/**
 * WebhookDeliveryDTO 承载 dto.webhook 场景中的不可变数据快照。
 * @param id id 字段。
 * @param deliveryId deliveryId 字段。
 * @param eventType eventType 字段。
 * @param attempt attempt 字段。
 * @param httpStatus httpStatus 字段。
 * @param status status 字段。
 * @param nextRetryAt nextRetryAt 字段。
 * @param errorMessage errorMessage 字段。
 * @param terminalReason terminalReason 字段。
 * @param createdAt createdAt 字段。
 */
public record WebhookDeliveryDTO(
        Long id,
        String deliveryId,
        String eventType,
        Integer attempt,
        Integer httpStatus,
        String status,
        LocalDateTime nextRetryAt,
        String errorMessage,
        String terminalReason,
        LocalDateTime createdAt
) {
}
