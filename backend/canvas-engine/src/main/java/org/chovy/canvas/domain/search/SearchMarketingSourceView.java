package org.chovy.canvas.domain.search;

import java.time.LocalDateTime;
import java.util.Map;

public record SearchMarketingSourceView(
        Long id,
        Long tenantId,
        String provider,
        String sourceKey,
        String displayName,
        String channel,
        String externalAccountId,
        String siteUrl,
        String currency,
        String timezone,
        boolean enabled,
        Map<String, Object> metadata,
        String createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public SearchMarketingSourceView {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
