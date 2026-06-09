package org.chovy.canvas.domain.monitoring;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * MarketingMonitorProviderOAuthAuthorizationEventView 承载 domain.monitoring 场景中的不可变数据快照。
 * @param id id 字段。
 * @param tenantId tenantId 字段。
 * @param authorizationId authorizationId 字段。
 * @param authState authState 字段。
 * @param credentialKey credentialKey 字段。
 * @param eventType eventType 字段。
 * @param status status 字段。
 * @param metadata metadata 字段。
 * @param errorMessage errorMessage 字段。
 * @param createdBy createdBy 字段。
 * @param createdAt createdAt 字段。
 */
public record MarketingMonitorProviderOAuthAuthorizationEventView(
        Long id,
        Long tenantId,
        Long authorizationId,
        String authState,
        String credentialKey,
        String eventType,
        String status,
        Map<String, Object> metadata,
        String errorMessage,
        String createdBy,
        LocalDateTime createdAt) {
}
