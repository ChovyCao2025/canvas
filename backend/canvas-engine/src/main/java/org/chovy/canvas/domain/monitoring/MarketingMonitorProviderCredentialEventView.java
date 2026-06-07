package org.chovy.canvas.domain.monitoring;

import java.time.LocalDateTime;
import java.util.Map;

public record MarketingMonitorProviderCredentialEventView(
        Long id,
        Long tenantId,
        Long credentialId,
        String credentialKey,
        String eventType,
        String status,
        Map<String, Object> metadata,
        String errorMessage,
        String createdBy,
        LocalDateTime createdAt) {
}
