package org.chovy.canvas.bi.api;

public record BiSelfServiceExportReviewCommand(
        String status,
        String reviewComment) {
}
