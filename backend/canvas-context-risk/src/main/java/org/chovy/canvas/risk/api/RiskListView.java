package org.chovy.canvas.risk.api;

public record RiskListView(
        Long tenantId,
        String listKey,
        String listType,
        String subjectType,
        String status,
        boolean requiresApproval,
        String owner) {
}
