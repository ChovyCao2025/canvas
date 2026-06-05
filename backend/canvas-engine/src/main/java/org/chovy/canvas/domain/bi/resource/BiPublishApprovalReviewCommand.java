package org.chovy.canvas.domain.bi.resource;

public record BiPublishApprovalReviewCommand(
        Long approvalId,
        String status,
        String reviewComment) {
}
