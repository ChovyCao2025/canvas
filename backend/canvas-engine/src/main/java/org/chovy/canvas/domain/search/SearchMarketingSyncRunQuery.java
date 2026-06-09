package org.chovy.canvas.domain.search;

/**
 * SearchMarketingSyncRunQuery 承载 domain.search 场景中的不可变数据快照。
 * @param sourceId sourceId 字段。
 * @param runType runType 字段。
 * @param status status 字段。
 * @param limit limit 字段。
 */
public record SearchMarketingSyncRunQuery(
        Long sourceId,
        String runType,
        String status,
        Integer limit) {
}
