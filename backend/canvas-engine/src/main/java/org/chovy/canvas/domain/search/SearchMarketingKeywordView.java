package org.chovy.canvas.domain.search;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record SearchMarketingKeywordView(
        Long id,
        Long tenantId,
        String channel,
        String keywordText,
        String keywordKey,
        String matchType,
        String landingPageUrl,
        String landingPageUrlHash,
        String searchIntent,
        List<String> labels,
        String status,
        Map<String, Object> metadata,
        String createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public SearchMarketingKeywordView {
        labels = labels == null ? List.of() : List.copyOf(labels);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
