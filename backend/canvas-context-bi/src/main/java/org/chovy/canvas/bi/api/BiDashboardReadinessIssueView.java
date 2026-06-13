package org.chovy.canvas.bi.api;

public record BiDashboardReadinessIssueView(
        String severity,
        String code,
        String itemType,
        String itemKey,
        String message
) {
}
