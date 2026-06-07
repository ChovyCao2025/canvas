package org.chovy.canvas.domain.search;

import java.util.Map;

public record SearchMarketingSourceCommand(
        String provider,
        String sourceKey,
        String displayName,
        String channel,
        String externalAccountId,
        String siteUrl,
        String currency,
        String timezone,
        Boolean enabled,
        Map<String, Object> metadata) {

    public SearchMarketingSourceCommand {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
