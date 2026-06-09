package org.chovy.canvas.domain.monitoring;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * MarketingMonitorAlertChannelView 承载 domain.monitoring 场景中的不可变数据快照。
 * @param id id 字段。
 * @param tenantId tenantId 字段。
 * @param channelKey channelKey 字段。
 * @param channelType channelType 字段。
 * @param displayName displayName 字段。
 * @param endpointUrl endpointUrl 字段。
 * @param enabled enabled 字段。
 * @param minSeverity minSeverity 字段。
 * @param alertTypes alertTypes 字段。
 * @param signingMode signingMode 字段。
 * @param secretPrefix secretPrefix 字段。
 * @param metadata metadata 字段。
 * @param maxAttempts maxAttempts 字段。
 * @param createdBy createdBy 字段。
 * @param createdAt createdAt 字段。
 * @param updatedAt updatedAt 字段。
 */
public record MarketingMonitorAlertChannelView(Long id,
                                               Long tenantId,
                                               String channelKey,
                                               String channelType,
                                               String displayName,
                                               String endpointUrl,
                                               boolean enabled,
                                               String minSeverity,
                                               List<String> alertTypes,
                                               String signingMode,
                                               String secretPrefix,
                                               Map<String, Object> metadata,
                                               int maxAttempts,
                                               String createdBy,
                                               LocalDateTime createdAt,
                                               LocalDateTime updatedAt) {
}
