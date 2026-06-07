package org.chovy.canvas.domain.search;

public record SearchMarketingOpportunityQuery(
        String channel,
        Long sourceId,
        String status,
        String severity,
        Integer limit) {
}
