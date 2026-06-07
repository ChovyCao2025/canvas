package org.chovy.canvas.domain.search;

import java.time.LocalDateTime;
import java.util.Map;

public record SearchMarketingProviderChangeView(
        Long id,
        Long tenantId,
        Long sourceId,
        Long mutationId,
        String provider,
        String externalResourceId,
        String changeType,
        Map<String, Object> changedFields,
        String providerActor,
        LocalDateTime providerChangedAt,
        String reconciliationStatus,
        Map<String, Object> evidence,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public SearchMarketingProviderChangeView {
        changedFields = changedFields == null ? Map.of() : Map.copyOf(changedFields);
        evidence = evidence == null ? Map.of() : Map.copyOf(evidence);
    }
}
