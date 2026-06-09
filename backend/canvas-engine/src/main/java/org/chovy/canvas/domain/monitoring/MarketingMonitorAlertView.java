package org.chovy.canvas.domain.monitoring;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * MarketingMonitorAlertView 承载 domain.monitoring 场景中的不可变数据快照。
 * @param id id 字段。
 * @param tenantId tenantId 字段。
 * @param alertType alertType 字段。
 * @param severity severity 字段。
 * @param status status 字段。
 * @param scopeKey scopeKey 字段。
 * @param title title 字段。
 * @param reason reason 字段。
 * @param itemCount itemCount 字段。
 * @param windowStart windowStart 字段。
 * @param windowEnd windowEnd 字段。
 * @param metadata metadata 字段。
 * @param createdBy createdBy 字段。
 * @param resolvedBy resolvedBy 字段。
 * @param resolvedAt resolvedAt 字段。
 * @param createdAt createdAt 字段。
 * @param updatedAt updatedAt 字段。
 */
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
