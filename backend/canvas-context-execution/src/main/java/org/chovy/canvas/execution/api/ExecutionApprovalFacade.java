package org.chovy.canvas.execution.api;

import java.time.LocalDateTime;

public interface ExecutionApprovalFacade {

    ExecutionApprovalDecision approve(Long tenantId, String executionId, String actor, String role);

    ExecutionApprovalDecision reject(Long tenantId, String executionId, String actor, String reason, String role);

    record ExecutionApprovalDecision(
            String executionId,
            String result,
            String resultBy,
            String comment,
            LocalDateTime resultAt) {
    }
}
