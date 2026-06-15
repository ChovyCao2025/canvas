package org.chovy.canvas.bi.api;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

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
        LocalDateTime updatedAt) {

    public BiDeliveryLogView {
        receiver = receiver == null ? Map.of() : Map.copyOf(receiver);
        payload = payload == null ? Map.of() : Map.copyOf(payload);
    }
}
