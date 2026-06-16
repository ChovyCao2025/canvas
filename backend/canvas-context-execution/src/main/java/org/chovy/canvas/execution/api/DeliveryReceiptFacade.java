package org.chovy.canvas.execution.api;

import java.time.LocalDateTime;
import java.util.Map;

public interface DeliveryReceiptFacade {

    ReceiptView recordReceipt(ReceiptCommand command);

    record ReceiptCommand(
            String provider,
            String providerMessageId,
            String receiptType,
            String idempotencyKey,
            LocalDateTime receivedAt,
            Map<String, Object> rawPayload) {
        public ReceiptCommand {
            rawPayload = Map.copyOf(rawPayload == null ? Map.of() : rawPayload);
        }
    }

    record ReceiptView(
            Long id,
            Long tenantId,
            Long outboxId,
            String provider,
            String providerMessageId,
            String receiptType,
            String rawPayloadJson,
            String idempotencyKey,
            LocalDateTime receivedAt,
            LocalDateTime createdAt) {
    }
}
