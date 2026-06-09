package org.chovy.canvas.domain.search;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * SearchMarketingUrlInspectionRow 承载 domain.search 场景中的不可变数据快照。
 * @param pageUrl pageUrl 字段。
 * @param inspectionDate inspectionDate 字段。
 * @param indexedState indexedState 字段。
 * @param crawlState crawlState 字段。
 * @param canonicalUrl canonicalUrl 字段。
 * @param sitemapState sitemapState 字段。
 * @param mobileUsabilityState mobileUsabilityState 字段。
 * @param lastCrawlAt lastCrawlAt 字段。
 * @param evidence evidence 字段。
 */
public record SearchMarketingUrlInspectionRow(
        String pageUrl,
        LocalDate inspectionDate,
        String indexedState,
        String crawlState,
        String canonicalUrl,
        String sitemapState,
        String mobileUsabilityState,
        LocalDateTime lastCrawlAt,
        Map<String, Object> evidence) {

    public SearchMarketingUrlInspectionRow {
        evidence = evidence == null ? Map.of() : Map.copyOf(evidence);
    }
}
