package org.chovy.canvas.domain.search;

public record SearchMarketingImpactWindowQuery(
        Long opportunityId,
        Long mutationId,
        Long sourceId,
        String status,
        String decision,
        Integer limit) {
}
