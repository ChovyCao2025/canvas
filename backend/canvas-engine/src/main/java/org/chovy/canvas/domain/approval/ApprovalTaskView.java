package org.chovy.canvas.domain.approval;

import java.time.LocalDateTime;

public record ApprovalTaskView(
        Long id,
        Long tenantId,
        Long instanceId,
        Integer stepNo,
        String approver,
        String status,
        String externalTaskId,
        LocalDateTime dueAt,
        LocalDateTime actedAt,
        String actionComment) {
}
