package org.chovy.canvas.domain.approval;

public record ApprovalDecisionCommand(
        Long tenantId,
        Long taskId,
        String actor,
        String actorRole,
        String comment) {
}
