package org.chovy.canvas.domain.programmatic;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ProgrammaticDspSummaryView(
        Long tenantId,
        Long seatId,
        Long campaignId,
        Long lineItemId,
        LocalDate startDate,
        LocalDate endDate,
        int snapshotCount,
        Long bidCount,
        Long winCount,
        Long impressionCount,
        Long clickCount,
        Long conversionCount,
        Long viewableImpressionCount,
        BigDecimal spendAmount,
        BigDecimal revenueAmount,
        BigDecimal budgetAmount,
        BigDecimal winRate,
        BigDecimal ctr,
        BigDecimal conversionRate,
        BigDecimal cpa,
        BigDecimal roas,
        BigDecimal viewabilityRate,
        BigDecimal budgetSpentPercent,
        String pacingStatus,
        LocalDateTime evaluatedAt) {
}
