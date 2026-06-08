package org.chovy.canvas.domain.bi.export;

/**
 * BiExportApprovalReviewCommand 承载 domain.bi.export 场景中的不可变数据快照。
 * @param status status 字段。
 * @param reviewComment reviewComment 字段。
 */
public record BiExportApprovalReviewCommand(
        String status,
        String reviewComment) {
}
