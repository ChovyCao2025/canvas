package org.chovy.canvas.domain.search;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * SearchMarketingUrlInspectionView 承载 domain.search 场景中的不可变数据快照。
 * @param id id 字段。
 * @param tenantId tenantId 字段。
 * @param sourceId sourceId 字段。
 * @param provider provider 字段。
 * @param pageUrl pageUrl 字段。
 * @param pageUrlHash pageUrlHash 字段。
 * @param inspectionDate inspectionDate 字段。
 * @param indexedState indexedState 字段。
 * @param crawlState crawlState 字段。
 * @param canonicalUrl canonicalUrl 字段。
 * @param sitemapState sitemapState 字段。
 * @param mobileUsabilityState mobileUsabilityState 字段。
 * @param lastCrawlAt lastCrawlAt 字段。
 * @param evidence evidence 字段。
 * @param createdBy createdBy 字段。
 * @param createdAt createdAt 字段。
 * @param updatedAt updatedAt 字段。
 */
public record SearchMarketingUrlInspectionView(
        Long id,
        Long tenantId,
        Long sourceId,
        String provider,
        String pageUrl,
        String pageUrlHash,
        LocalDate inspectionDate,
        String indexedState,
        String crawlState,
        String canonicalUrl,
        String sitemapState,
        String mobileUsabilityState,
        LocalDateTime lastCrawlAt,
        Map<String, Object> evidence,
        String createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public SearchMarketingUrlInspectionView {
        evidence = evidence == null ? Map.of() : Map.copyOf(evidence);
    }
}
