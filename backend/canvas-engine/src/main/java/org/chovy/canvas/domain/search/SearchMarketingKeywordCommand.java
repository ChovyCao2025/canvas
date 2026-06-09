package org.chovy.canvas.domain.search;

import java.util.List;
import java.util.Map;

/**
 * SearchMarketingKeywordCommand 承载 domain.search 场景中的不可变数据快照。
 * @param channel channel 字段。
 * @param keywordText keywordText 字段。
 * @param matchType matchType 字段。
 * @param landingPageUrl landingPageUrl 字段。
 * @param searchIntent searchIntent 字段。
 * @param labels labels 字段。
 * @param status status 字段。
 * @param metadata metadata 字段。
 */
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
