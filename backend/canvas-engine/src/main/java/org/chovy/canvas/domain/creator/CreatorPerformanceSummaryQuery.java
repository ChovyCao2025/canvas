package org.chovy.canvas.domain.creator;

import java.time.LocalDateTime;

public record CreatorPerformanceSummaryQuery(
        Long campaignId,
        Long creatorId,
        Long collaborationId,
        LocalDateTime evaluatedAt) {
}
