package org.chovy.canvas.domain.search;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SearchMarketingOpportunityEvaluationCommand(
        String channel,
        Long sourceId,
        Long keywordId,
        LocalDate startDate,
        LocalDate endDate,
        Long minImpressions,
        BigDecimal lowCtrThreshold,
        BigDecimal seoPageTwoPosition,
        BigDecimal wastedSpendThreshold) {
}
