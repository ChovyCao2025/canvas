package org.chovy.canvas.domain.monitoring;

import java.time.LocalDateTime;

/**
 * MarketingMonitorAlertDeliveryView 承载 domain.monitoring 场景中的不可变数据快照。
 * @param id id 字段。
 * @param tenantId tenantId 字段。
 * @param alertId alertId 字段。
 * @param channelId channelId 字段。
 * @param channelKey channelKey 字段。
 * @param channelType channelType 字段。
 * @param deliveryId deliveryId 字段。
 * @param attempt attempt 字段。
 * @param httpStatus httpStatus 字段。
 * @param status status 字段。
 * @param nextRetryAt nextRetryAt 字段。
 * @param errorMessage errorMessage 字段。
 * @param terminalReason terminalReason 字段。
 * @param createdAt createdAt 字段。
 * @param updatedAt updatedAt 字段。
 */
public record MarketingMonitorAlertDeliveryView(Long id,
                                                Long tenantId,
                                                Long alertId,
                                                Long channelId,
                                                String channelKey,
                                                String channelType,
                                                String deliveryId,
                                                int attempt,
                                                Integer httpStatus,
                                                String status,
                                                LocalDateTime nextRetryAt,
                                                String errorMessage,
                                                String terminalReason,
                                                LocalDateTime createdAt,
                                                LocalDateTime updatedAt) {
}
