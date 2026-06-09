package org.chovy.canvas.domain.search;

import java.util.Map;

/**
 * SearchMarketingSourceCommand 承载 domain.search 场景中的不可变数据快照。
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
 */
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
