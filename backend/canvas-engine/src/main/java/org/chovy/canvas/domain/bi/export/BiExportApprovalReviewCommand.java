package org.chovy.canvas.domain.bi.export;

public record BiExportApprovalReviewCommand(
        String status,
        String reviewComment) {
}
