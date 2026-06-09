package org.chovy.canvas.domain.search;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * SearchMarketingSourceView 承载 domain.search 场景中的不可变数据快照。
 * @param id id 字段。
 * @param tenantId tenantId 字段。
 * @param provider provider 字段。
 * @param sourceKey sourceKey 字段。
 * @param displayName displayName 字段。
 * @param channel channel 字段。
 * @param externalAccountId externalAccountId 字段。
 * @param siteUrl siteUrl 字段。
 * @param currency currency 字段。
 * @param timezone timezone 字段。
 * @param enabled enabled 字段。
 * @param metadata metadata 字段。
 * @param createdBy createdBy 字段。
 * @param createdAt createdAt 字段。
 * @param updatedAt updatedAt 字段。
 */
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
