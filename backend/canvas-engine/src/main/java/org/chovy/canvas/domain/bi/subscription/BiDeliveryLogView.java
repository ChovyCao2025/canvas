package org.chovy.canvas.domain.bi.subscription;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * BiDeliveryLogView 承载 domain.bi.subscription 场景中的不可变数据快照。
 * @param id id 字段。
 * @param tenantId tenantId 字段。
 * @param workspaceId workspaceId 字段。
 * @param jobType jobType 字段。
 * @param jobId jobId 字段。
 * @param jobKey jobKey 字段。
 * @param resourceType resourceType 字段。
 * @param resourceId resourceId 字段。
 * @param channel channel 字段。
 * @param receiver receiver 字段。
 * @param payload payload 字段。
 * @param metricValue metricValue 字段。
 * @param status status 字段。
 * @param message message 字段。
 * @param errorMessage errorMessage 字段。
 * @param retryCount retryCount 字段。
 * @param maxRetryCount maxRetryCount 字段。
 * @param nextRetryAt nextRetryAt 字段。
 * @param lastRetryAt lastRetryAt 字段。
 * @param retryExhaustedAt retryExhaustedAt 字段。
 * @param triggeredBy triggeredBy 字段。
 * @param createdAt createdAt 字段。
 * @param updatedAt updatedAt 字段。
 */
public record BiDeliveryLogView(
        Long id,
        Long tenantId,
        Long workspaceId,
        String jobType,
        Long jobId,
        String jobKey,
        String resourceType,
        Long resourceId,
        String channel,
        Map<String, Object> receiver,
        Map<String, Object> payload,
        BigDecimal metricValue,
        String status,
        String message,
        String errorMessage,
        Integer retryCount,
        Integer maxRetryCount,
        LocalDateTime nextRetryAt,
        LocalDateTime lastRetryAt,
        LocalDateTime retryExhaustedAt,
        String triggeredBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public BiDeliveryLogView {
        receiver = receiver == null ? Map.of() : Map.copyOf(receiver);
        payload = payload == null ? Map.of() : Map.copyOf(payload);
    }
}
