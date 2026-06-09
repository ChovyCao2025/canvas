package org.chovy.canvas.domain.monitoring;

import java.time.LocalDateTime;
import java.util.List;

/**
 * MarketingMonitorProviderCredentialDueRefreshResult 承载 domain.monitoring 场景中的不可变数据快照。
 * @param tenantId tenantId 字段。
 * @param candidateCount candidateCount 字段。
 * @param dueCount dueCount 字段。
 * @param refreshedCount refreshedCount 字段。
 * @param failedCount failedCount 字段。
 * @param skippedCount skippedCount 字段。
 * @param cutoffAt cutoffAt 字段。
 * @param evaluatedAt evaluatedAt 字段。
 * @param credentials credentials 字段。
 */
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
