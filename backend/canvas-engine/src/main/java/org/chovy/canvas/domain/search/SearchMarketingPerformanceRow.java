package org.chovy.canvas.domain.search;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public record SearchMarketingPerformanceRow(
        String keywordText,
        String matchType,
        String landingPageUrl,
        LocalDate snapshotDate,
        String device,
        String country,
        String queryGroupKey,
        long impressionCount,
        long clickCount,
        BigDecimal costAmount,
        long conversionCount,
        BigDecimal revenueAmount,
        BigDecimal averagePosition,
        Map<String, Object> metadata) {

    public SearchMarketingPerformanceRow {
        costAmount = costAmount == null ? BigDecimal.ZERO : costAmount;
        revenueAmount = revenueAmount == null ? BigDecimal.ZERO : revenueAmount;
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
