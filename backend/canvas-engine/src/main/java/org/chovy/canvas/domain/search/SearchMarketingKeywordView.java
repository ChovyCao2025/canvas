package org.chovy.canvas.domain.search;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * SearchMarketingKeywordView 承载 domain.search 场景中的不可变数据快照。
 * @param id id 字段。
 * @param tenantId tenantId 字段。
 * @param channel channel 字段。
 * @param keywordText keywordText 字段。
 * @param keywordKey keywordKey 字段。
 * @param matchType matchType 字段。
 * @param landingPageUrl landingPageUrl 字段。
 * @param landingPageUrlHash landingPageUrlHash 字段。
 * @param searchIntent searchIntent 字段。
 * @param labels labels 字段。
 * @param status status 字段。
 * @param metadata metadata 字段。
 * @param createdBy createdBy 字段。
 * @param createdAt createdAt 字段。
 * @param updatedAt updatedAt 字段。
 */
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
