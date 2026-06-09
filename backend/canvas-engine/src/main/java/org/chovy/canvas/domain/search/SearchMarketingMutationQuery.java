package org.chovy.canvas.domain.search;

/**
 * SearchMarketingMutationQuery 承载 domain.search 场景中的不可变数据快照。
 * @param sourceId sourceId 字段。
 * @param status status 字段。
 * @param approvalStatus approvalStatus 字段。
 * @param limit limit 字段。
 */
public record SearchMarketingMutationQuery(
        Long sourceId,
        String status,
        String approvalStatus,
        Integer limit) {
}
