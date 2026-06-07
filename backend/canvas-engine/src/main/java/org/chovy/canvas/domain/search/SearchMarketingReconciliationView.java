package org.chovy.canvas.domain.search;

import java.time.LocalDateTime;
import java.util.Map;

public record SearchMarketingReconciliationView(
        Long tenantId,
        Long mutationId,
        Long providerChangeId,
        String status,
        String providerOperationId,
        Map<String, Object> evidence,
        LocalDateTime reconciledAt) {

    public SearchMarketingReconciliationView {
        evidence = evidence == null ? Map.of() : Map.copyOf(evidence);
    }
}
