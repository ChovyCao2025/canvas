package org.chovy.canvas.domain.programmatic;

/**
 * ProgrammaticDspMutationQuery 承载 domain.programmatic 场景中的不可变数据快照。
 * @param seatId seatId 字段。
 * @param campaignId campaignId 字段。
 * @param lineItemId lineItemId 字段。
 * @param status status 字段。
 * @param approvalStatus approvalStatus 字段。
 * @param limit limit 字段。
 */
public record ProgrammaticDspMutationQuery(
        Long seatId,
        Long campaignId,
        Long lineItemId,
        String status,
        String approvalStatus,
        Integer limit
) {
}
