package org.chovy.canvas.domain.bi.resource;

/**
 * BiPublishApprovalReviewCommand 承载 domain.bi.resource 场景中的不可变数据快照。
 * @param approvalId approvalId 字段。
 * @param status status 字段。
 * @param reviewComment reviewComment 字段。
 */
public record BiPublishApprovalReviewCommand(
        Long approvalId,
        String status,
        String reviewComment) {
}
