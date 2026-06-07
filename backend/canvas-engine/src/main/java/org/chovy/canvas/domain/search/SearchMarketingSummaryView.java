package org.chovy.canvas.domain.search;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SearchMarketingSummaryView(
        Long tenantId,
        String channel,
        Long sourceId,
        Long keywordId,
        LocalDate startDate,
        LocalDate endDate,
        int snapshotCount,
        Long impressionCount,
        Long clickCount,
        BigDecimal costAmount,
        Long conversionCount,
        BigDecimal revenueAmount,
        BigDecimal ctr,
        BigDecimal cpc,
        BigDecimal conversionRate,
        BigDecimal roas,
        BigDecimal averagePosition) {
}
