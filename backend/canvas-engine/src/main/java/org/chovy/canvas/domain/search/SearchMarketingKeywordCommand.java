package org.chovy.canvas.domain.search;

import java.util.List;
import java.util.Map;

public record SearchMarketingKeywordCommand(
        String channel,
        String keywordText,
        String matchType,
        String landingPageUrl,
        String searchIntent,
        List<String> labels,
        String status,
        Map<String, Object> metadata) {

    public SearchMarketingKeywordCommand {
        labels = labels == null ? List.of() : List.copyOf(labels);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
