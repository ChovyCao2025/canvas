package org.chovy.canvas.domain.approval;

public record LarkApprovalCreateInstanceRequest(
        Long tenantId,
        String approvalCode,
        String uuid,
        String openId,
        String userId,
        String departmentId,
        String form) {
}
