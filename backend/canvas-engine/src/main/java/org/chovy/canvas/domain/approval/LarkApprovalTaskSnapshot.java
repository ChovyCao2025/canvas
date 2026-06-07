package org.chovy.canvas.domain.approval;

public record LarkApprovalTaskSnapshot(
        String taskId,
        String status,
        String userId) {
    public LarkApprovalTaskSnapshot(String taskId, String status) {
        this(taskId, status, null);
    }
}
