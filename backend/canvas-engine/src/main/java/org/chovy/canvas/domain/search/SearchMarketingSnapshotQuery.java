package org.chovy.canvas.domain.search;

import java.time.LocalDate;

public record SearchMarketingSnapshotQuery(
        String channel,
        Long sourceId,
        Long keywordId,
        LocalDate startDate,
        LocalDate endDate,
        Integer limit) {
}
