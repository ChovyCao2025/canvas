package org.chovy.canvas.domain.monitoring;

import java.time.LocalDateTime;
import java.util.Map;

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
