package org.chovy.canvas.domain.search;

/**
 * SearchMarketingKeywordQuery 承载 domain.search 场景中的不可变数据快照。
 * @param channel channel 字段。
 * @param status status 字段。
 * @param limit limit 字段。
 */
public record SearchMarketingKeywordQuery(
        String channel,
        String status,
        Integer limit) {
}
