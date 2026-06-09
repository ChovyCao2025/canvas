package org.chovy.canvas.domain.search;

import java.time.LocalDate;

/**
 * SearchMarketingSnapshotQuery 承载 domain.search 场景中的不可变数据快照。
 * @param channel channel 字段。
 * @param sourceId sourceId 字段。
 * @param keywordId keywordId 字段。
 * @param startDate startDate 字段。
 * @param endDate endDate 字段。
 * @param limit limit 字段。
 */
public record SearchMarketingSnapshotQuery(
        String channel,
        Long sourceId,
        Long keywordId,
        LocalDate startDate,
        LocalDate endDate,
        Integer limit) {
}
