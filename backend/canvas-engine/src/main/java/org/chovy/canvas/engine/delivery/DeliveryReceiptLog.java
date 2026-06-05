package org.chovy.canvas.engine.delivery;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DeliveryReceiptLog {
    private Long id;
    private Long tenantId;
    private Long outboxId;
    private String provider;
    private String providerMessageId;
    private String receiptType;
    private String rawPayloadJson;
    private String idempotencyKey;
    private LocalDateTime receivedAt;
    private LocalDateTime createdAt;
}
