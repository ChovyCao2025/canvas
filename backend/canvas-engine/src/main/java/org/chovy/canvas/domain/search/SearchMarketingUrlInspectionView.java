package org.chovy.canvas.domain.search;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

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
