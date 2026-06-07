package org.chovy.canvas.domain.search;

public record SearchMarketingMutationQuery(
        Long sourceId,
        String status,
        String approvalStatus,
        Integer limit) {
}
