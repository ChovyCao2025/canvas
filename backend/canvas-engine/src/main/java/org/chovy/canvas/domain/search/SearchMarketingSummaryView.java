package org.chovy.canvas.domain.search;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * SearchMarketingSummaryView 承载 domain.search 场景中的不可变数据快照。
 * @param tenantId tenantId 字段。
 * @param channel channel 字段。
 * @param sourceId sourceId 字段。
 * @param keywordId keywordId 字段。
 * @param startDate startDate 字段。
 * @param endDate endDate 字段。
 * @param snapshotCount snapshotCount 字段。
 * @param impressionCount impressionCount 字段。
 * @param clickCount clickCount 字段。
 * @param costAmount costAmount 字段。
 * @param conversionCount conversionCount 字段。
 * @param revenueAmount revenueAmount 字段。
 * @param ctr ctr 字段。
 * @param cpc cpc 字段。
 * @param conversionRate conversionRate 字段。
 * @param roas roas 字段。
 * @param averagePosition averagePosition 字段。
 */
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
