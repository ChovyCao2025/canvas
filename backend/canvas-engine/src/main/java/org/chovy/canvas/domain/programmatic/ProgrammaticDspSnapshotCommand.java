package org.chovy.canvas.domain.programmatic;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * ProgrammaticDspSnapshotCommand 承载 domain.programmatic 场景中的不可变数据快照。
 * @param lineItemId lineItemId 字段。
 * @param snapshotDate snapshotDate 字段。
 * @param bidCount bidCount 字段。
 * @param winCount winCount 字段。
 * @param impressionCount impressionCount 字段。
 * @param clickCount clickCount 字段。
 * @param conversionCount conversionCount 字段。
 * @param viewableImpressionCount viewableImpressionCount 字段。
 * @param spendAmount spendAmount 字段。
 * @param revenueAmount revenueAmount 字段。
 * @param metadata metadata 字段。
 */
public record ProgrammaticDspSnapshotCommand(
        Long lineItemId,
        LocalDate snapshotDate,
        Long bidCount,
        Long winCount,
        Long impressionCount,
        Long clickCount,
        Long conversionCount,
        Long viewableImpressionCount,
        BigDecimal spendAmount,
        BigDecimal revenueAmount,
        Map<String, Object> metadata) {
}
