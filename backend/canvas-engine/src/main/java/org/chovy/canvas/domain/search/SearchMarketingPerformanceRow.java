package org.chovy.canvas.domain.search;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * SearchMarketingPerformanceRow 承载 domain.search 场景中的不可变数据快照。
 * @param keywordText keywordText 字段。
 * @param matchType matchType 字段。
 * @param landingPageUrl landingPageUrl 字段。
 * @param snapshotDate snapshotDate 字段。
 * @param device device 字段。
 * @param country country 字段。
 * @param queryGroupKey queryGroupKey 字段。
 * @param impressionCount impressionCount 字段。
 * @param clickCount clickCount 字段。
 * @param costAmount costAmount 字段。
 * @param conversionCount conversionCount 字段。
 * @param revenueAmount revenueAmount 字段。
 * @param averagePosition averagePosition 字段。
 * @param metadata metadata 字段。
 */
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
