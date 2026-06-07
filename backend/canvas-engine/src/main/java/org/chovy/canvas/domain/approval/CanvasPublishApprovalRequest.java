package org.chovy.canvas.domain.approval;

public record CanvasPublishApprovalRequest(String reason,
                                           String larkOpenId,
                                           String larkUserId,
                                           String larkDepartmentId) {
    public CanvasPublishApprovalRequest(String reason) {
        this(reason, null, null, null);
    }
}
