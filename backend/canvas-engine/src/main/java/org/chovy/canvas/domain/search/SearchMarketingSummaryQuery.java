package org.chovy.canvas.domain.search;

import java.time.LocalDate;

public record SearchMarketingSummaryQuery(
        String channel,
        Long sourceId,
        Long keywordId,
        LocalDate startDate,
        LocalDate endDate) {
}
