package org.chovy.canvas.domain.creator;

public record CreatorProviderMutationQuery(
        Long campaignId,
        Long collaborationId,
        String status,
        String approvalStatus,
        Integer limit
) {
}
