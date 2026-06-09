package org.chovy.canvas.domain.search;

import java.time.LocalDate;

/**
 * SearchMarketingManualSyncCommand 承载 domain.search 场景中的不可变数据快照。
 * @param runType runType 字段。
 * @param windowStart windowStart 字段。
 * @param windowEnd windowEnd 字段。
 * @param cursorValue cursorValue 字段。
 */
public record SearchMarketingManualSyncCommand(
        String runType,
        LocalDate windowStart,
        LocalDate windowEnd,
        String cursorValue) {
}
