package org.chovy.canvas.domain.approval;

public record LarkApprovalTaskActionRequest(
        Long tenantId,
        String instanceCode,
        String taskId,
        String actor,
        String comment) {
}
