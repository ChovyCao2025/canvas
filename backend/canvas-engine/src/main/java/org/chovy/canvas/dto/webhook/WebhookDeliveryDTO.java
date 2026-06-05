package org.chovy.canvas.dto.webhook;

import java.time.LocalDateTime;

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
