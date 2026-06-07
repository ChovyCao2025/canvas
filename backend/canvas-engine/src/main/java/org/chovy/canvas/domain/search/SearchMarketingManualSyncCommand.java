package org.chovy.canvas.domain.search;

import java.time.LocalDate;

public record SearchMarketingManualSyncCommand(
        String runType,
        LocalDate windowStart,
        LocalDate windowEnd,
        String cursorValue) {
}
