package org.chovy.canvas.domain.paidmedia;

public record PaidMediaAudienceRunQuery(
        Long destinationId,
        Long audienceId,
        String status,
        int limit) {
}
