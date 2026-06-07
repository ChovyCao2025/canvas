package org.chovy.canvas.domain.search;

public record SearchMarketingOpportunityStatusCommand(
        String status,
        String reason) {
}
