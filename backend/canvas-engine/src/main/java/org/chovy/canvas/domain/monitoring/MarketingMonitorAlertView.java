package org.chovy.canvas.domain.monitoring;

import java.time.LocalDateTime;
import java.util.Map;

public record MarketingMonitorAlertView(Long id,
                                        Long tenantId,
                                        String alertType,
                                        String severity,
                                        String status,
                                        String scopeKey,
                                        String title,
                                        String reason,
                                        int itemCount,
                                        LocalDateTime windowStart,
                                        LocalDateTime windowEnd,
                                        Map<String, Object> metadata,
                                        String createdBy,
                                        String resolvedBy,
                                        LocalDateTime resolvedAt,
                                        LocalDateTime createdAt,
                                        LocalDateTime updatedAt) {
}
