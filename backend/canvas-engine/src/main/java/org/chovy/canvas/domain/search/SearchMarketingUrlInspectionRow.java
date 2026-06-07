package org.chovy.canvas.domain.search;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

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
