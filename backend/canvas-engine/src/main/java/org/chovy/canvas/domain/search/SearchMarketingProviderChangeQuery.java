package org.chovy.canvas.domain.search;

/**
 * SearchMarketingProviderChangeQuery 承载 domain.search 场景中的不可变数据快照。
 * @param sourceId sourceId 字段。
 * @param mutationId mutationId 字段。
 * @param provider provider 字段。
 * @param reconciliationStatus reconciliationStatus 字段。
 * @param limit limit 字段。
 */
public record SearchMarketingProviderChangeQuery(
        Long sourceId,
        Long mutationId,
        String provider,
        String reconciliationStatus,
        Integer limit) {
}
