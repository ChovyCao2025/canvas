package org.chovy.canvas.domain.search;

import java.time.LocalDate;

/**
 * SearchMarketingUrlInspectionQuery 承载 domain.search 场景中的不可变数据快照。
 * @param sourceId sourceId 字段。
 * @param indexedState indexedState 字段。
 * @param startDate startDate 字段。
 * @param endDate endDate 字段。
 * @param limit limit 字段。
 */
public record SearchMarketingUrlInspectionQuery(
        Long sourceId,
        String indexedState,
        LocalDate startDate,
        LocalDate endDate,
        Integer limit) {
}
