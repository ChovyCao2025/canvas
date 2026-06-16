package org.chovy.canvas.execution.api;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 定义 DeliveryReceiptFacade 的执行上下文数据结构或业务契约。
 */
public interface DeliveryReceiptFacade {

    /**
     * 执行 recordReceipt 对应的业务处理。
     * @param command command 参数
     */
    ReceiptView recordReceipt(ReceiptCommand command);

    /**
     * 定义 ReceiptCommand 的执行上下文数据结构或业务契约。
     * @param provider provider 对应的数据字段
     * @param providerMessageId providerMessageId 对应的数据字段
     * @param receiptType receiptType 对应的数据字段
     * @param idempotencyKey idempotencyKey 对应的数据字段
     * @param receivedAt receivedAt 对应的数据字段
     * @param rawPayload rawPayload 对应的数据字段
     */
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

    /**
     * 定义 ReceiptView 的执行上下文数据结构或业务契约。
     * @param id id 对应的数据字段
     * @param tenantId tenantId 对应的数据字段
     * @param outboxId outboxId 对应的数据字段
     * @param provider provider 对应的数据字段
     * @param providerMessageId providerMessageId 对应的数据字段
     * @param receiptType receiptType 对应的数据字段
     * @param rawPayloadJson rawPayloadJson 对应的数据字段
     * @param idempotencyKey idempotencyKey 对应的数据字段
     * @param receivedAt receivedAt 对应的数据字段
     * @param createdAt createdAt 对应的数据字段
     */
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
