package org.chovy.canvas.domain.search;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * SearchMarketingOpportunityEvaluationCommand 承载 domain.search 场景中的不可变数据快照。
 * @param channel channel 字段。
 * @param sourceId sourceId 字段。
 * @param keywordId keywordId 字段。
 * @param startDate startDate 字段。
 * @param endDate endDate 字段。
 * @param minImpressions minImpressions 字段。
 * @param lowCtrThreshold lowCtrThreshold 字段。
 * @param seoPageTwoPosition seoPageTwoPosition 字段。
 * @param wastedSpendThreshold wastedSpendThreshold 字段。
 */
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
