package org.chovy.canvas.domain.search;

/**
 * SearchMarketingImpactWindowQuery 承载 domain.search 场景中的不可变数据快照。
 * @param opportunityId opportunityId 字段。
 * @param mutationId mutationId 字段。
 * @param sourceId sourceId 字段。
 * @param status status 字段。
 * @param decision decision 字段。
 * @param limit limit 字段。
 */
public record SearchMarketingImpactWindowQuery(
        Long opportunityId,
        Long mutationId,
        Long sourceId,
        String status,
        String decision,
        Integer limit) {
}
