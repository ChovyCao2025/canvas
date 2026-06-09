package org.chovy.canvas.domain.monitoring;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * MarketingMonitorSourceView 承载 domain.monitoring 场景中的不可变数据快照。
 * @param id id 字段。
 * @param tenantId tenantId 字段。
 * @param sourceKey sourceKey 字段。
 * @param sourceType sourceType 字段。
 * @param displayName displayName 字段。
 * @param enabled enabled 字段。
 * @param metadata metadata 字段。
 * @param createdBy createdBy 字段。
 * @param createdAt createdAt 字段。
 * @param updatedAt updatedAt 字段。
 */
public record MarketingMonitorSourceView(Long id,
                                         Long tenantId,
                                         String sourceKey,
                                         String sourceType,
                                         String displayName,
                                         boolean enabled,
                                         Map<String, Object> metadata,
                                         String createdBy,
                                         LocalDateTime createdAt,
                                         LocalDateTime updatedAt) {
}
