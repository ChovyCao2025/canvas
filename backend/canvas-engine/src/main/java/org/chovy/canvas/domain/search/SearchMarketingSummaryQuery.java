package org.chovy.canvas.domain.search;

import java.time.LocalDate;

/**
 * SearchMarketingSummaryQuery 承载 domain.search 场景中的不可变数据快照。
 * @param channel channel 字段。
 * @param sourceId sourceId 字段。
 * @param keywordId keywordId 字段。
 * @param startDate startDate 字段。
 * @param endDate endDate 字段。
 */
public record SearchMarketingSummaryQuery(
        String channel,
        Long sourceId,
        Long keywordId,
        LocalDate startDate,
        LocalDate endDate) {
}
