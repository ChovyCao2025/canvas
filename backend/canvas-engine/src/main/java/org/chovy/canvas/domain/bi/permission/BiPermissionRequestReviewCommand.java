package org.chovy.canvas.domain.bi.permission;

public record BiPermissionRequestReviewCommand(
        Long requestId,
        String status,
        String reviewComment) {
}
