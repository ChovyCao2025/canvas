package org.chovy.canvas.bi.domain;

public record BiAccessDecision(
        boolean allowed,
        String effect,
        String matchedSubjectType,
        String matchedSubjectId,
        String reason,
        String signature
) {
}
