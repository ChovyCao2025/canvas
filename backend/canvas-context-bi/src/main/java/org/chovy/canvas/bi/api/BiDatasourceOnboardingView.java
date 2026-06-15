package org.chovy.canvas.bi.api;

import java.util.Map;

public record BiDatasourceOnboardingView(
        Long id,
        Long tenantId,
        String sourceKey,
        String connectorType,
        String name,
        String maskedUrl,
        String maskedUsername,
        String description,
        boolean enabled,
        String status,
        Map<String, Object> connectorConfig,
        String createdBy,
        String updatedBy) {

    public BiDatasourceOnboardingView {
        connectorConfig = connectorConfig == null ? Map.of() : Map.copyOf(connectorConfig);
    }
}
