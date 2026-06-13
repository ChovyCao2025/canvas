package org.chovy.canvas.bi.domain;

public record BiDashboardReadinessIssue(
        String severity,
        String code,
        String itemType,
        String itemKey,
        String message
) {
}
