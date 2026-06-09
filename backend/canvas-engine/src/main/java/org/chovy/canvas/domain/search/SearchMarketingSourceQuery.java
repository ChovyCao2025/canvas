package org.chovy.canvas.domain.search;

/**
 * SearchMarketingSourceQuery 承载 domain.search 场景中的不可变数据快照。
 * @param provider provider 字段。
 * @param channel channel 字段。
 * @param enabled enabled 字段。
 * @param limit limit 字段。
 */
public record SearchMarketingSourceQuery(
        String provider,
        String channel,
        Boolean enabled,
        Integer limit) {
}
