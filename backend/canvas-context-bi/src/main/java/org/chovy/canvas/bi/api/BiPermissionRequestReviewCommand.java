package org.chovy.canvas.bi.api;

public record BiPermissionRequestReviewCommand(
        Long requestId,
        String status,
        String reviewComment) {
}
