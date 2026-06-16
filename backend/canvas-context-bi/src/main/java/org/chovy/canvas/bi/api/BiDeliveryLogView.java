package org.chovy.canvas.bi.api;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
/**
 * BiDeliveryLogView 视图。
 */
public record BiDeliveryLogView(
        /**
         * 唯一标识。
         */
        Long id,
        /**
         * 租户标识。
         */
        Long tenantId,
        /**
         * 工作空间标识。
         */
        Long workspaceId,
        /**
         * jobType 字段值。
         */
        String jobType,
        /**
         * jobId 对应的标识。
         */
        Long jobId,
        /**
         * jobKey 对应的业务键。
         */
        String jobKey,
        /**
         * 资源类型。
         */
        String resourceType,
        /**
         * 资源标识。
         */
        Long resourceId,
        /**
         * channel 字段值。
         */
        String channel,
        /**
         * receiver 字段值。
         */
        Map<String, Object> receiver,
        /**
         * payload 字段值。
         */
        Map<String, Object> payload,
        /**
         * metricValue 字段值。
         */
        BigDecimal metricValue,
        /**
         * 状态值。
         */
        String status,
        /**
         * message 字段值。
         */
        String message,
        /**
         * errorMessage 字段值。
         */
        String errorMessage,
        /**
         * retryCount 对应的统计数量。
         */
        Integer retryCount,
        /**
         * maxRetryCount 对应的统计数量。
         */
        Integer maxRetryCount,
        /**
         * nextRetryAt 对应的时间。
         */
        LocalDateTime nextRetryAt,
        /**
         * lastRetryAt 对应的时间。
         */
        LocalDateTime lastRetryAt,
        /**
         * retryExhaustedAt 对应的时间。
         */
        LocalDateTime retryExhaustedAt,
        /**
         * triggeredBy 字段值。
         */
        String triggeredBy,
        /**
         * 创建时间。
         */
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public BiDeliveryLogView {
        receiver = receiver == null ? Map.of() : Map.copyOf(receiver);
        payload = payload == null ? Map.of() : Map.copyOf(payload);
    }
}
