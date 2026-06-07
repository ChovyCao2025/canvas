package org.chovy.canvas.domain.bi.datasource;

import java.util.Map;

public record BiDatasourceOnboardingCommand(
        String connectorType,
        String name,
        String url,
        String username,
        String password,
        String driverClassName,
        String description,
        Boolean enabled,
        String connectionMode,
        Map<String, Object> connectorConfig) {

    public BiDatasourceOnboardingCommand(String connectorType,
                                         String name,
                                         String url,
                                         String username,
                                         String password,
                                         String driverClassName,
                                         String description,
                                         Boolean enabled,
                                         String connectionMode) {
        this(connectorType, name, url, username, password, driverClassName, description, enabled, connectionMode, Map.of());
    }

    public BiDatasourceOnboardingCommand {
        connectorConfig = connectorConfig == null ? Map.of() : Map.copyOf(connectorConfig);
    }
}
