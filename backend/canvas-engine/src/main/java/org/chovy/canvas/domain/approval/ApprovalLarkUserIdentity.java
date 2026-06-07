package org.chovy.canvas.domain.approval;

public record ApprovalLarkUserIdentity(
        String openId,
        String userId,
        String departmentId) {
}
