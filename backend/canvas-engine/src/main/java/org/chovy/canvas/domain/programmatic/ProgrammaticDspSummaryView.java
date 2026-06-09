package org.chovy.canvas.domain.programmatic;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ProgrammaticDspSummaryView 承载 domain.programmatic 场景中的不可变数据快照。
 * @param tenantId tenantId 字段。
 * @param seatId seatId 字段。
 * @param campaignId campaignId 字段。
 * @param lineItemId lineItemId 字段。
 * @param startDate startDate 字段。
 * @param endDate endDate 字段。
 * @param snapshotCount snapshotCount 字段。
 * @param bidCount bidCount 字段。
 * @param winCount winCount 字段。
 * @param impressionCount impressionCount 字段。
 * @param clickCount clickCount 字段。
 * @param conversionCount conversionCount 字段。
 * @param viewableImpressionCount viewableImpressionCount 字段。
 * @param spendAmount spendAmount 字段。
 * @param revenueAmount revenueAmount 字段。
 * @param budgetAmount budgetAmount 字段。
 * @param winRate winRate 字段。
 * @param ctr ctr 字段。
 * @param conversionRate conversionRate 字段。
 * @param cpa cpa 字段。
 * @param roas roas 字段。
 * @param viewabilityRate viewabilityRate 字段。
 * @param budgetSpentPercent budgetSpentPercent 字段。
 * @param pacingStatus pacingStatus 字段。
 * @param evaluatedAt evaluatedAt 字段。
 */
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
