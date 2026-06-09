package org.chovy.canvas.domain.paidmedia;

/**
 * PaidMediaAudienceMemberQuery 承载 domain.paidmedia 场景中的不可变数据快照。
 * @param runId runId 字段。
 * @param eligibilityStatus eligibilityStatus 字段。
 * @param limit limit 字段。
 */
public record PaidMediaAudienceMemberQuery(
        Long runId,
        String eligibilityStatus,
        int limit) {
}
