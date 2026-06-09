package org.chovy.canvas.domain.programmatic;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ProgrammaticDspSummaryQuery 承载 domain.programmatic 场景中的不可变数据快照。
 * @param seatId seatId 字段。
 * @param campaignId campaignId 字段。
 * @param lineItemId lineItemId 字段。
 * @param startDate startDate 字段。
 * @param endDate endDate 字段。
 * @param evaluatedAt evaluatedAt 字段。
 */
public record ProgrammaticDspSummaryQuery(
        Long seatId,
        Long campaignId,
        Long lineItemId,
        LocalDate startDate,
        LocalDate endDate,
        LocalDateTime evaluatedAt) {
}
