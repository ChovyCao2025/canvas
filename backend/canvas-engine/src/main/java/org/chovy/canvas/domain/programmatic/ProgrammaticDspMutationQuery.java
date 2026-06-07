package org.chovy.canvas.domain.programmatic;

public record ProgrammaticDspMutationQuery(
        Long seatId,
        Long campaignId,
        Long lineItemId,
        String status,
        String approvalStatus,
        Integer limit
) {
}
