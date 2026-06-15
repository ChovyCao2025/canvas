package org.chovy.canvas.bi.api;

import java.util.Map;

public record BiDatasourceOnboardingCommand(
        String connectorType,
        String name,
        String url,
        String username,
        String password,
        String sourceKey,
        String description,
        Boolean enabled,
        String status,
        Map<String, Object> connectorConfig) {

    public BiDatasourceOnboardingCommand {
        connectorConfig = connectorConfig == null ? Map.of() : Map.copyOf(connectorConfig);
    }
}
