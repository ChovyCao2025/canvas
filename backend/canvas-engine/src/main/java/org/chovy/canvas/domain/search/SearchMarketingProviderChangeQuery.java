package org.chovy.canvas.domain.search;

public record SearchMarketingProviderChangeQuery(
        Long sourceId,
        Long mutationId,
        String provider,
        String reconciliationStatus,
        Integer limit) {
}
