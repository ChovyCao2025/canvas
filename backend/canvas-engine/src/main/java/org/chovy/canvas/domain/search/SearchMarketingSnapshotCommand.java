package org.chovy.canvas.domain.search;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public record SearchMarketingSnapshotCommand(
        Long sourceId,
        Long keywordId,
        LocalDate snapshotDate,
        String device,
        String country,
        String queryGroupKey,
        Long impressionCount,
        Long clickCount,
        BigDecimal costAmount,
        Long conversionCount,
        BigDecimal revenueAmount,
        BigDecimal averagePosition,
        Map<String, Object> metadata) {

    public SearchMarketingSnapshotCommand {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
