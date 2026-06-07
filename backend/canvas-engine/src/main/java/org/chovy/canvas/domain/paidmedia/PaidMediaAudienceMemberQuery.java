package org.chovy.canvas.domain.paidmedia;

public record PaidMediaAudienceMemberQuery(
        Long runId,
        String eligibilityStatus,
        int limit) {
}
