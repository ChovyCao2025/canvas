package org.chovy.canvas.domain.programmatic;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ProgrammaticDspSummaryQuery(
        Long seatId,
        Long campaignId,
        Long lineItemId,
        LocalDate startDate,
        LocalDate endDate,
        LocalDateTime evaluatedAt) {
}
