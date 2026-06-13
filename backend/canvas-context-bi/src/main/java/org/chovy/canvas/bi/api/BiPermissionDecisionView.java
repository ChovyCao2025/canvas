package org.chovy.canvas.bi.api;

public record BiPermissionDecisionView(
        boolean allowed,
        String effect,
        String matchedSubjectType,
        String matchedSubjectId,
        String reason,
        String signature
) {
}
