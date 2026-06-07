package org.chovy.canvas.domain.search;

import java.time.LocalDate;

public record SearchMarketingUrlInspectionQuery(
        Long sourceId,
        String indexedState,
        LocalDate startDate,
        LocalDate endDate,
        Integer limit) {
}
