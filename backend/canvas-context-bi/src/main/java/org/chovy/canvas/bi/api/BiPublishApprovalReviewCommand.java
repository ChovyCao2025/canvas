package org.chovy.canvas.bi.api;
/**
 * BiPublishApprovalReviewCommand 命令。
 */
public record BiPublishApprovalReviewCommand(
        /**
         * approvalId 对应的标识。
         */
        Long approvalId,
        /**
         * 状态值。
         */
        String status,
        String reviewComment) {
}
