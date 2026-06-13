package org.chovy.canvas.risk.api;

public record RiskListCommand(
        String listKey,
        String listType,
        String subjectType,
        boolean requiresApproval) {
}
