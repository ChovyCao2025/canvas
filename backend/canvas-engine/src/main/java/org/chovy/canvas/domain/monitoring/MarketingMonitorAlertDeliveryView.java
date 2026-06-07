package org.chovy.canvas.domain.monitoring;

import java.time.LocalDateTime;

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
