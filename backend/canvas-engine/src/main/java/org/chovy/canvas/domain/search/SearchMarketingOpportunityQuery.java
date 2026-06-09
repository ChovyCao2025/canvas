package org.chovy.canvas.domain.search;

/**
 * SearchMarketingOpportunityQuery 承载 domain.search 场景中的不可变数据快照。
 * @param channel channel 字段。
 * @param sourceId sourceId 字段。
 * @param status status 字段。
 * @param severity severity 字段。
 * @param limit limit 字段。
 */
public record SearchMarketingOpportunityQuery(
        String channel,
        Long sourceId,
        String status,
        String severity,
        Integer limit) {
}
