package org.chovy.canvas.domain.bi.datasource;

import java.time.LocalDateTime;
import java.util.List;

public record BiDatasourceOnboardingView(
        Long id,
        String sourceKey,
        String name,
        String type,
        String connectorType,
        boolean enabled,
        String driverClassName,
        String maskedUrl,
        String maskedUsername,
        String connectionMode,
        String schemaSyncStatus,
        int tableCount,
        LocalDateTime lastSyncedAt,
        List<String> supportedModes,
        String supportStatus,
        List<String> capabilities
) {

    public BiDatasourceOnboardingView {
        supportedModes = supportedModes == null ? List.of() : List.copyOf(supportedModes);
        capabilities = capabilities == null ? List.of() : List.copyOf(capabilities);
    }
}
