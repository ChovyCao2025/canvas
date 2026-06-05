package org.chovy.canvas.engine.delivery;

import java.time.LocalDateTime;
import java.util.Map;

public record DeliveryReceiptRequest(
        String provider,
        String providerMessageId,
        String receiptType,
        String idempotencyKey,
        LocalDateTime receivedAt,
        Map<String, Object> rawPayload
) {
}
