package org.chovy.canvas.engine.delivery;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DeliveryReceiptRequest 承载 engine.delivery 场景中的不可变数据快照。
 * @param provider provider 字段。
 * @param providerMessageId providerMessageId 字段。
 * @param receiptType receiptType 字段。
 * @param idempotencyKey idempotencyKey 字段。
 * @param receivedAt receivedAt 字段。
 * @param rawPayload rawPayload 字段。
 */
public record DeliveryReceiptRequest(
        String provider,
        String providerMessageId,
        String receiptType,
        String idempotencyKey,
        LocalDateTime receivedAt,
        Map<String, Object> rawPayload
) {
}
