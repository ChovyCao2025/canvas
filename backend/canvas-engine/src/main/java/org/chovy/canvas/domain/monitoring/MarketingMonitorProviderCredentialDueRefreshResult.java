package org.chovy.canvas.domain.monitoring;

import java.time.LocalDateTime;
import java.util.List;

public record MarketingMonitorProviderCredentialDueRefreshResult(
        Long tenantId,
        int candidateCount,
        int dueCount,
        int refreshedCount,
        int failedCount,
        int skippedCount,
        LocalDateTime cutoffAt,
        LocalDateTime evaluatedAt,
        List<MarketingMonitorProviderCredentialView> credentials) {

    public MarketingMonitorProviderCredentialDueRefreshResult {
        credentials = credentials == null ? List.of() : List.copyOf(credentials);
    }
}
