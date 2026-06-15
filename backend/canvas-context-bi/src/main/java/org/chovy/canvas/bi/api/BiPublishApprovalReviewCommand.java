package org.chovy.canvas.bi.api;

public record BiPublishApprovalReviewCommand(
        Long approvalId,
        String status,
        String reviewComment) {
}
